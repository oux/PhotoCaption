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
import android.provider.MediaStore;
import android.widget.Toast;
import android.os.Environment;
import android.content.ContentResolver;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.database.Cursor;
import android.view.View;
import android.view.Window;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.BitmapFactory;

import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.exif.IfdId;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import uk.co.senab.photoview.PhotoView;
// For Adapter:
import android.provider.MediaStore;
import android.content.Context;

// Add Zoom, slide and change exifInterface

public class PhotoCaptionView extends Activity
{
    static final String TAG = "photoCaptionView";
    private Uri imageUri;
    TextView descriptionView;
    // MyImageView imageView;
    GridViewAdapter adapter = null;
    ActionBar actionBar;
    ViewPager mViewPager;
    int mPosition;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view);

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
        // imageView = (MyImageView) findViewById(R.id.ImageView);
        mViewPager = (HackyViewPager) findViewById(R.id.view_pager);

        SamplePagerAdapter samplePagerAdapter = new SamplePagerAdapter();
        samplePagerAdapter.setContext(this);
		mViewPager.setAdapter(samplePagerAdapter);
        // imageView = (ImageViewTouch) findViewById(R.id.ImageView);
        descriptionView = (TextView)findViewById(R.id.Description);

        mPosition = intent.getIntExtra("position",-1);

        if (mPosition != -1)
        {
            Log.i(TAG,"new position: " + mPosition);
            mViewPager.setCurrentItem(mPosition,false);
        }
        else
        {
            if (Intent.ACTION_VIEW.equals(action)) {
                imageUri = intent.getData();
                Log.i(TAG,"Receive Adapter: " + adapter);
            } else if (Intent.ACTION_SEND.equals(action) && type != null) {
                Log.i(TAG,"Action View:" + intent.getData());
                if (type.startsWith("image/")) {
                    imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                }
            } else {
                finish();
            }
            if (imageUri.getScheme().equals("content"))
            {
                Log.i(TAG,"VIEW: Uri1:" + imageUri
                        + " uri2:"
                        + samplePagerAdapter.getPosition(Integer.valueOf(imageUri.getLastPathSegment()).intValue()));
                mPosition = samplePagerAdapter.getPosition(Integer.valueOf(imageUri.getLastPathSegment()).intValue());
                mViewPager.setCurrentItem(samplePagerAdapter.getCount() - mPosition-1,false);
            } else {
                handleImage(imageUri);
            }
        }
    }

    static class SamplePagerAdapter extends PagerAdapter {

        private PhotoCaptionView mContext;
        private int layoutResourceId;
        private Cursor externalCursor;
        private Uri externalContentUri;
        private int externalColumnIndex;
        static final String TAG = "photoCaptionPagerAdapter";

        public void setContext(PhotoCaptionView context) {
            mContext = context;
            //Do the query
            externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            String[] projection = {MediaStore.Images.Media._ID};
            String selection = "";
            String [] selectionArgs = null;
            externalCursor = mContext.getContentResolver().query(
                    externalContentUri,projection,selection,selectionArgs,null);
            externalColumnIndex = externalCursor.getColumnIndex(MediaStore.Images.Media._ID);
        }

        public int getPosition(int id) {
            //Do the query
            String[] projection = {MediaStore.Images.Media._ID};
            String selection = "_ID = ?";
            String [] selectionArgs = {String.valueOf(id)};
            externalCursor.moveToFirst();
            while (externalCursor.getInt(externalColumnIndex) != id)
            {
                externalCursor.moveToNext();
            }
            Log.i(TAG,"getPosition: position:" + externalCursor.getPosition());
            return externalCursor.getPosition();
        }

        @Override
        public int getCount() {
            return externalCursor.getCount();
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {
            Log.i(TAG,"VIEW:" + position + ", container:" + container
                    + ", context:" + container.getContext() + ", mContext:" + mContext);
            PhotoView photoView = new PhotoView(container.getContext());

            ExifInterface exifInterface = new ExifInterface();
            Bitmap preview_bitmap = null;

            externalCursor.moveToPosition(getCount()-(position+1));
            int imageID = externalCursor.getInt( externalColumnIndex );
            Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));

            Log.i(TAG,"Id:" + imageID + ", imageUri:" + imageUri);
            try {
                exifInterface.readExif(mContext.getContentResolver().openInputStream(imageUri));
            } catch (Exception e) {
                e.printStackTrace();
            }

            ExifTag tag = exifInterface.getTag(ExifInterface.TAG_USER_COMMENT);
            String description = null;
            if (tag != null)
                description = tag.getValueAsString();
            try {
                BitmapFactory.Options options=new BitmapFactory.Options();
                options.inSampleSize = 8;
                if (imageUri.getScheme().equals("content"))
                {
                    Log.i(TAG,"Content");
                    preview_bitmap=BitmapFactory.decodeFile(getRealPathFromURI(imageUri),options);
                }
                else
                    preview_bitmap=BitmapFactory.decodeFile(imageUri.getPath(),options);
                photoView.setImageBitmap(preview_bitmap);
                if (description != "")
                    mContext.descriptionView.setText(description);
                else
                    mContext.descriptionView.setVisibility(View.INVISIBLE);

                // Now just add PhotoView to ViewPager and return it
                container.addView(photoView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return photoView;
        }

        public String getRealPathFromURI(Uri uri) {
            Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            String ret = cursor.getString(idx);
            cursor.close();
            return ret;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

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
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, imageUri);
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

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        String ret = cursor.getString(idx);
        cursor.close();
        return ret;
    }

    void handleImage(Uri imageUri) {
        if (imageUri != null) {
            Log.i(TAG, "Incoming image Uri=" + imageUri + " path=" + imageUri.getPath());
            Bitmap preview_bitmap = null;
            try {
                File image = null;
                if (imageUri.getScheme().equals("content"))
                    image = new File(getRealPathFromURI(imageUri));
                else
                    image = new File(imageUri.getPath());
                BitmapFactory.Options options=new BitmapFactory.Options();
                options.inSampleSize = 8;
                preview_bitmap=BitmapFactory.decodeStream(new FileInputStream(image),null,options);
                // imageView.setImageBitmap(preview_bitmap);
                getDescription();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    void getDescription()
    {
        ExifInterface exifInterface = new ExifInterface();
        try {
            exifInterface.readExif(getContentResolver().openInputStream(imageUri));
            ExifTag tag = exifInterface.getTag(ExifInterface.TAG_USER_COMMENT);
            if (tag != null)
            {
                String description = tag.getValueAsString();
                Log.i(TAG, "image description=<" + description + ">");
                if (description != "")
                {
                    descriptionView.setText(description);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        descriptionView.setVisibility(View.INVISIBLE);
    }
}
