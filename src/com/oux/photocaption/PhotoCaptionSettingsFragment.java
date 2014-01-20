package com.oux.photocaption;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

import android.util.Log;
import android.os.Bundle;
import android.os.Environment;
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

public class PhotoCaptionSettingsFragment extends PreferenceFragment {

    static final String TAG = "PhotoCaptionSettingsFragment";
    private static final String BUILD_VERSION = "build_version";
    private static final String CAMAPP = "pref_capture_camapp";
    private static final String DIRECTORY = "pref_capture_directory";
    private static final String EXIFGAL = "pref_gallery_exif_field";
    private static final String EXIFVIEW = "pref_view_exif_field";
    private static final String EXIFEDIT = "pref_edit_exif_field";
    PhotoCaptionListPreference mPrefExifEdit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        Intent intent = getActivity().getIntent();
        String wantedPref = intent.getStringExtra("preference");

        mPrefExifEdit = ((PhotoCaptionListPreference)findPreference(EXIFEDIT));

        setPackageVersion();
        setCamAppList();
        setExifLists();
        setDirectoryList();

        if (wantedPref != null)
        {
            startPreference(wantedPref);
        }
    }

    public void startPreference(String dialogName){
        if (EXIFEDIT.equals(dialogName))
        {
            if (mPrefExifEdit != null)
            {
                mPrefExifEdit.show();
            }
        }
    }

    public void setPackageVersion()
    {
        try {
            final PackageInfo packageInfo =
                getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
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

        MultiSelectListPreference prefExifGal  = ((MultiSelectListPreference) findPreference(EXIFGAL));
        MultiSelectListPreference prefExifView = ((MultiSelectListPreference) findPreference(EXIFVIEW));

        prefExifGal.setEntries(entries.toArray(new CharSequence[entries.size()]));
        prefExifGal.setEntryValues(values.toArray(new CharSequence[values.size()]));

        prefExifView.setEntries(entries.toArray(new CharSequence[entries.size()]));
        prefExifView.setEntryValues(values.toArray(new CharSequence[values.size()]));

        mPrefExifEdit.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mPrefExifEdit.setEntryValues(values.toArray(new CharSequence[values.size()]));

        Set<String> defPrefExifGal = prefExifGal.getValues();
        if (defPrefExifGal.size() == 0)
            prefExifGal.setValues(
                    new HashSet<String>(Arrays.asList(Integer.toString(ExifInterface.TAG_USER_COMMENT))));

        Set<String> defPrefExifView = prefExifView.getValues();
        if (defPrefExifView.size() == 0)
            prefExifView.setValues(
                    new HashSet<String>(Arrays.asList(Integer.toString(ExifInterface.TAG_USER_COMMENT))));

        String defPrefExifEdit = mPrefExifEdit.getValue();
        if (defPrefExifEdit == null)
            mPrefExifEdit.setValue(Integer.toString(ExifInterface.TAG_USER_COMMENT));
    }

    public void setCamAppList()
    {
        Context context = getActivity().getApplicationContext();
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

        final ListPreference prefCamApp = ((ListPreference)findPreference(CAMAPP));
        if (values.size() <= 2) {
            prefCamApp.setEnabled(false);
            Log.e(TAG,"No more than one application found to take a shot");
        }
        else
        {
            prefCamApp.setEntries(values.toArray(new CharSequence[values.size()]));
            prefCamApp.setEntryValues(values.toArray(new CharSequence[values.size()]));
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

        final ListPreference listPrefDir = ((ListPreference)findPreference(DIRECTORY));
        listPrefDir.setEntries(values.toArray(new CharSequence[values.size()]));
        listPrefDir.setEntryValues(values.toArray(new CharSequence[values.size()]));
        final String prefDir = listPrefDir.getValue();
        if (prefDir == null)
            listPrefDir.setValue(Environment.DIRECTORY_PICTURES);
    }
}
