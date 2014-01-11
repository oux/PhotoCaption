package com.oux.photocaption;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

import android.util.Log;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.app.Activity;
import android.preference.PreferenceFragment;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;

import com.android.gallery3d.exif.ExifInterface;

public class PhotoCaptionSettings extends PreferenceActivity {

    static final String TAG = "PhotoCaptionSettings";
    private static final String BUILD_VERSION = "build_version";
    private static final String CAMAPP = "pref_capture_camapp";
    private static final String DIRECTORY = "pref_capture_directory";
    private static final String EXIFGAL = "pref_gallery_exif_field";
    private static final String EXIFVIEW = "pref_view_exif_field";
    private static final String EXIFEDIT = "pref_edit_exif_field";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            addPreferencesFromResource(R.xml.settings);
            setPackageVersion();
            setCamAppList();
            setExifLists();
            setDirectoryList();
        } else {
            getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PhotoCaptionSettingsFragment()).commit();
        }
    }

    public void setPackageVersion()
    {
        try {
            final PackageInfo packageInfo =
                getPackageManager().getPackageInfo(getPackageName(), 0);
            findPreference(BUILD_VERSION).setTitle(getResources().getString(R.string.app_name) + " " +packageInfo.versionName);
        } catch (NameNotFoundException e) {
            findPreference(BUILD_VERSION).setTitle(R.string.app_name);
        }
    }

    public void setExifLists()
    {
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        values.add(Integer.toString(ExifInterface.TAG_DATE_TIME_ORIGINAL));
        entries.add("DateTimeOriginal");
        values.add(Integer.toString(ExifInterface.TAG_DATE_TIME_DIGITIZED));
        entries.add("DateTimeDigitized");
        values.add(Integer.toString(ExifInterface.TAG_USER_COMMENT));
        entries.add("UserComment");
        values.add(Integer.toString(ExifInterface.TAG_IMAGE_DESCRIPTION));
        entries.add("ImageDescription");

        ((MultiSelectListPreference)
         findPreference(EXIFVIEW)).setEntries(entries.toArray(new
             CharSequence[entries.size()]));
        ((MultiSelectListPreference)
         findPreference(EXIFVIEW)).setEntryValues(values.toArray(new
             CharSequence[values.size()]));
        ((MultiSelectListPreference)
         findPreference(EXIFGAL)).setEntries(entries.toArray(new
             CharSequence[entries.size()]));
        ((MultiSelectListPreference)
         findPreference(EXIFGAL)).setEntryValues(values.toArray(new
             CharSequence[values.size()]));
        ((ListPreference)findPreference(EXIFEDIT)).setEntries(entries.toArray(new
            CharSequence[entries.size()]));
        ((ListPreference)findPreference(EXIFEDIT)).setEntryValues(values.toArray(new
            CharSequence[values.size()]));
    }

    public void setCamAppList()
    {
        Context context = getApplicationContext();
        final PackageManager pm = context.getPackageManager();

        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");

        List<ResolveInfo> appInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        values.add(getResources().getString(R.string.choose_each_time));
        for (ResolveInfo ri : appInfoList) {
            String packageName = ri.activityInfo.packageName;
            Log.i(TAG,"packageName:" + packageName);
            if (!packageName.equals(context.getPackageName())) {
                values.add(packageName);
            }
        }

        if (values.size() <= 2) {
            findPreference(CAMAPP).setEnabled(false);
            Log.e(TAG,"No more than one application found to take a shot");
        }
        else
        {
            ((ListPreference)findPreference(CAMAPP)).setEntries(values.toArray(new
                CharSequence[values.size()]));
            ((ListPreference)findPreference(CAMAPP)).setEntryValues(values.toArray(new
                CharSequence[values.size()]));
        }
    }

    public void setDirectoryList()
    {
        ArrayList<String> roots = new ArrayList<String>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        roots.add(Environment.DIRECTORY_PICTURES);
        roots.add(Environment.DIRECTORY_DCIM);
        for (String d:roots)
        {
            if (Environment.getExternalStoragePublicDirectory(d).isDirectory()) {
                values.add(d);
                Log.i(TAG,"Scanning:" + d );
                for (File f: Environment.getExternalStoragePublicDirectory(d).listFiles())
                    if (f.isDirectory())
                        values.add(d + "/" + f.getName());
            }
        }

        ((ListPreference)findPreference(DIRECTORY)).setEntries(values.toArray(new
            CharSequence[values.size()]));
        ((ListPreference)findPreference(DIRECTORY)).setEntryValues(values.toArray(new
            CharSequence[values.size()]));
    }
}
