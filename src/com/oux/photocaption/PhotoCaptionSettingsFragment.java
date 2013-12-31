package com.oux.photocaption;

import java.util.List;
import java.util.ArrayList;
 
import android.util.Log;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceFragment;
import android.preference.ListPreference;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;
 
public class PhotoCaptionSettingsFragment extends PreferenceFragment {

    static final String TAG = "PhotoCaptionSettingsFragment";
    private static final String BUILD_VERSION = "build_version";
    private static final String CAMAPP = "pref_edit_camapp";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        setPackageVersion();
        setCamAppList();
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

    public void setCamAppList()
    {
        Context context = getActivity().getApplicationContext();
        final PackageManager pm = context.getPackageManager();

        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");

        List<ResolveInfo> appInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        // ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        values.add(getResources().getString(R.string.choose_each_time));
        for (ResolveInfo ri : appInfoList) {
            String packageName = ri.activityInfo.packageName;
            Log.i(TAG,"packageName:" + packageName);
            if (!packageName.equals(context.getPackageName())) {
                values.add(packageName);
                // entries.add(packageName + " " +ri.activityInfo.name);
            }
        }

        if (values.size() <= 1) {
            // Add a none entry or hide preference
            findPreference(CAMAPP).setEnabled(false);
            Log.e(TAG,"No application found to take a shot");
            // Toast.makeText(context,
            //        getResources().getString(R.string.noapptoshot), Toast.LENGTH_SHORT).show();
        }
        else
        {
            ((ListPreference)findPreference(CAMAPP)).setEntries(values.toArray(new CharSequence[values.size()]));
            ((ListPreference)findPreference(CAMAPP)).setEntryValues(values.toArray(new CharSequence[values.size()]));
        }
    }
}