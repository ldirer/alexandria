package it.jaschke.alexandria;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import it.jaschke.alexandria.api.Callback;


public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, Callback {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment navigationDrawerFragment;
    private AddBook mAddBookFragment;
    public static final String LIST_BOOK_FRAGMENT_TAG = "LB_FRAGMENT_TAG";

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence title;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        title = getTitle();

        // Set up the drawer.
        navigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment nextFragment;
        String tag = "";
        switch (position){
            default:
            case 0:
                tag = LIST_BOOK_FRAGMENT_TAG;
                ListOfBooks existingFragment = (ListOfBooks) fragmentManager.findFragmentByTag(tag);
                if (null != existingFragment) {
                    // Use this existing fragment and don't create a new one!
                    Log.d(LOG_TAG, "Reusing existing fragment");
                    nextFragment = existingFragment;
                }
                else {
                    nextFragment = new ListOfBooks();
                }

                break;
            case 1:
                mAddBookFragment = new AddBook();
                nextFragment = mAddBookFragment;
                break;
            case 2:
                nextFragment = new About();
                break;

        }

        Log.d(LOG_TAG, "Adding a fragment - tag: " + tag);
        fragmentManager.beginTransaction()
                .replace(R.id.container, nextFragment, tag)
                .addToBackStack((String) title)
                .commit();
    }

    public void setTitle(int titleId) {
        title = getString(titleId);
        Log.d(LOG_TAG, String.format("in setTitle, title=%s, titleid=%d", title, titleId));
    }

    public void restoreActionBar() {
        Log.d(LOG_TAG, "in restoreActionBar");
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(title);
        actionBar.setDisplayShowTitleEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!navigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemSelected(String ean) {
        Bundle args = new Bundle();
        args.putString(BookDetail.EAN_KEY, ean);

        BookDetail fragment = new BookDetail();
        fragment.setArguments(args);

        if(findViewById(R.id.right_container) != null){
            // We have a two-pane layout.
            int id = R.id.right_container;
            getSupportFragmentManager().beginTransaction()
                    .replace(id, fragment)
                    .addToBackStack("Book Detail")
                    .commit();
        }
        else{
            // We want to launch a different activity.
            Intent intent = new Intent(this, DetailActivity.class)
                    .putExtra(BookDetail.EAN_KEY, ean);
            startActivity(intent);
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "in onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentScanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        String eanNumber = intentScanResult.getContents();
        mAddBookFragment.eanEditText.setText(eanNumber);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(LOG_TAG, String.format("in dispatchTouchEvent, event action is ACTION_DOWN"));
            case MotionEvent.ACTION_UP:
                Log.d(LOG_TAG, String.format("in dispatchTouchEvent, event action is ACTION_UP"));
            case MotionEvent.ACTION_CANCEL:
                Log.d(LOG_TAG, String.format("in dispatchTouchEvent, event action is ACTION_CANCEL"));
            case MotionEvent.ACTION_MOVE:
                Log.d(LOG_TAG, String.format("in dispatchTouchEvent, event action is ACTION_MOVE"));
            case MotionEvent.ACTION_SCROLL:
                Log.d(LOG_TAG, String.format("in dispatchTouchEvent, event action is ACTION_SCROLL"));
            case MotionEvent.ACTION_HOVER_ENTER:
                Log.d(LOG_TAG, String.format("in dispatchTouchEvent, event action is ACTION_HOVER_ENTER"));
            default:
                Log.d(LOG_TAG, "in dispatchTouchEvent, event action is something else...");
        }
        return super.dispatchTouchEvent(ev);
    }
}