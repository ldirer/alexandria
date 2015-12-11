package it.jaschke.alexandria.api;


import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.RecursiveAction;

import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.DownloadImage;

/**
 * Created by saj on 11/01/15.
 */
public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.BookListViewHolder> {

    private Cursor mCursor;
    // We want a context to get resources (string formatting, etc).
    private Context mContext;
    private Callback mOnItemSelectedCallback;


    public BookListAdapter(Context context, Cursor cursor, Callback onItemSelectedCallback) {
        mContext = context;
        mCursor = cursor;
        mOnItemSelectedCallback = onItemSelectedCallback;
    }

    public void swapCursor(Cursor data) {
        mCursor = data;
        notifyDataSetChanged();
        // Here we could make an empty view visible if the itemCount is 0.
    }

    public class BookListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final ImageView bookCover;
        public final TextView bookTitle;
        public final TextView bookSubTitle;

        public BookListViewHolder(View view) {
            super(view);
            bookCover = (ImageView) view.findViewById(R.id.fullBookCover);
            bookTitle = (TextView) view.findViewById(R.id.listBookTitle);
            bookSubTitle = (TextView) view.findViewById(R.id.listBookSubTitle);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mCursor != null && mCursor.moveToPosition(getAdapterPosition())) {
                mOnItemSelectedCallback.onItemSelected(mCursor.getString(mCursor.getColumnIndex(AlexandriaContract.BookEntry._ID)));
            }
        }
    }

    @Override
    public BookListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_list_item, parent, false);
        // TODO: copied from sunshine. What is focusable anyway??
        view.setFocusable(true);
        return new BookListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BookListViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        // TODO - QUESTION: Using getColumnIndex vs the pattern we used in Sunshine (Declaring integer constants ourselves)?
        // TODO - QUESTION: Is this heavier? This seems to avoid some boilerplate, error-prone code.
        String imgUrl = mCursor.getString(mCursor.getColumnIndex(AlexandriaContract.BookEntry.COLUMN_IMAGE_URL));
        new DownloadImage(holder.bookCover).execute(imgUrl);

        String bookTitle = mCursor.getString(mCursor.getColumnIndex(AlexandriaContract.BookEntry.COLUMN_TITLE));
        holder.bookTitle.setText(bookTitle);

        String bookSubTitle = mCursor.getString(mCursor.getColumnIndex(AlexandriaContract.BookEntry.COLUMN_SUBTITLE));
        holder.bookSubTitle.setText(bookSubTitle);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

}
