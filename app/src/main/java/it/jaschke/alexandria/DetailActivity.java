package it.jaschke.alexandria;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class DetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = DetailActivity.class.getSimpleName();
    private String DETAIL_FRAGMENT_NAME = "book_detail_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "in onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_activity);

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle extras = getIntent().getExtras();
        if(null != extras) {
            BookDetail fragment = new BookDetail();
            fragment.setArguments(extras);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.book_detail_container, fragment)
                 // If we had it to the backstack, the back button takes us to an empty activity and we need to press twice to go back to the launcher activity.
                 //   .addToBackStack(DETAIL_FRAGMENT_NAME)
                    .commit();
            getSupportFragmentManager().executePendingTransactions();
            Log.d(LOG_TAG, "Activity has " + getSupportFragmentManager().getBackStackEntryCount() + " fragments");
        }
    }


}
