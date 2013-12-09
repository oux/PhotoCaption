package com.oux.photocaption;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
// import java.io.OutputStream;
// import java.io.BufferedOutputStream;
// import java.io.FileOutputStream;
import java.io.File;
// import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
// import android.os.ResultReceiver;
// import android.os.Handler;
// import android.os.Message;
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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.BitmapFactory;

import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.exif.IfdId;

public class PhotoCaptionEdit extends Activity
{
    static final String TAG = "photoCaptionEdit";
    private Uri imageUri;
    private String mInitialDescription;
    private static int SHOT = 100;
    EditText descriptionView;
    ImageView imageView;
    File mFile;
    ActionBar actionBar;
    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";
    AlertDialog saveDialog;
    AlertDialog deleteDialog;
    static boolean mBackToShot = false;

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

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        imageView = (ImageView) findViewById(R.id.ImageView);
        descriptionView = (EditText)findViewById(R.id.Description);
        saveDialog = new AlertDialog.Builder(
                this).create();

        saveDialog.setTitle(R.string.save);
        saveDialog.setMessage(getResources().getString(R.string.ask_save_description));
        saveDialog.setIcon(android.R.drawable.ic_menu_save);
        saveDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getResources().getString(R.string.donotsave), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        saveDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                setDescription(descriptionView.getText().toString());
                Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.saved), Toast.LENGTH_SHORT).show();
                if (mBackToShot)
                {
                    takePhoto();
                }
                else
                {
                    Intent intent = new Intent(getApplicationContext(),PhotoCaptionGallery.class);
                    startActivity(intent);
                    finish();
                }
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
                //finish();
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
        mBackToShot=false;
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
            case R.id.action_del:
                deleteDialog.show();
                return true;
            case R.id.action_cancel:
                if (mInitialDescription.equals(descriptionView.getText().toString()))
                    finish();
                else
                    saveDialog.show();
                return true;
            case R.id.action_gallery:
                intent = new Intent(this,PhotoCaptionGallery.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.action_save:
                setDescription(descriptionView.getText().toString());
                if (mBackToShot)
                {
                    takePhoto();
                }
                else
                {
                    intent = new Intent(this,PhotoCaptionGallery.class);
                    startActivity(intent);
                    finish();
                }
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
    public void onResume() {
        super.onResume();
        // TODO: restore last edittext content
        Log.i(TAG,"onResume");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SHOT) {
            if (resultCode == Activity.RESULT_OK) {
                scanMedia(imageUri.getPath());
                handleImage();
            } else {
                finish();
            }
        }
    }

    public void takePhoto() {
        mBackToShot = true;
        List<Intent> targetedIntents = new ArrayList<Intent>();
        Context context = getApplicationContext();
        final PackageManager pm = context.getPackageManager();

        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        Log.i(TAG,"path:" + path);
        File photo = new File(path,  "CAP_"+ sdf.format(new Date()) +".jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);

        List<ResolveInfo> appInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (appInfoList.isEmpty()) {
            Log.e(TAG,"No application found to take a shot");
            Toast.makeText(context,
                    getResources().getString(R.string.noapptoshot), Toast.LENGTH_SHORT).show();
            finish();
        }

        ArrayList<Intent> extraIntents = new ArrayList<Intent>();

        for (ResolveInfo ri : appInfoList) {
            String packageName = ri.activityInfo.packageName;
            Log.i(TAG,"packageName:" + packageName);
            if (!packageName.equals(context.getPackageName())) {
                Intent it = new Intent("android.media.action.IMAGE_CAPTURE");
                it.setComponent(new ComponentName(packageName, ri.activityInfo.name));
                it.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photo));
                extraIntents.add(it);
            }
        }

        if (extraIntents.isEmpty())
        {
            Log.e(TAG,"No application found to take a shot");
            Toast.makeText(context,
                    getResources().getString(R.string.noapptoshot), Toast.LENGTH_SHORT).show();
            finish();
        }
        Intent chooserIntent = Intent.createChooser( extraIntents.remove(extraIntents.size() -1), "Choose your camera to take the shot");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toArray(new Parcelable[] {}));
        startActivityForResult(chooserIntent,SHOT);

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
                imageView.setImageBitmap(preview_bitmap);
                mInitialDescription = getDescription();
                descriptionView.setText(mInitialDescription);
            } catch (FileNotFoundException e) {
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
        }
        ExifTag tag = exifInterface.getTag(ExifInterface.TAG_USER_COMMENT);
        if (tag != null)
        {
            return tag.getValueAsString();
        }
        return "";
    }

  void setDescription(String description)
  {
      Log.i(TAG,"Setting description:" + description + " on " + imageUri);
      ExifInterface exifInterface = new ExifInterface();
      description = description.replaceAll("[ïî]", "i");
      description = description.replaceAll("[àáâã]", "a");
      description = description.replaceAll("[éèë]", "e");
      description = description.replaceAll("[ç]", "c");
      description = description.replaceAll("[ô]", "o");
      description = description.replaceAll("[ù]", "u");
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

  @Override
  protected void onStop() {
      super.onStop();
  }
}
