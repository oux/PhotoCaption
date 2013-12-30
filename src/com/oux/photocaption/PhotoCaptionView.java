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
import android.widget.Button;
import android.view.Window;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.app.ActionBar;
// import android.graphics.drawable.BitmapDrawable;

import android.support.v4.view.ViewPager;

// Add Zoom, slide and change exifInterface

public class PhotoCaptionView extends Activity
{
    static final String TAG = "photoCaptionView";
    private Uri imageUri;
    ActionBar actionBar;
    ViewPager mViewPager;
    PhotoCaptionPagerAdapter mPagerAdapter;
    int mPosition;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);

        Log.i(TAG,"onCreate");

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        // actionBar.hide();

        actionBar.setTitle(R.string.app_name);
        actionBar.setSubtitle(R.string.mode_view);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        mViewPager = (HackyViewPager) findViewById(R.id.view_pager);

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
                Toast.makeText(this,getResources().getString(R.string.not_able_to_perform), Toast.LENGTH_SHORT).show();
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
                // To be implemented
            }
        }

        if (mPosition == -1) {
            mPagerAdapter.forceUri(imageUri);
        }
        Log.i(TAG,"new position: " + mPosition);
        mViewPager.setCurrentItem(mPosition,false);

    }

    @Override
    public void onPause() {
        Log.i(TAG,"onPause");
        finish();
        super.onPause();
    }


    @Override
    public void onDestroy()
    {
        /*
        if (imageView != null)
        {
            BitmapDrawable bd = (BitmapDrawable)imageView.getDrawable();
            if (bd != null)
                bd.getBitmap().recycle();
            imageView.setImageBitmap(null);
        }
        */
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_actions, menu);
        return super.onCreateOptionsMenu(menu);
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
            case R.id.action_gallery:
                intent = new Intent(getApplicationContext(),PhotoCaptionGallery.class);
                startActivity(intent);
                finish();
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
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // TODO: re enable is code
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (actionBar.isShowing()) {
                actionBar.hide();
            } else {
                actionBar.show();
            }
        }
        return true;
    }

    public void sharePhoto() {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND,
                Uri.parse("file:///sdcard/image.png"));
        shareIntent.setType("image/png");
        this.setResult(Activity.RESULT_OK, shareIntent);
        this.finish();
    }
}
