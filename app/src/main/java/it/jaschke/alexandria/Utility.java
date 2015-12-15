package it.jaschke.alexandria;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;


import android.support.v4.app.Fragment;
import android.widget.TextView;

import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;

public class Utility {

    private static final String LOG_TAG = Utility.class.getSimpleName();

    /**
     * Copied from Sunshine.
     * Returns true if the network is available or about to become available.
     *
     * @param c Context used to get the ConnectivityManager
     * @return true if the network is available
     */
    static public boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    /**
     * @param c Context used to get the Shared Preferences.
     * @return
     */

    @SuppressWarnings("ResourceType")
    static public
    @BookService.FetchBookStatus
    int getFetchBookStatus(Context c) {
        Log.d(LOG_TAG, "in getFetchBookStatus");
        SharedPreferences bookStatusPref = PreferenceManager.getDefaultSharedPreferences(c);
        return bookStatusPref.getInt(c.getString(R.string.pref_fetch_book_status_key),
                BookService.FETCH_BOOK_STATUS_UNKNOWN);
    }

    /**
     * Add a book that is already in the database to the user's list by changing a boolean field.
     *
     * @param context: a context to get the content resolver from.
     * @param eanStr:  the ISBN-13 number of the book.
     */
    public static void addBookToList(Context context, String eanStr) {
        ContentValues bookValues = new ContentValues();
        bookValues.put(AlexandriaContract.BookEntry.COLUMN_BOOK_IN_LIST, 1);
        context.getContentResolver().update(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(eanStr)),
                bookValues,
                null,
                null
        );
    }

    /**
     * Remove a book that is already in the database from the user's list by changing a boolean field.
     *
     * @param context: a context to get the content resolver from.
     * @param eanStr:  the ISBN-13 number of the book.
     */
    public static void removeBookFromList(Context context, String eanStr) {
        ContentValues bookValues = new ContentValues();
        bookValues.put(AlexandriaContract.BookEntry.COLUMN_BOOK_IN_LIST, 0);
        context.getContentResolver().update(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(eanStr)),
                bookValues,
                null,
                null
        );
    }


    public static void deleteBook(Context context, String eanStr) {
        Log.d(LOG_TAG, "in deleteBook");
        context.getContentResolver().delete(AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(eanStr)), null, null);

        // This previous code made it complicated to notify the adapter of the changes.
//        Intent bookIntent = new Intent(context, BookService.class);
//        bookIntent.putExtra(BookService.EAN, eanStr);
//        bookIntent.setAction(BookService.DELETE_BOOK);
//        context.startService(bookIntent);
    }
}
