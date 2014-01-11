package com.oux.photocaption;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.Charset;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ActionBar.OnNavigationListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.graphics.Bitmap;
import android.util.Log;
import android.content.Intent;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.ComponentName;
import android.content.pm.LabeledIntent;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.SpinnerAdapter;
import android.widget.ArrayAdapter;
import android.database.Cursor;
import android.view.Display;
import android.view.Window;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.content.SharedPreferences;

import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.exif.IfdId;
import android.preference.PreferenceManager;

public class PhotoCaptionEdit extends Activity
{
    static final String TAG = "PhotoCaptionEdit";
    private static final String CAMAPP = "pref_edit_camapp";
    private Uri imageUri;
    private String mInitialDescription;
    private static int SHOT = 100;
    private static int SETTINGS = 101;
    EditText descriptionView;
    ImageView imageView;
    File mFile;
    ActionBar actionBar;
    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";
    AlertDialog saveDialog;
    AlertDialog deleteDialog;
    static boolean mBackToShot = false;
    static boolean mOneMoreShot = false;
    Point mSize;
    SharedPreferences mSharedPrefs;
    int mTagId;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.app_name);
        actionBar.setSubtitle(R.string.mode_edit);

        Display display = getWindowManager().getDefaultDisplay();

        mSize = new Point();
        display.getSize(mSize);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String tagId = mSharedPrefs.getString("pref_edit_exif_field", Integer.toString(ExifInterface.TAG_USER_COMMENT));
        mTagId = Integer.parseInt(tagId);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Log.d(TAG,"intent:" + intent + " action:" + action + " type:" + type);
        imageView = (ImageView) findViewById(R.id.ImageView);
        descriptionView = (EditText)findViewById(R.id.Description);
        mBackToShot = intent.getBooleanExtra("backToShot",false);
        Log.i(TAG,"mBackToShot:" + mBackToShot);

        // Dialogs
        saveDialog = new AlertDialog.Builder(
                this).create();

        saveDialog.setTitle(R.string.save);
        saveDialog.setMessage(getResources().getString(R.string.ask_save_description));
        saveDialog.setIcon(android.R.drawable.ic_menu_save);
        saveDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getResources().getString(R.string.donotsave), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (mOneMoreShot)
                {
                    Intent intent = new Intent(getApplicationContext(),PhotoCaptionCapture.class);
                    startActivity(intent);
                }
                finish();
            }
        });
        saveDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                setDescription(descriptionView.getText().toString());
                Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.saved), Toast.LENGTH_SHORT).show();
                if (mBackToShot || mOneMoreShot)
                {
                    Intent intent = new Intent(getApplicationContext(),PhotoCaptionCapture.class);
                    startActivity(intent);
                }
                finish();
            }
        });

        deleteDialog = new AlertDialog.Builder(
                this).create();

        deleteDialog.setTitle(getResources().getString(R.string.delete));
        deleteDialog.setMessage(getResources().getString(R.string.ask_delete_image));
        deleteDialog.setIcon(android.R.drawable.ic_menu_delete);
        deleteDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getResources().getString(R.string.donotdelete), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        deleteDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.deleted), Toast.LENGTH_SHORT).show();
                getContentResolver().delete(imageUri,null,null);
                finish();
            }
        });

        // TODO:
        // ACTION_REVIEW.equalsIgnoreCase(action)...
        if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action))
        {
            Log.d(TAG,"Action: "+ action);
            imageUri = intent.getData();
            handleImage();
        } else if (Intent.ACTION_SEND.equals(action) && type != null) {
            Log.d(TAG,"Action: Send");
            if (type.startsWith("image/")) {
                imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                handleImage();
            }
        } else {
            Log.d(TAG,"Nothing todo");
            finish();
        }
    }

    @Override
    public void onDestroy()
    {
        if (imageView != null)
        {
            BitmapDrawable bd = (BitmapDrawable)imageView.getDrawable();
            if (bd != null && bd.getBitmap() != null)
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
                if (mInitialDescription.equals(descriptionView.getText().toString()))
                {
                    intent = new Intent(getApplicationContext(),PhotoCaptionCapture.class);
                    startActivity(intent);
                    finish();
                }
                else
                {
                    mOneMoreShot=true;
                    saveDialog.show();
                }
                return true;
            case R.id.action_del:
                deleteDialog.show();
                return true;
            case R.id.action_cancel:
                if (mInitialDescription.equals(descriptionView.getText().toString()))
                {
                    intent = new Intent(getApplicationContext(),PhotoCaptionGallery.class);
                    startActivity(intent);
                    finish();
                }
                else
                    saveDialog.show();
                return true;
            case R.id.action_settings:
                intent = new Intent(getApplicationContext(),PhotoCaptionSettings.class);
                startActivityForResult(intent,SETTINGS);
                return true;
            case R.id.action_save:
                setDescription(descriptionView.getText().toString());
                if (mBackToShot)
                    intent = new Intent(getApplicationContext(),PhotoCaptionCapture.class);
                else
                    intent = new Intent(getApplicationContext(),PhotoCaptionGallery.class);
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mInitialDescription.equals(descriptionView.getText().toString()))
            finish();
        else
            saveDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS) {
            String tagId = mSharedPrefs.getString("pref_edit_exif_field", Integer.toString(ExifInterface.TAG_USER_COMMENT));
            mTagId = Integer.parseInt(tagId);
            handleImage();
        }
    }

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
            Log.i(TAG, "Incoming image Uri=" + imageUri + " path=" + imageUri.getPath());
            Bitmap preview_bitmap = null;
            try {
                String image;
                if (imageUri.getScheme().equals("content"))
                    image = getRealPathFromURI(imageUri);
                else
                    image = imageUri.getPath();

                BitmapFactory.Options options=new BitmapFactory.Options();
                options.inJustDecodeBounds=true;
                BitmapFactory.decodeFile(image ,options);

                int h=(int) Math.ceil(options.outHeight/(float)mSize.y);
                int w=(int) Math.ceil(options.outWidth/(float)mSize.x);

                if(h>1 || w>1){
                    if(h>w){
                        options.inSampleSize=h;

                    }else{
                        options.inSampleSize=w;
                    }
                }
                options.inJustDecodeBounds=false;
                preview_bitmap=BitmapFactory.decodeFile(image,options);
                imageView.setImageBitmap(preview_bitmap);
                mInitialDescription = getDescription();
                descriptionView.setText(mInitialDescription);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    String getDescription()
    {
        ExifInterface exifInterface = new ExifInterface();
        try {
            exifInterface.readExif(getContentResolver().openInputStream(imageUri));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        ExifTag tag = exifInterface.getTag(mTagId);
        if (tag != null)
        {
            String description = tag.getValueAsString();
            CharsetEncoder encoder =
                    Charset.forName("US-ASCII").newEncoder();

            if (encoder.canEncode(description)) {
                return description;
            } else {
                return "<BINARY DATA>";
            }
        }
        return "";
    }

    public String decompose(String s) {
        return java.text.Normalizer.normalize(s,
                java.text.Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+","");
    }

  void setDescription(String description)
  {
      Log.i(TAG,"Setting description:" + description + " on " + imageUri);
      ExifInterface exifInterface = new ExifInterface();

      ExifTag tag = exifInterface.buildTag(mTagId, decompose(description));
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
          Toast.makeText(this,
                    getResources().getString(R.string.tagnotsaved), Toast.LENGTH_LONG).show();
          Log.e(TAG, "forceRewriteExif");
          e.printStackTrace();
      }
  }
}
