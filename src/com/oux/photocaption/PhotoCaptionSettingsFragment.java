package com.oux.photocaption;
 
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceFragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
 
public class PhotoCaptionSettingsFragment extends PreferenceFragment {

    private static final String BUILD_VERSION = "build_version";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
        try {
            final PackageInfo packageInfo =
                getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            findPreference(BUILD_VERSION).setTitle(getResources().getString(R.string.app_name) + " " +packageInfo.versionName);
        } catch (NameNotFoundException e) {
            findPreference(BUILD_VERSION).setTitle(R.string.app_name);
        }
    }
}
