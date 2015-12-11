package it.jaschke.alexandria;

import android.content.ContentValues;
import android.content.Context;
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
                (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    /**
     *
     * @param c Context used to get the Shared Preferences.
     * @return
     */

    @SuppressWarnings("ResourceType")
    static public @BookService.FetchBookStatus int getFetchBookStatus(Context c) {
        Log.d(LOG_TAG, "in getFetchBookStatus");
        SharedPreferences bookStatusPref = PreferenceManager.getDefaultSharedPreferences(c);
        return bookStatusPref.getInt(c.getString(R.string.pref_fetch_book_status_key),
                BookService.FETCH_BOOK_STATUS_UNKNOWN);
    }

    /**
     * Add a book that is already in the database to the user's list by changing a boolean field.
     * @param context: a context to get the content resolver from.
     * @param eanStr: the ISBN-13 number of the book.
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
     * @param context: a context to get the content resolver from.
     * @param eanStr: the ISBN-13 number of the book.
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


    /**
     * Dichotomic search to find a text size that fits.
     * Adapted from:
     http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview
     *
     * @param fragment
     * @param view
     * @return A text size that works (in pixels).
     */
    static float findRightTextSize(Fragment fragment, TextView view) {
        String text = (String) view.getText();
        int textWidth = view.getWidth();
        int targetWidth = textWidth - view.getPaddingLeft() - view.getPaddingRight();
        // We don't want to use view.getTextSize() because repeated calls would gradually shrink the text!
        // float hi = view.getTextSize();

        // Get view height in dp:  http://stackoverflow.com/questions/4605527/converting-pixels-to-dp
        DisplayMetrics metrics = fragment.getResources().getDisplayMetrics();
        float dpHeight = view.getHeight() / (metrics.densityDpi / 160f);
        // We take the max so that the text fits on one line and does not overflow vertically.
        float hi = Math.min(fragment.getResources().getInteger(R.integer.detail_max_text_font), dpHeight);

        float lo = 10;
        final float threshold = 2f; // How close we have to be

        TextPaint testPaint = new TextPaint();
        testPaint.set(view.getPaint());

        while ((hi - lo) > threshold) {
            float size = (hi + lo) / 2;
            testPaint.setTextSize(size);
            // For some reason if we target exactly targetWidth the text still does not fit on a line.
            if (testPaint.measureText(text) >= targetWidth / 1.5)
                hi = size; // too big
            else
                lo = size; // too small
        }
        // Use lo so that we undershoot rather than overshoot
        return lo;
    }
}
