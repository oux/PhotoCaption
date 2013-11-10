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
import android.view.View.OnClickListener;


public class PhotoCaptionView extends Activity
{
    static final String TAG = "photoCaptionView";
    private Uri imageUri;
    private static int TAKE_PICTURE = 1;    
    TextView descriptionView;
    ImageView imageView;
    boolean edit = false;
    ExifInterface mExif;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (edit) {
            setContentView(R.layout.edit);
        }
        else
        {
            setContentView(R.layout.view);
        }

        Log.i(TAG,"onCreate");

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        imageView = (ImageView) findViewById(R.id.ImageView);
        descriptionView = (TextView)findViewById(R.id.Description);

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                Log.i(TAG,"Uri1:" + imageUri + " uri2:" + intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT) + " real path:" + getRealPathFromURI(imageUri));
                File image = new File(getRealPathFromURI(imageUri));
                handleImage(Uri.fromFile(image));
            }
        } else {
            takePhoto();
        }
    }
    private OnClickListener SaveListener = new OnClickListener() {
        public void onClick(View v) {
            setDescription(descriptionView.getText().toString());
        }
    };


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
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
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

    private String getTagString(String tag)
    {
        return(tag + " : " + mExif.getAttribute(tag) + "\n");
    }

    private void showDescription()
    {
        /*
           String myAttribute="Exif information ---\n";
           myAttribute += getTagString(ExifInterface.TAG_DATETIME);
           myAttribute += getTagString(ExifInterface.TAG_FLASH);
           myAttribute += getTagString(ExifInterface.TAG_GPS_LATITUDE);
           myAttribute += getTagString(ExifInterface.TAG_GPS_LATITUDE_REF);
           myAttribute += getTagString(ExifInterface.TAG_GPS_LONGITUDE);
           myAttribute += getTagString(ExifInterface.TAG_GPS_LONGITUDE_REF);
           myAttribute += getTagString(ExifInterface.TAG_IMAGE_LENGTH);
           myAttribute += getTagString(ExifInterface.TAG_IMAGE_WIDTH);
           myAttribute += getTagString(ExifInterface.TAG_MAKE);
           myAttribute += getTagString(ExifInterface.TAG_MODEL);
           myAttribute += getTagString(ExifInterface.TAG_ORIENTATION);
           myAttribute += getTagString(ExifInterface.TAG_WHITE_BALANCE);
           myAttribute += getTagString("UserComment");
           descriptionView.setText(myAttribute);
         */
        descriptionView.setText(mExif.getAttribute("UserComment"));
    }
        
}

