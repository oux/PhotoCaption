package com.oux.photocaption;

import java.util.Date;
// import java.io.FileInputStream;
// import java.io.OutputStream;
// import java.io.BufferedOutputStream;
// import java.io.FileOutputStream;
import java.io.File;
// import java.io.IOException;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.ActionBar;
// import android.os.ResultReceiver;
// import android.os.Handler;
// import android.os.Message;
import android.app.ActionBar.OnNavigationListener;
import android.os.Bundle;
import android.os.Environment;
import android.graphics.Bitmap;
import android.util.Log;
import android.content.Intent;
import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.SpinnerAdapter;
import android.widget.ArrayAdapter;
import android.database.Cursor;
import android.view.Window;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.graphics.drawable.BitmapDrawable;

import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.exif.IfdId;

public class PhotoCaptionEdit extends Activity
{
    static final String TAG = "photoCaptionEdit";
    private Uri imageUri;
    private static int TAKE_PICTURE = 100;
    EditText descriptionView;
    ImageView imageView;
    File mFile;
    ActionBar actionBar;
    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        // actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        // actionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationCallback);
        actionBar.setTitle(R.string.app_name);
        actionBar.setSubtitle(R.string.mode_edit);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        imageView = (ImageView) findViewById(R.id.ImageView);
        descriptionView = (EditText)findViewById(R.id.Description);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(descriptionView, InputMethodManager.SHOW_IMPLICIT);

        // TODO:
        // ACTION_REVIEW.equalsIgnoreCase(action)...
        if (Intent.ACTION_EDIT.equals(action))
        {
            imageUri = intent.getData();
            handleImage();
        } else if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                handleImage();
            }
        } else {
            takePhoto();
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_capture:
                takePhoto();
                return true;
            case R.id.action_view:
                intent = new Intent(Intent.ACTION_VIEW, imageUri, this,PhotoCaptionView.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.action_gallery:
                intent = new Intent(this,PhotoCaptionGallery.class);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                /* behave bad:
                try {
                    MediaStore.Images.Media.insertImage(getContentResolver(), imageUri.getPath(), "ceci est le titre" , "ceci est une description");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                */
                scanMedia(imageUri.getPath());
                handleImage();
            } else {
                finish();
            }
        }
    }

    public void takePhoto() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        // intent.setClassName("com.android.gallery3d","com.android.camera.CameraActivity");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        File path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES);
        Log.i(TAG,"path:" + path);
        File photo = new File(path,  "CAP_"+ sdf.format(new Date()) +".jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    private void scanMedia(String path) {
        //TODO: try insertImage(... description)
        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        Intent scanFileIntent = new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri);
        sendBroadcast(scanFileIntent);
    }

    /*
       public void sharePhoto() {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND, Uri.parse("file:///sdcard/image.png"));
        shareIntent.setType("image/png");
        this.setResult(Activity.RESULT_OK, shareIntent);
        this.finish();
    }
    */

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        String ret = cursor.getString(idx);
        cursor.close();
        return ret;
    }

    void handleImage() {
        if (imageUri != null) {
            /*
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize = 8;
            Bitmap preview_bitmap=BitmapFactory.decodeStream(is,null,options);
            */
            imageView.setImageURI(imageUri);
            getDescription();
        }
    }


    void getDescription()
    {
        ExifInterface exifInterface = new ExifInterface();
        try {
            exifInterface.readExif(getContentResolver().openInputStream(imageUri));
        } catch (Exception e) {
            e.printStackTrace();
        }
        ExifTag tag = exifInterface.getTag(ExifInterface.TAG_USER_COMMENT);
        if (tag != null)
        {
            descriptionView.setText(tag.getValueAsString());
            Log.i(TAG,"Description view is focusable: " + descriptionView.isFocusable());
            Log.i(TAG,"Description view is focusableInTouchMode: " + descriptionView.isFocusableInTouchMode());
            descriptionView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(descriptionView, InputMethodManager.SHOW_IMPLICIT);
        }
    }

  void setDescription(String description)
  {
      Log.i(TAG,"Setting description:" + description + " on " + imageUri);
      ExifInterface exifInterface = new ExifInterface();
      description = description.replaceAll("[áâã]", "a");
      description = description.replaceAll("[éèë]", "e");
      description = description.replaceAll("[ç]", "c");
      ExifTag tag = exifInterface.buildTag(ExifInterface.TAG_USER_COMMENT, description);
      if(tag != null) {
          exifInterface.setTag(tag);
      }
      try {
          if (imageUri.getScheme().equals("content"))
          {
              Log.i(TAG,"Content");
              exifInterface.forceRewriteExif(getRealPathFromURI(imageUri));
          } else {
              Log.i(TAG,"File");
              exifInterface.forceRewriteExif(imageUri.getPath());
          }
      } catch (Exception e) {
          Log.e(TAG, "forceRewriteExif");
          e.printStackTrace();
      }
  }

}
