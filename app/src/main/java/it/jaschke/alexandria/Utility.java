package it.jaschke.alexandria;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import it.jaschke.alexandria.R;
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
}
