package it.jaschke.alexandria;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private static final String LOG_TAG = AddBook.class.getSimpleName();
    private EditText ean;
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT = "eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";

    private BroadcastReceiver mNetworkChangeReceiver = new BroadcastReceiver() {
        /**
         * Listen on network events to relaunch the search when internet is available.
         * Example scenario: you're in plane mode, you search: no internet.
         * You remove the plane mode, you expect it to launch the search without having to do
         * something weird like delete/retype the last digit.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "in onReceive (for connectivity state changes)");
            Log.d(LOG_TAG, "Intent action is: " + intent.getAction());
            if (Utility.isNetworkAvailable(context)) {
                Log.d(LOG_TAG, "We have network!");
                validateInputAndLaunchService();
            } else {
                Log.d(LOG_TAG, "We DO NOT have network...");
                // Here we don't do anything: if we had network and we displayed a result, we don't want "check your internet connection" to replace it!
            }
        }
    };

    public AddBook() {
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mNetworkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mNetworkChangeReceiver);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Check if the current input is a valid ISBN number.
     * If so, launch the fetching service and restarts the loader.
     */
    public void validateInputAndLaunchService() {
        String currentInput = ean.getText().toString();
        //catch isbn10 numbers
        if (currentInput.length() == 10 && !currentInput.startsWith("978")) {
            currentInput = "978" + currentInput;
        }
        if (currentInput.length() < 13) {
            clearFields();
            // Say we have an ISBN 13 and we remove the last digit: we want to clear the result.
            // We need to restart the loader otherwise it'll re-populate the views.
            BookService.resetFetchBookStatus(getActivity());
            restartLoader();
            return;
        }
        //Once we have an ISBN, start a book intent

        // First we want to reset the status of the task.
        BookService.resetFetchBookStatus(getActivity());
        // If we don't have internet, there's no point in proceeding.
        if (Utility.isNetworkAvailable(getActivity())) {
            Intent bookIntent = new Intent(getActivity(), BookService.class);
            bookIntent.putExtra(BookService.EAN, currentInput);
            bookIntent.setAction(BookService.FETCH_BOOK);
            Log.d(LOG_TAG, "starting BookIntent service...");
            getActivity().startService(bookIntent);
        }
        restartLoader();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (ean != null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) rootView.findViewById(R.id.ean);

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                clearFields();
                validateInputAndLaunchService();
            }
        });

        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is the callback method that the system will invoke when your button is
                // clicked. You might do this by launching another app or by including the
                //functionality directly in this app.
                // Hint: Use a Try/Catch block to handle the Intent dispatch gracefully, if you
                // are using an external app.
                //when you're done, remove the toast below.
                Context context = getActivity();
                CharSequence text = "This button should let you scan a book for its barcode!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Add da book!!
                String eanStr = ean.getText().toString();
                addBookToList(eanStr);
                ean.setText("");
            }
        });
// TODO: move that to the fragment with the list of books.
//        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent bookIntent = new Intent(getActivity(), BookService.class);
//                bookIntent.putExtra(BookService.EAN, ean.getText().toString());
//                bookIntent.setAction(BookService.DELETE_BOOK);
//                getActivity().startService(bookIntent);
//                ean.setText("");
//            }
//        });

        if (savedInstanceState != null) {
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        return rootView;
    }

    /**
     * Add a book that is already in the database to the user's list by changing a boolean field.
     *
     * @param eanStr: the ISBN-13 number of the book.
     */
    private void addBookToList(String eanStr) {
        ContentValues bookValues = new ContentValues();
        bookValues.put(AlexandriaContract.BookEntry.COLUMN_BOOK_IN_LIST, 1);
        getContext().getContentResolver().update(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(eanStr)),
                bookValues,
                null,
                null
        );
    }

    private void restartLoader() {
        Log.d(LOG_TAG, "in restartLoader");
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "in onCreateLoader");
//        if (ean.getText().length() == 0) {
//            return null;
//        }
        String eanStr = ean.getText().toString();
        if (eanStr.length() <= 10 && !eanStr.startsWith("978")) {
            eanStr = "978" + eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "in onLoadFinished");
        if (!data.moveToFirst()) {
            updateEmptyView();
            return;
        }
        // Clear a possible pre-existing view.
        updateEmptyView();
        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.COLUMN_TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.COLUMN_SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.COLUMN_AUTHOR));
        if (authors != null) {
            String[] authorsArr = authors.split(",");
            ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
            ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",", "\n"));
        }
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.COLUMN_IMAGE_URL));
        if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
            new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
            rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

        rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.clear_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields() {
        Log.d(LOG_TAG, "in clearFields");
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(LOG_TAG, "in onSharedPreferenceChanged, with key: " + key);
        if (key.equals(getString(R.string.pref_fetch_book_status_key))) {
            updateEmptyView();
        }
    }

    /**
     * Update the empty view with a relevant message for the user, based on the status of the
     * book-fetching task.
     */
    private void updateEmptyView() {
        int fetchBookStatus = Utility.getFetchBookStatus(getActivity());
        String errorMessage = null;
        switch (fetchBookStatus) {
            case BookService.FETCH_BOOK_STATUS_OK:
                Log.d(LOG_TAG, "Successfully fetched book (status ok)");
                AddBook.this.restartLoader();
                break;
            case BookService.FETCH_BOOK_STATUS_SERVER_INVALID:
                Log.d(LOG_TAG, "Display smt for server invalid");
                errorMessage = getActivity().getString(R.string.fetch_book_status_server_invalid);
                break;
            case BookService.FETCH_BOOK_STATUS_SERVER_DOWN:
                Log.d(LOG_TAG, "Display smt for server down");
                errorMessage = getActivity().getString(R.string.fetch_book_status_server_down);
                break;
            case BookService.FETCH_BOOK_STATUS_NO_BOOK_FOUND:
                Log.d(LOG_TAG, "fetch book status: No book found.");
                errorMessage = getActivity().getString(R.string.fetch_book_status_no_book_found);
                break;
            default:
                if (!Utility.isNetworkAvailable(getActivity())) {
                    Log.d(LOG_TAG, "Display smt for Internet ra?");
                    errorMessage = getActivity().getString(R.string.fetch_book_status_no_internet);
                } else {
                    Log.d(LOG_TAG, "Status unknown but there's internet.");
                }
        }
        TextView emptyView = (TextView) getView().findViewById(R.id.no_book_found);
        if (null != errorMessage) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(errorMessage);
        } else {
            emptyView.setVisibility(View.INVISIBLE);
        }
    }

}
