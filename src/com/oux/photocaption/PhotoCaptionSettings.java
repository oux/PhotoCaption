package com.oux.photocaption;
 
import android.os.Bundle;
import android.os.Build;
import android.app.Activity;
import android.preference.PreferenceActivity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
 
public class PhotoCaptionSettings extends PreferenceActivity {

    private static final String BUILD_VERSION = "build_version";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            addPreferencesFromResource(R.xml.settings);
            try {
                final PackageInfo packageInfo =
                    getPackageManager().getPackageInfo(getPackageName(), 0);
                findPreference(BUILD_VERSION).setTitle(getResources().getString(R.string.app_name) + " " +packageInfo.versionName);
            } catch (NameNotFoundException e) {
                findPreference(BUILD_VERSION).setTitle(R.string.app_name);
            }
        } else {
            getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PhotoCaptionSettingsFragment()).commit();
        }
    }
}
