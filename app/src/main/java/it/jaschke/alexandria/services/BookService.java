package it.jaschke.alexandria.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;

import it.jaschke.alexandria.MainActivity;
import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class BookService extends IntentService {

    private static final String LOG_TAG = BookService.class.getSimpleName();

    public static final String FETCH_BOOK = "it.jaschke.alexandria.services.action.FETCH_BOOK";
    public static final String DELETE_BOOK = "it.jaschke.alexandria.services.action.DELETE_BOOK";

    public static final String EAN = "it.jaschke.alexandria.services.extra.EAN";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FETCH_BOOK_STATUS_OK, FETCH_BOOK_STATUS_SERVER_DOWN, FETCH_BOOK_STATUS_SERVER_INVALID, FETCH_BOOK_STATUS_UNKNOWN, FETCH_BOOK_STATUS_NO_BOOK_FOUND})
    public @interface FetchBookStatus {}

    public static final int FETCH_BOOK_STATUS_OK = 0;
    public static final int FETCH_BOOK_STATUS_SERVER_DOWN = 1;
    public static final int FETCH_BOOK_STATUS_SERVER_INVALID = 2;
    public static final int FETCH_BOOK_STATUS_UNKNOWN = 3;
    public static final int FETCH_BOOK_STATUS_NO_BOOK_FOUND = 4;

    public BookService() {
        super("Alexandria");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            switch (action) {
                case FETCH_BOOK: {
                    final String ean = intent.getStringExtra(EAN);
                    fetchBook(ean);
                    break;
                }
                case DELETE_BOOK: {
                    final String ean = intent.getStringExtra(EAN);
                    deleteBook(ean);
                }
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void deleteBook(String ean) {
        if(ean!=null) {
            getContentResolver().delete(AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)), null, null);
        }
    }

    /**
     * Handle action fetchBook in the provided background thread with the provided
     * parameters.
     */
    private void fetchBook(String ean) {

        if(ean.length()!=13){
            setFetchBookStatus(this, FETCH_BOOK_STATUS_OK);
            return;
        }

        Cursor bookEntry = getContentResolver().query(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        if(bookEntry.getCount()>0){
            bookEntry.close();
            setFetchBookStatus(this, FETCH_BOOK_STATUS_OK);
            return;
        }

        bookEntry.close();

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String bookJsonString = null;

        try {
            final String FORECAST_BASE_URL = "https://www.googleapis.com/books/v1/volumes?";
            final String QUERY_PARAM = "q";

            final String ISBN_PARAM = "isbn:" + ean;

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                    .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // No data returned by the server.
                return;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }

            if (buffer.length() == 0) {
                setFetchBookStatus(this, FETCH_BOOK_STATUS_SERVER_DOWN);
                return;
            }
            bookJsonString = buffer.toString();
            parseBookJson(ean, bookJsonString);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            setFetchBookStatus(this, FETCH_BOOK_STATUS_SERVER_DOWN);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }

        }

    }

    private void parseBookJson(String ean, String bookJsonString) {
        final String ITEMS = "items";

        final String VOLUME_INFO = "volumeInfo";

        final String TITLE = "title";
        final String SUBTITLE = "subtitle";
        final String AUTHORS = "authors";
        final String DESC = "description";
        final String CATEGORIES = "categories";
        final String IMG_URL_PATH = "imageLinks";
        final String IMG_URL = "thumbnail";

        try {
            JSONObject bookJson = new JSONObject(bookJsonString);

            JSONArray bookArray;
            if(bookJson.has(ITEMS)){
                bookArray = bookJson.getJSONArray(ITEMS);
            }else{
                Log.d(LOG_TAG, "No book found");
                setFetchBookStatus(this, FETCH_BOOK_STATUS_NO_BOOK_FOUND);
                return;
            }

            JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(VOLUME_INFO);

            String title = bookInfo.getString(TITLE);

            String subtitle = "";
            if(bookInfo.has(SUBTITLE)) {
                subtitle = bookInfo.getString(SUBTITLE);
            }

            String desc="";
            if(bookInfo.has(DESC)){
                desc = bookInfo.getString(DESC);
            }

            String imgUrl = "";
            if(bookInfo.has(IMG_URL_PATH) && bookInfo.getJSONObject(IMG_URL_PATH).has(IMG_URL)) {
                imgUrl = bookInfo.getJSONObject(IMG_URL_PATH).getString(IMG_URL);
            }

            writeBackBook(ean, title, subtitle, desc, imgUrl);

            if(bookInfo.has(AUTHORS)) {
                writeBackAuthors(ean, bookInfo.getJSONArray(AUTHORS));
            }
            if(bookInfo.has(CATEGORIES)){
                writeBackCategories(ean,bookInfo.getJSONArray(CATEGORIES) );
            }
            setFetchBookStatus(this, FETCH_BOOK_STATUS_OK);

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error ", e);
            setFetchBookStatus(this, FETCH_BOOK_STATUS_SERVER_INVALID);
        }
    }

    /** Taken from Sunshine.
     * Sets the fetch book status into shared preference.  This function should not be called from
     * the UI thread because it uses commit to write to the shared preferences.
     * @param context Context to get the default shared preferences from.
     * @param fetchBookStatus The IntDef value to set
     */
    private static void setFetchBookStatus(Context context, @FetchBookStatus int fetchBookStatus) {
        Log.d(LOG_TAG, "in setFetchBookStatus");
        SharedPreferences.Editor spe = PreferenceManager.getDefaultSharedPreferences(context).edit();
//        PreferenceManager pm = ;
//        PreferenceManager.getSharedPreferencesName()
        spe.putInt(context.getString(R.string.pref_fetch_book_status_key), fetchBookStatus);
        spe.commit();
    }

    /**
     * Reset the status of the fetch book task in the shared preferences.
     * @param context
     */

    public static void resetFetchBookStatus(Context context) {
        Log.d(LOG_TAG, "in resetFetchBookStatus");
        setFetchBookStatus(context, FETCH_BOOK_STATUS_UNKNOWN);
    }

    /**
     * Write a book to the database. Note a book written to the database is not added to the user's
     * list unless a boolean field indicates so.
     */
    private void writeBackBook(String ean, String title, String subtitle, String desc, String imgUrl) {
        ContentValues values= new ContentValues();
        values.put(AlexandriaContract.BookEntry._ID, ean);
        values.put(AlexandriaContract.BookEntry.COLUMN_TITLE, title);
        values.put(AlexandriaContract.BookEntry.COLUMN_IMAGE_URL, imgUrl);
        values.put(AlexandriaContract.BookEntry.COLUMN_SUBTITLE, subtitle);
        values.put(AlexandriaContract.BookEntry.COLUMN_DESC, desc);
        values.put(AlexandriaContract.BookEntry.COLUMN_BOOK_IN_LIST, 0);
        getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI,values);
    }

    private void writeBackAuthors(String ean, JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            ContentValues values= new ContentValues();
            values.put(AlexandriaContract.AuthorEntry._ID, ean);
            values.put(AlexandriaContract.AuthorEntry.COLUMN_AUTHOR, jsonArray.getString(i));
            getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);
        }
    }

    private void writeBackCategories(String ean, JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            ContentValues values= new ContentValues();
            values.put(AlexandriaContract.CategoryEntry._ID, ean);
            values.put(AlexandriaContract.CategoryEntry.CATEGORY, jsonArray.getString(i));
            getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
        }
    }
 }