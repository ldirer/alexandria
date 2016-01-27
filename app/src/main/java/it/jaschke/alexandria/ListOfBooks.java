package it.jaschke.alexandria;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

import it.jaschke.alexandria.api.BookListAdapter;
import it.jaschke.alexandria.api.Callback;
import it.jaschke.alexandria.data.AlexandriaContract;


public class ListOfBooks extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = ListOfBooks.class.getSimpleName();
    private BookListAdapter bookListAdapter;
    private RecyclerView bookList;
    private int position = ListView.INVALID_POSITION;
    private EditText searchText;

    private final int LOADER_ID = 10;

    public ListOfBooks() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Cursor cursor = getActivity().getContentResolver().query(
                AlexandriaContract.BookEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                AlexandriaContract.BookEntry.COLUMN_BOOK_IN_LIST + " = ?", // cols for "where" clause
                new String[]{"1"}, // values for "where" clause
                null  // sort order
        );


        bookListAdapter = new BookListAdapter(getActivity(), cursor, (Callback) getActivity());
        View rootView = inflater.inflate(R.layout.fragment_list_of_books, container, false);
        searchText = (EditText) rootView.findViewById(R.id.searchText);
        rootView.findViewById(R.id.searchButton).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ListOfBooks.this.restartLoader();
                    }
                }
        );

        bookList = (RecyclerView) rootView.findViewById(R.id.listOfBooks);
        // This is a custom method because we need a reference to recyclerview in the adapter.
        bookListAdapter.setRecyclerView(bookList);
        bookList.setAdapter(bookListAdapter);
        bookList.setLayoutManager(new LinearLayoutManager(getActivity()));

        return rootView;
    }

    private void restartLoader(){
        Log.d(LOG_TAG, "in restartLoader");
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selectionStr = AlexandriaContract.BookEntry.COLUMN_BOOK_IN_LIST + " = ?";
        ArrayList<String> selectionArgs = new ArrayList<String>();
        selectionArgs.add("1");

        String searchStringSelection = AlexandriaContract.BookEntry.COLUMN_TITLE +" LIKE ? OR " + AlexandriaContract.BookEntry.COLUMN_SUBTITLE + " LIKE ? ";
        String searchString = searchText.getText().toString();

        if(searchString.length()>0){
            searchString = "%"+searchString+"%";
            selectionStr += " AND " + searchStringSelection;
            selectionArgs.add(searchString);
            selectionArgs.add(searchString);
        }

        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.CONTENT_URI,
                null,
                selectionStr,
                selectionArgs.toArray(new String[]{}),
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "in onLoadFinished");
        bookListAdapter.swapCursor(data);
        if (position != ListView.INVALID_POSITION) {
            bookList.smoothScrollToPosition(position);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        bookListAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(LOG_TAG, "in onActivityCreated");
        getActivity().setTitle(R.string.books);
    }


    @Override
    public void onResume() {
        super.onResume();
        // Make sure we see a new list if we removed an item.
        restartLoader();
    }

    public BookListAdapter getAdapter() {
        Log.d(LOG_TAG, "in getAdapter");
        return bookListAdapter;
    }
}
