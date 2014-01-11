package com.oux.photocaption;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.Charset;
import java.io.File;
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

public class PhotoCaptionCapture extends Activity
{
    static final String TAG = "PhotoCaptionCapture";
    private Uri imageUri;
    private static int SHOT = 100;
    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";
    SharedPreferences mSharedPrefs;
    private boolean mBackToShot = true;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.edit);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Get intent
        Intent intent = getIntent();
        mBackToShot = intent.getBooleanExtra("backToShot",true);
        Log.i(TAG,"mBackToShot:" + mBackToShot);

        // TODO:
        // ACTION_REVIEW.equalsIgnoreCase(action)...
        Log.d(TAG,"Taking a photo.");
        takePhoto();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SHOT) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG,"onActivityResult => scanMedia");
                scanMedia(imageUri.getPath());
                Log.i(TAG,"onActivityResult => RESULT_OK");
                Intent intent = new Intent(this,PhotoCaptionEdit.class);
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                if (mBackToShot)
                    intent.putExtra("backToShot",true);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(this,PhotoCaptionGallery.class);
                startActivity(intent);
                finish();
            }
        }
    }

    public void takePhoto() {
        List<Intent> targetedIntents = new ArrayList<Intent>();
        // Context context = getApplicationContext();
        final PackageManager pm = getPackageManager();

        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        File path = Environment.getExternalStoragePublicDirectory(
                mSharedPrefs.getString("pref_capture_directory", Environment.DIRECTORY_PICTURES)
                );
        Log.i(TAG,"path:" + path);
        File photo = new File(path,  "CAP_"+ sdf.format(new Date()) +".jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);

        String packageName = mSharedPrefs.getString("pref_capture_camapp", "");

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
}
