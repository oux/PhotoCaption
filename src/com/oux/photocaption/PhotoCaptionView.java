package com.oux.photocaption;

import java.util.Date;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import android.os.Environment;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.widget.Button;
import android.view.Window;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.app.ActionBar;
import android.widget.ShareActionProvider;
import android.preference.PreferenceManager;

import android.support.v4.view.ViewPager;

public class PhotoCaptionView extends Activity
{
    static final String TAG = "photoCaptionView";
    private static int SETTINGS = 101;
    private Uri imageUri;
    ActionBar actionBar;
    ViewPager mViewPager;
    PhotoCaptionPagerAdapter mPagerAdapter;
    int mPosition;
    ShareActionProvider mShareActionProvider;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);

        Log.i(TAG,"onCreate");

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPrefs.getBoolean("pref_view_action_bar", false))
            actionBar.hide();

        actionBar.setTitle(R.string.app_name);
        actionBar.setSubtitle(R.string.mode_view);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        mViewPager = (HackyViewPager) findViewById(R.id.view_pager);

        // Check PagerAdapter Constructors vs GridViewAdapter Constructors to pass Context
        mPagerAdapter = new PhotoCaptionPagerAdapter();
        mPagerAdapter.setContext(this);
		mViewPager.setAdapter(mPagerAdapter);

        mPosition = intent.getIntExtra("position",-1);

        if (mPosition == -1)
        {
            if (Intent.ACTION_VIEW.equals(action)) {
                Log.i(TAG,"Action View:" + intent.getData());
                imageUri = intent.getData();
            } else if (Intent.ACTION_SEND.equals(action) && type != null) {
                Log.i(TAG,"Action Send:" + intent.getData());
                imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            } else {
                Toast.makeText(this,getResources().getString(R.string.not_able_to_perform), Toast.LENGTH_LONG).show();
                finish();
            }
            if (imageUri.getScheme().equals("content"))
            {
                Log.i(TAG,"VIEW: Uri:" + imageUri
                        + " uri2:"
                        + mPagerAdapter.getPosition(Long.valueOf(imageUri.getLastPathSegment()).longValue()));
                mPosition = mPagerAdapter.getPosition(Long.valueOf(imageUri.getLastPathSegment()).longValue());
            } else if (imageUri.getScheme().equals("file"))
            {
                Log.i(TAG,"VIEW: Uri:" + imageUri
                        + " uri2:"
                        + mPagerAdapter.getPosition(imageUri.getPath()));
                mPosition = mPagerAdapter.getPosition(imageUri.getPath());
            } else {
                Log.i(TAG,"To be implemented: Scheme:" + imageUri.getScheme() + ", Uri:" + imageUri);
                Toast.makeText(this,getResources().getString(R.string.not_able_to_perform), Toast.LENGTH_LONG).show();
                finish();
            }
        }

        if (mPosition == -1) {
            mPagerAdapter.forceUri(imageUri);
        }
        Log.i(TAG,"new position: " + mPosition);
        mViewPager.setCurrentItem(mPosition,false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_actions, menu);

        // Set up ShareActionProvider's default share intent
        MenuItem shareItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider)
            shareItem.getActionProvider();
        mShareActionProvider.setShareIntent(getDefaultIntent());

        return super.onCreateOptionsMenu(menu);
    }

    private Intent getDefaultIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        String description;
        if (mPosition == -1)
        {
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            description = mPagerAdapter.getDescription(imageUri);
        }
        else
        {
            intent.putExtra(Intent.EXTRA_STREAM, mPagerAdapter.getUri(mViewPager.getCurrentItem()));
            description = mPagerAdapter.getDescription(mPagerAdapter.getUri(mViewPager.getCurrentItem()));
        }
        if (description.equals("") || description.equals("<BINARY DATA>"))
            intent.putExtra(android.content.Intent.EXTRA_TEXT, getResources().getString(R.string.shared_via_photocaption) );
        else
            intent.putExtra(android.content.Intent.EXTRA_TEXT, getResources().getString(R.string.shared_via_photocaption) + ":\n" + description );

        return intent;
    }

    // Somewhere in the application.
    public void doShare(Intent shareIntent) {
        // When you want to share set the share intent.
        mShareActionProvider.setShareIntent(shareIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_capture:
                intent = new Intent(getApplicationContext(),PhotoCaptionEdit.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.action_settings:
                intent = new Intent(getApplicationContext(),PhotoCaptionSettings.class);
                startActivityForResult(intent,SETTINGS);
                return true;
            case R.id.action_edit:
                intent = new Intent(getApplicationContext(),PhotoCaptionEdit.class);
                Log.i(TAG,"Current:" + mViewPager.getCurrentItem());
                intent.setAction(Intent.ACTION_SEND);
                if (mPosition == -1)
                {
                    intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                }
                else
                {
                    intent.putExtra(Intent.EXTRA_STREAM, mPagerAdapter.getUri(mViewPager.getCurrentItem()));
                }
                intent.setType("image/jpeg");
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS) {
            // Trick to invalidate pageView
            mViewPager.setAdapter(mPagerAdapter);
            mViewPager.setCurrentItem(mPosition,false);
        }
    }


    public void toggleActionBar() {
        if (actionBar.isShowing()) {
            actionBar.hide();
        } else {
            actionBar.show();
        }
    }
}
