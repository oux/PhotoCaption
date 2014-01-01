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
    EditText descriptionView;
    ImageView imageView;
    File mFile;
    ActionBar actionBar;
    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";
    AlertDialog saveDialog;
    AlertDialog deleteDialog;
    static boolean mBackToShot = false;
    Point mSize;

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
                Intent intent = new Intent(this,PhotoCaptionGallery.class);
                startActivity(intent);
                finish();
            }
        }
    }

    public void takePhoto() {
        mBackToShot = true;
        List<Intent> targetedIntents = new ArrayList<Intent>();
        // Context context = getApplicationContext();
        final PackageManager pm = getPackageManager();

        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        Log.i(TAG,"path:" + path);
        File photo = new File(path,  "CAP_"+ sdf.format(new Date()) +".jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String packageName = sharedPrefs.getString("pref_edit_camapp", "");

        if (isPackageExisted(packageName)) {
            intent.setPackage(packageName);
            startActivityForResult(intent,SHOT);
        } else {
            List<ResolveInfo> appInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (appInfoList.isEmpty()) {
                Log.e(TAG,"No application found to take a shot");
                Toast.makeText(this,
                        getResources().getString(R.string.noapptoshot), Toast.LENGTH_SHORT).show();
                finish();
            }

            ArrayList<Intent> extraIntents = new ArrayList<Intent>();

            for (ResolveInfo ri : appInfoList) {
                packageName = ri.activityInfo.packageName;
                Log.i(TAG,"packageName:" + packageName);
                if (!packageName.equals(this.getPackageName())) {
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
                Toast.makeText(this,
                        getResources().getString(R.string.noapptoshot), Toast.LENGTH_SHORT).show();
                finish();
            }
            Intent chooserIntent = Intent.createChooser( extraIntents.remove(extraIntents.size() -1),
                    getResources().getString(R.string.choose_cam));
            Log.i(TAG,"Number of apps on choice:" + extraIntents.size());
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toArray(new Parcelable[] {}));
            startActivityForResult(chooserIntent,SHOT);
        }
    }

    public boolean isPackageExisted(String targetPackage){
        PackageManager pm=getPackageManager();
        try {
            PackageInfo info=pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            return false;
        }  
        return true;
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
        }
        ExifTag tag = exifInterface.getTag(ExifInterface.TAG_USER_COMMENT);
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

      ExifTag tag = exifInterface.buildTag(ExifInterface.TAG_USER_COMMENT, decompose(description));
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
