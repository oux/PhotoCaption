package com.oux.photocaption;

import java.util.Date;
import java.io.IOException;
import java.io.File;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;
import android.os.Environment;
import android.content.ContentResolver;
import android.graphics.Bitmap;
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
import android.graphics.drawable.BitmapDrawable;


// Add Zoom, slide and change exifInterface

public class PhotoCaptionView extends Activity
{
    static final String TAG = "photoCaptionView";
    private Uri imageUri;
    TextView descriptionView;
    ImageView imageView;
    // ImageViewTouch imageView;
    GridViewAdapter adapter = null;
    ExifInterface mExif;
    ActionBar actionBar;

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
        imageView = (ImageView) findViewById(R.id.ImageView);
        // imageView = (ImageViewTouch) findViewById(R.id.ImageView);
        descriptionView = (TextView)findViewById(R.id.Description);

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
            Log.i(TAG,"VIEW: Uri1:" + imageUri + " uri2:" +
                    intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT));
            File image = new File(getRealPathFromURI(imageUri));
            handleImage(Uri.fromFile(image));
        } else {
            handleImage(imageUri);
        }
    }

    @Override
    public void onDestroy()
    {
        if (imageView != null)
        {
            BitmapDrawable bd = (BitmapDrawable)imageView.getDrawable();
            if (bd != null)
                bd.getBitmap().recycle();
            imageView.setImageBitmap(null);
        }
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
        return cursor.getString(idx); 
    }

    void handleImage(Uri imageUri) {
        if (imageUri != null) {
            Log.i(TAG, "Incoming image Uri=" + imageUri + " path=" + imageUri.getPath());
            imageView.setImageURI(imageUri);
            openExif(imageUri);
            getDescription();
        }
    }

    void openExif(Uri imageUri)
    {
        try {
            mExif = new ExifInterface(imageUri.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error during loading Exif", 
                    Toast.LENGTH_LONG).show();
        }
    }

    void getDescription()
    {
        String description = mExif.getAttribute("UserComment");
        if (description != null)
        {
            try {
                descriptionView.setText(mExif.getAttribute("UserComment"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
        {
                descriptionView.setVisibility(View.INVISIBLE);
        }
    }
}
