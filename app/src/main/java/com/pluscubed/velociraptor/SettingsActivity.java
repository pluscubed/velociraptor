package com.pluscubed.velociraptor;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import com.afollestad.materialdialogs.MaterialDialog;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION = 105;

    private Button mEnableServiceButton;
    private ImageView mEnabledServiceImage;
    private Button mEnableFloatingButton;
    private ImageView mEnabledFloatingImage;
    private Button mEnableLocationButton;
    private ImageView mEnabledLocationImage;

    private Spinner mUnitSpinner;
    private Spinner mStyleSpinner;
    private Spinner mOverspeedSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mEnableServiceButton = (Button) findViewById(R.id.button_enable_service);
        mEnabledServiceImage = (ImageView) findViewById(R.id.image_service_enabled);
        mEnableFloatingButton = (Button) findViewById(R.id.button_floating_enabled);
        mEnabledFloatingImage = (ImageView) findViewById(R.id.image_floating_enabled);
        mEnableLocationButton = (Button) findViewById(R.id.button_location_enabled);
        mEnabledLocationImage = (ImageView) findViewById(R.id.image_location_enabled);

        mUnitSpinner = (Spinner) findViewById(R.id.spinner_unit);
        mStyleSpinner = (Spinner) findViewById(R.id.spinner_style);
        mOverspeedSpinner = (Spinner) findViewById(R.id.spinner_overspeed);
        final SwitchCompat showSpeedometer = (SwitchCompat) findViewById(R.id.switch_speedometer);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            ((View) mEnabledFloatingImage.getParent()).setVisibility(View.GONE);
            mEnableFloatingButton.setVisibility(View.GONE);
            findViewById(R.id.floating_enabled_space).setVisibility(View.GONE);

            ((View) mEnabledLocationImage.getParent()).setVisibility(View.GONE);
            mEnableLocationButton.setVisibility(View.GONE);
            findViewById(R.id.location_enabled_space).setVisibility(View.GONE);
        }

        mEnableServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } catch (ActivityNotFoundException e) {
                    Snackbar.make(mEnableServiceButton, R.string.open_settings_failed_accessibility, Snackbar.LENGTH_LONG).show();
                }
            }
        });

        mEnableFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //Open the current default browswer App Info page
                    openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, BuildConfig.APPLICATION_ID);
                } catch (ActivityNotFoundException ignored) {
                    Snackbar.make(mEnableFloatingButton, R.string.open_settings_failed_overlay, Snackbar.LENGTH_LONG).show();
                }
            }
        });

        mEnableLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            }
        });

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item,
                new String[]{"mph", "km/h"});
        mUnitSpinner.setAdapter(unitAdapter);
        mUnitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PrefUtils.setUseMetric(SettingsActivity.this, position == 1);
                mUnitSpinner.setDropDownVerticalOffset(
                        Utils.convertDpToPx(SettingsActivity.this, mUnitSpinner.getSelectedItemPosition() * -48));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mUnitSpinner.setSelection(PrefUtils.getUseMetric(this) ? 1 : 0);
        mUnitSpinner.setDropDownVerticalOffset(Utils.convertDpToPx(this, mUnitSpinner.getSelectedItemPosition() * -48));

        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item,
                new String[]{getString(R.string.united_states), getString(R.string.international)});
        mStyleSpinner.setAdapter(styleAdapter);
        mStyleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != PrefUtils.getSignStyle(SettingsActivity.this)) {
                    PrefUtils.setSignStyle(SettingsActivity.this, position);
                    mStyleSpinner.setDropDownVerticalOffset(
                            Utils.convertDpToPx(SettingsActivity.this, mStyleSpinner.getSelectedItemPosition() * -48));

                    if (isServiceReady()) {
                        enableService(false);
                        enableService(true);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mStyleSpinner.setSelection(PrefUtils.getSignStyle(this));
        mStyleSpinner.setDropDownVerticalOffset(Utils.convertDpToPx(this, mStyleSpinner.getSelectedItemPosition() * -48));

        ArrayAdapter<String> overspeedAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item,
                new String[]{"0%", "5%", "10%", "15%", "20%"});
        mOverspeedSpinner.setAdapter(overspeedAdapter);
        mOverspeedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PrefUtils.setOverspeedPercent(SettingsActivity.this, position * 5);
                mOverspeedSpinner.setDropDownVerticalOffset(
                        Utils.convertDpToPx(SettingsActivity.this, mOverspeedSpinner.getSelectedItemPosition() * -48));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mOverspeedSpinner.setSelection(PrefUtils.getOverspeedPercent(this) / 5);
        mOverspeedSpinner.setDropDownVerticalOffset(Utils.convertDpToPx(this, mOverspeedSpinner.getSelectedItemPosition() * -48));

        showSpeedometer.setChecked(PrefUtils.getShowSpeedometer(this));
        ((View) showSpeedometer.getParent()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpeedometer.setChecked(!showSpeedometer.isChecked());

                PrefUtils.setShowSpeedometer(SettingsActivity.this, showSpeedometer.isChecked());
            }
        });

        invalidateStates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        enableService(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings_about:
                showAboutDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showAboutDialog() {
        new MaterialDialog.Builder(this)
                .title(getString(R.string.about_dialog_title, BuildConfig.VERSION_NAME))
                .positiveText(R.string.dismiss)
                .content(Html.fromHtml(getString(R.string.about_body)))
                .iconRes(R.mipmap.ic_launcher)
                .show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            invalidateStates();
        }
    }

    private void invalidateStates() {
        boolean permissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
        mEnabledLocationImage.setImageResource(permissionGranted ? R.drawable.ic_done_green_40dp : R.drawable.ic_cross_red_40dp);
        mEnableLocationButton.setEnabled(!permissionGranted);

        boolean overlayEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        mEnabledFloatingImage.setImageResource(overlayEnabled ? R.drawable.ic_done_green_40dp : R.drawable.ic_cross_red_40dp);
        mEnableFloatingButton.setEnabled(!overlayEnabled);

        boolean serviceEnabled = Utils.isAccessibilityServiceEnabled(this, AppDetectionService.class);
        mEnabledServiceImage.setImageResource(serviceEnabled ? R.drawable.ic_done_green_40dp : R.drawable.ic_cross_red_40dp);
        mEnableServiceButton.setEnabled(!serviceEnabled);

        if (permissionGranted && overlayEnabled) {
            enableService(true);
        }
    }

    private void enableService(boolean start) {
        Intent intent = new Intent(this, FloatingService.class);
        if (start) {
            startService(intent);
        } else {
            stopService(intent);
        }
    }

    private boolean isServiceReady() {
        boolean permissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
        boolean overlayEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        return permissionGranted && overlayEnabled;
    }

    void openSettings(String settingsAction, String packageName) {
        Intent intent = new Intent(settingsAction);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
    }
}
