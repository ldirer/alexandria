package it.jaschke.alexandria.api;


import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import it.jaschke.alexandria.R;
import it.jaschke.alexandria.Utility;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.DownloadImage;

/**
 * Created by saj on 11/01/15.
 */
public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.BookListViewHolder> {

    private static final String LOG_TAG = BookListAdapter.class.getSimpleName();
    private Cursor mCursor;
    // We want a context to get resources (string formatting, etc).
    private Context mContext;
    private Callback mOnItemSelectedCallback;

    public SwipeDetector mSwipeDetector;
    private RecyclerView recyclerView;

    public BookListAdapter(Context context, Cursor cursor, Callback onItemSelectedCallback) {
        Log.d(LOG_TAG, "in BookListAdapter constructor");
        mContext = context;
        mCursor = cursor;
        mOnItemSelectedCallback = onItemSelectedCallback;
    }

    public void setRecyclerView(RecyclerView view) {
        recyclerView = view;
    }

    public void swapCursor(Cursor data) {
        mCursor = data;
        notifyDataSetChanged();
        // Here we could make an empty view visible if the itemCount is 0.
    }

    public class BookListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
    OnMySwipe {
        public final View mainView;
        public final ImageView bookCover;
        public final TextView bookTitle;
        public final TextView bookSubTitle;
        public final Button bookRemoveButton;
        public final LinearLayout bookView;

        public SwipeDetector mSwipeDetector;

        public BookListViewHolder(View view) {
            super(view);
            mainView = view.findViewById(R.id.mainView);
            bookCover = (ImageView) view.findViewById(R.id.bookCover);
            bookTitle = (TextView) view.findViewById(R.id.listBookTitle);
            bookSubTitle = (TextView) view.findViewById(R.id.listBookSubTitle);
            bookRemoveButton = (Button) view.findViewById(R.id.listBookRemove);
            bookRemoveButton.setClickable(false);
            bookView = (LinearLayout) view.findViewById(R.id.bookView);

            mSwipeDetector = new SwipeDetector(this);
            view.setOnTouchListener(mSwipeDetector);

            // Calling setOnClickListener (even AFTER) the setOnTouchListener does not work.
            // The click is interpreted as a touch first and so is not handled properly, unless we 'relay the click' in the OnTouchListener.
            view.setOnClickListener(this);

            bookRemoveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "in remove onClick");
                    Utility.removeBookFromList(mContext, mCursor.getString(mCursor.getColumnIndex(AlexandriaContract.BookEntry._ID)));
                }
            });

        }

        @Override
        public void onClick(View v) {
            Log.d(LOG_TAG, "in onClick");
            if (mCursor != null && mCursor.moveToPosition(getAdapterPosition())) {
                mOnItemSelectedCallback.onItemSelected(mCursor.getString(mCursor.getColumnIndex(AlexandriaContract.BookEntry._ID)));
            }
        }

        /**
         * Here we'll update the margin of our bookView so that we see more/less of our remove button.
         */
        @Override
        public void onMySwiped(final int distance) {
            // getLayoutParams refers to params for the *parent* of this view! This is a bit weird.
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bookView.getLayoutParams();
            Log.d(LOG_TAG, String.format("onMySwiped: distance=%d", distance));
            Utility.setMarginEnd(params, distance);
            // Make sure the changes are taken into account. No need to use setLayoutParams since we changed params inplace.
            bookView.requestLayout();
        }

        /**
         * Set the button state to the expanded state with the button fully visible.
        */
        @Override
        public void setFullSwipeState() {
            bookRemoveButton.setClickable(true);
            // We want to animate the margin towards our expanded state width, maxWidth.
            // Would it be better to store the 0.15 in Resources? Is there a better approach?
            final int maxWidth = (int) (0.15 * mainView.getWidth());
            Log.d(LOG_TAG, String.format("maxWidth: %d", maxWidth));
            Animation a = new Animation() {
                Integer initMarginEnd;

                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bookView.getLayoutParams();
                    if (null == initMarginEnd) {
                        initMarginEnd = Utility.getMarginEnd(params);
                    }
                    else{
                        Utility.setMarginEnd(params, (int) (initMarginEnd + (maxWidth - initMarginEnd) * interpolatedTime));
                        bookView.requestLayout();
                    }
                }
            };
            a.setDuration(500); // in ms
            bookView.startAnimation(a);

            // Alternative with no animation:
//            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bookView.getLayoutParams();
//            params.setMargins(0, 0, maxWidth, 0);
//            bookView.requestLayout();
        }

        /**
         * Set the layout to the initial state: button hidden, not clickable.
         */
        @Override
        public void setInitialState() {
            Log.d(LOG_TAG, "in setInitialState");
            bookRemoveButton.setClickable(false);
            Animation a = new Animation() {
                Integer initMarginEnd;

                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bookView.getLayoutParams();
                    if (null == initMarginEnd) {
                        initMarginEnd = Utility.getMarginEnd(params);
                    }
                    else{
                        Utility.setMarginEnd(params, (int) (initMarginEnd * (1 - interpolatedTime)));
                        Log.d(LOG_TAG, String.format("1 - interpolatedTime=%f", 1 - interpolatedTime));
                        bookView.requestLayout();
                    }
                }
            };
            a.setDuration(500); // in ms
            bookView.startAnimation(a);
            Log.d(LOG_TAG, "Done hiding button...");
        }

    }


    /**
     * I tried to decouple the detection from the 'UI logic'.
     * Results are poor though, my SwipeDetector and my ViewHolder are still heavily coupled.
     */

    public interface OnMySwipe {
        void onMySwiped(int margin);
        void setFullSwipeState();
        void setInitialState();
    }


    /**
     * Still very fragile. Setting the button as clickable when not hidden for instance:
     * that makes the button deal with all click events before the book view.
     * If the button has a `match_parent` width, that's ALL the clicks...
     *
     * It also means clicking close to the remove button (but not *on* it) is likely to trigger the removal.
     * How could I better deal with that ?
     *
     * Heavily inspired from: http://www.jayrambhia.com/blog/swipe-listview/
     */
    public class SwipeDetector implements View.OnTouchListener {
        double mInitialTouchX;
        double mInitialTouchY;
        double dX;
        double dY;
        // We 'preempt' scrolling when the user swipes a certain amount.
        private double mSwipeThreshold = 5;
        private double mSwipeEndThreshold = 100;
        private RecyclerView.ViewHolder mViewHolder;
        private String LOG_TAG = SwipeDetector.class.getSimpleName();
        private boolean disallowInterceptTouchEvent;
        private Boolean mButtonHidden;

        public SwipeDetector(RecyclerView.ViewHolder viewHolder) {
            mViewHolder = viewHolder;
        }

        /* onTouch return value indicates if we *consumed* the event or not.
        If false other views will have an opportunity to handle the event.

        We ask the recyclerview to not intercept our events: otherwise it'll try to scroll (and not
        swipe) every time we swipe slightly down/upwards.

        We also set a threshold to trigger this 'no interception request', otherwise we can't scroll at all.
         */
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.d(LOG_TAG, "in onTouch");
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                mInitialTouchX = event.getX();
                mInitialTouchY = event.getY();
                // If we put the following line here, we can't scroll no more!
//                recyclerView.requestDisallowInterceptTouchEvent(true);
                return true; // Allow other events like click to be processed. TODO: that's from the tutorial, I thought that was the exact opposite!
            }

            dX = event.getX() - mInitialTouchX;
            dY = Math.abs(event.getY() - mInitialTouchY);
            Log.d(LOG_TAG, String.format("in onTouch: dX=%f", dX));
            Log.d(LOG_TAG, String.format("in onTouch: dY=%f", dY));
            if (event.getAction() == MotionEvent.ACTION_MOVE) {

                // We're swiping to the left so we need to use -dX.
                if(-dX > mSwipeThreshold) {

                    if(!disallowInterceptTouchEvent) {
                        recyclerView.requestDisallowInterceptTouchEvent(true);
                        disallowInterceptTouchEvent = true;
                    }
                    ((OnMySwipe) mViewHolder).onMySwiped((int) (Math.abs(dX) - mSwipeThreshold));
                    mButtonHidden = false;
                    return true;
                }
            }

            if(event.getAction() == MotionEvent.ACTION_UP) {
                recyclerView.requestDisallowInterceptTouchEvent(false);
                disallowInterceptTouchEvent = false;
                if(-dX > mSwipeThreshold + (mSwipeEndThreshold - mSwipeThreshold) / 2) {
                    // We're move than halfway through: we expand the button fully (or reset it to the normal size)
                    ((OnMySwipe) mViewHolder).setFullSwipeState();
                    mButtonHidden = false;
                }
                else {
                    // We swiped right, or mini-swiped.
                    if(-dX < mSwipeThreshold && mButtonHidden) {
                        // We did not move enough to swipe: this is a click event.
                        // We need to relay it since we intercepted the touch events.
                        mViewHolder.itemView.performClick();
                    }
                    // We hide the button again.
                    ((OnMySwipe) mViewHolder).setInitialState();
                    mButtonHidden = true;
                }
                return true;
            }

            return false;
        }
    }

    @Override
    public BookListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(LOG_TAG, "in onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_list_item, parent, false);
        view.setFocusable(true);
        return new BookListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BookListViewHolder holder, int position) {
        mCursor.moveToPosition(position);
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
