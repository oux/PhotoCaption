package com.oux.photocaption;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import java.util.Date;
import android.content.Intent;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.app.ActionBar;
import android.os.ResultReceiver;
import android.os.Handler;
import android.os.Message;

public class PhotoCaptionEdit extends Activity
{
    static final String TAG = "photoCaptionEdit";
    private Uri imageUri;
    private static int TAKE_PICTURE = 1;    
    TextView descriptionView;
    ImageView imageView;
    ExifInterface mExif;
    ActionBar actionBar;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        // TODO: switch between edit and view:
        // actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        actionBar.setSubtitle("Edit Mode");

        /*
           actionBar.setDisplayOptions(
           ActionBar.DISPLAY_SHOW_CUSTOM,
           ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
           | ActionBar.DISPLAY_SHOW_TITLE);

         */
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        imageView = (ImageView) findViewById(R.id.ImageView);
        descriptionView = (TextView)findViewById(R.id.Description);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(descriptionView, InputMethodManager.SHOW_IMPLICIT);

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Log.i(TAG,"Action Edit:" + intent.getData());
            if (type.startsWith("image/")) {
                imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri.getScheme().equals("content")) 
                {
                    Log.i(TAG,"Uri1:" + imageUri + " uri2:" +
                            intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT));
                    File image = new File(getRealPathFromURI(imageUri));
                    handleImage(Uri.fromFile(image));
                } else {
                    handleImage(imageUri);
                }
            }
        } else {
            takePhoto();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_capture:
                takePhoto();
                return true;
            case R.id.action_gallery:
                intent = new Intent(Intent.ACTION_VIEW, imageUri, this,PhotoCaptionView.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.action_save:
                setDescription(descriptionView.getText().toString());
                intent = new Intent(Intent.ACTION_VIEW, imageUri, this,PhotoCaptionView.class);
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

    /*
    public class H extends Handler
    {
        public void handleMessage(Message msg) {
            Log.i(TAG,"Receive Result: " + msg);
        }
    }

    public class RR extends ResultReceiver
    {
        public void RR (Handler handler)
        {
            Log.i(TAG,"Constructor");
            super(handler);
        }

        @Override
        public void onReceiveResult(int resultCode, Bundle resultData)
        {
            Log.i(TAG,"Receive Result: " + resultCode);
        }
    }
     */

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG,"onResume");
        // Handler h=new H();
        // ResultReceiver rr = new RR(h);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(descriptionView, InputMethodManager.SHOW_IMPLICIT);
        // imm.showSoftInput(descriptionView, InputMethodManager.SHOW_IMPLICIT, rr);
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

    public void takePhoto() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        // IMG_20130510_135404.jpg
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        File photo = new File(Environment.getExternalStorageDirectory()+ "/DCIM/Camera",  "CAP_"+ sdf.format(new Date()) +".jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    private void scanMedia(String path) {
        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        Intent scanFileIntent = new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri);
        sendBroadcast(scanFileIntent);
    }

    public void sharePhoto() {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND, Uri.parse("file:///sdcard/image.png")); 
        shareIntent.setType("image/png");
        this.setResult(Activity.RESULT_OK, shareIntent); 
        this.finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PICTURE)
        {
            if (resultCode == Activity.RESULT_OK) {
                getContentResolver().notifyChange(imageUri, null);
                scanMedia(imageUri.getPath());
                handleImage(imageUri);
            }
        }
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

    void setDescription(String description)
    {
        mExif.setAttribute("UserComment",description);
        try {
            mExif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error during saving Exif", 
                    Toast.LENGTH_LONG).show();
        }
    }
}
