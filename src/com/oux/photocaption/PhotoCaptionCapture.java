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
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;

import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.exif.IfdId;

public class PhotoCaptionCapture extends Activity
{
    static final String TAG = "PhotoCaptionCapture";
    private Uri imageUri;
    private static int SHOT = 100;
    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";
    SharedPreferences mSharedPrefs;
    private boolean mBackToShot = true;
    ArrayList<String> camApps = new ArrayList<String>();

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

        final String prefPackageName = mSharedPrefs.getString("pref_capture_camapp", "");

        if (isPackageExisted(prefPackageName)) {
            askShot(prefPackageName);
        } else {
            final PackageManager pm = getPackageManager();
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            List<ResolveInfo> appInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (appInfoList.isEmpty())
                noCamAppFound();

            for (ResolveInfo ri : appInfoList) {
                String packageName = ri.activityInfo.packageName;
                Log.i(TAG,"packageName:" + packageName);
                if (!packageName.equals(this.getPackageName())) {
                    camApps.add(packageName);
                }
            }

            if (camApps.isEmpty())
                noCamAppFound();
            else if (camApps.size() == 1)
            {
                String packageName = camApps.get(0);
                askShot(packageName);
            }
            else
            {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.pref_capture_camapp_summ);
                builder.setItems(
                        camApps.toArray(new CharSequence[] {}),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String packageName = camApps.get(which);
                                if (!prefPackageName.equals(getResources().getString(R.string.choose_each_time)))
                    setPreferenceCamApp(packageName);
                askShot(packageName);
                            }
                        });
                builder.create().show();
            }
        }
    }

    public void askShot(String packageName)
    {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        File path = Environment.getExternalStoragePublicDirectory(
                mSharedPrefs.getString("pref_capture_directory", Environment.DIRECTORY_PICTURES)
                );
        File photo = new File(path,  "CAP_"+ sdf.format(new Date()) +".jpg");
        Log.i(TAG,"Ask shot:" + photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);

        intent.setPackage(packageName);
        startActivityForResult(intent,SHOT);
    }

    public void noCamAppFound()
    {
        Log.e(TAG,"No application found to take a shot");
        Toast.makeText(this,
                getResources().getString(R.string.noapptoshot), Toast.LENGTH_SHORT).show();
        finish();
    }

    public void setPreferenceCamApp(String camApp)
    {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putString("pref_capture_camapp", camApp);
        editor.commit();
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
