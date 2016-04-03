package com.pluscubed.velociraptor;


import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION = 105;

    public static boolean isAccessibilityServiceEnabled(Context context) {
        int accessibilityEnabled = 0;
        final String service = BuildConfig.APPLICATION_ID + "/" + FloatingService.class.getName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(), android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessibilityService = splitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        } else {
            //Accessibility is disabled
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(android.R.id.content);
        if (f == null) {
            fm.beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            SwitchPreference preference = (SwitchPreference) findPreference("pref_enable_speed_limit");
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean on = (boolean) newValue;
                    Intent intent = new Intent(getActivity(), FloatingService.class);
                    if (on) {
                        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                            return false;
                        } else {
                            getActivity().startService(intent);
                        }

                        /*new MaterialDialog.Builder(getActivity())
                                .content(R.string.dialog_draw_overlay)
                                .positiveText(R.string.open_settings)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @TargetApi(Build.VERSION_CODES.M)
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        try {
                                            //Open the current default browswer App Info page
                                            openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, BuildConfig.APPLICATION_ID);
                                        } catch (ActivityNotFoundException ignored) {
                                            //Crashlytics.logException(ignored);
                                            //Toast.makeText(MainActivity.this, R.string.open_settings_failed_overlay, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                })
                                .show();*/
                    } else {
                        getActivity().stopService(intent);
                    }

                    return true;
                }
            });
            preference.setChecked(isAccessibilityServiceEnabled(getActivity()));
        }

        void openSettings(String settingsAction, String packageName) {
            Intent intent = new Intent(settingsAction);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }
}
