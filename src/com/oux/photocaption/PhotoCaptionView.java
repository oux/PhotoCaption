package com.oux.photocaption;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import java.util.Date;
import android.content.Intent;
import android.media.ExifInterface;
import android.view.View;
import java.io.File;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;
import android.os.Environment;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.database.Cursor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import android.view.Window;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.app.ActionBar;


public class PhotoCaptionView extends Activity
{
    static final String TAG = "photoCaptionView";
    private Uri imageUri;
    TextView descriptionView;
    ImageView imageView;
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

        actionBar.setSubtitle("View Mode");

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        imageView = (ImageView) findViewById(R.id.ImageView);
        descriptionView = (TextView)findViewById(R.id.Description);

        if (Intent.ACTION_VIEW.equals(action)) {
            imageUri = intent.getData();
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
                intent = new Intent(this,PhotoCaptionEdit.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.action_edit:
                intent = new Intent(this,PhotoCaptionEdit.class);
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                intent.setType("image/jpeg");
                startActivity(intent);
                finish();
                return true;
            case android.R.id.home:
                // TODO: go to our gallery tiled
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

            try {
                mExif = new ExifInterface(imageUri.getPath());
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error during loading Exif", 
                        Toast.LENGTH_LONG).show();
            }
            getDescription();
        }
    }

    void getDescription()
    {
        try {
            descriptionView.setText(mExif.getAttribute("UserComment"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
