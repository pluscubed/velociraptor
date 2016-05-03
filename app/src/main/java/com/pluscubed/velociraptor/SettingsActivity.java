package com.pluscubed.velociraptor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.afollestad.materialdialogs.MaterialDialog;
import com.pluscubed.velociraptor.appselection.AppSelectionActivity;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends AppCompatActivity {
    public static final int PENDING_SERVICE = 4;
    public static final int PENDING_SERVICE_CLOSE = 3;
    public static final int PENDING_SETTINGS = 2;
    public static final int NOTIFICATION_CONTROLS = 42;
    private static final int REQUEST_LOCATION = 105;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.button_enable_service)
    Button mEnableServiceButton;
    @BindView(R.id.image_service_enabled)
    ImageView mEnabledServiceImage;
    @BindView(R.id.button_floating_enabled)
    Button mEnableFloatingButton;
    @BindView(R.id.image_floating_enabled)
    ImageView mEnabledFloatingImage;
    @BindView(R.id.button_location_enabled)
    Button mEnableLocationButton;
    @BindView(R.id.image_location_enabled)
    ImageView mEnabledLocationImage;

    @BindView(R.id.open_openstreetmap)
    LinearLayout openStreetMapView;
    @BindView(R.id.check_coverage)
    LinearLayout checkCoverageView;

    @BindView(R.id.spinner_unit)
    Spinner mUnitSpinner;
    @BindView(R.id.spinner_style)
    Spinner mStyleSpinner;
    @BindView(R.id.spinner_overspeed)
    Spinner mOverspeedSpinner;
    @BindView(R.id.switch_speedometer)
    SwitchCompat mShowSpeedometerSwitch;
    @BindView(R.id.switch_auto_display)
    SwitchCompat mAutoDisplaySwitch;
    @BindView(R.id.switch_debugging)
    SwitchCompat mDebuggingSwitch;
    @BindView(R.id.linear_auto_display_options)
    ViewGroup mAutoDisplayOptionsContainer;
    @BindView(R.id.linear_app_selection)
    ViewGroup mOpenAppSelectionContainer;
    @BindView(R.id.switch_beep)
    SwitchCompat beepSwitch;
    @BindView(R.id.button_test_beep)
    Button testBeepButton;

    private NotificationManager mNotificationManager;


    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            View marshmallowPermissionsCard = findViewById(R.id.card_m_permissions);
            marshmallowPermissionsCard.setVisibility(View.GONE);
        }

        openStreetMapView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareCompat.IntentBuilder.from(SettingsActivity.this)
                        .setText("https://www.openstreetmap.org")
                        .setType("text/plain")
                        .startChooser();
            }
        });

        checkCoverageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setData(Uri.parse("http://product.itoworld.com/map/124"));
                intent.setAction(Intent.ACTION_VIEW);
                startActivity(intent);
            }
        });

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        View notifControls = findViewById(R.id.switch_notif_controls);
        notifControls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, FloatingService.class);
                intent.putExtra(FloatingService.EXTRA_NOTIF_START, true);
                PendingIntent pending = PendingIntent.getService(SettingsActivity.this,
                        PENDING_SERVICE, intent, PendingIntent.FLAG_CANCEL_CURRENT);

                Intent intentClose = new Intent(SettingsActivity.this, FloatingService.class);
                intentClose.putExtra(FloatingService.EXTRA_NOTIF_CLOSE, true);
                PendingIntent pendingClose = PendingIntent.getService(SettingsActivity.this,
                        PENDING_SERVICE_CLOSE, intentClose, PendingIntent.FLAG_CANCEL_CURRENT);

                Intent settings = new Intent(SettingsActivity.this, SettingsActivity.class);
                PendingIntent settingsIntent = PendingIntent.getActivity(SettingsActivity.this,
                        PENDING_SETTINGS, settings, PendingIntent.FLAG_CANCEL_CURRENT);

                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(SettingsActivity.this)
                                .setSmallIcon(R.drawable.ic_speedometer)
                                .setContentTitle(getString(R.string.controls_notif_title))
                                .setContentText(getString(R.string.controls_notif_desc))
                                .addAction(0, getString(R.string.show), pending)
                                .addAction(0, getString(R.string.hide), pendingClose)
                                .setDeleteIntent(pendingClose)
                                .setContentIntent(settingsIntent);
                Notification notification = builder.build();
                mNotificationManager.notify(NOTIFICATION_CONTROLS, notification);
            }
        });


        Button openAppSelection = (Button) findViewById(R.id.button_app_selection);
        openAppSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, AppSelectionActivity.class));
            }
        });

        mAutoDisplaySwitch.setChecked(PrefUtils.isAutoDisplayEnabled(this));
        mAutoDisplaySwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean autoDisplayEnabled = mAutoDisplaySwitch.isChecked();
                PrefUtils.setAutoDisplay(SettingsActivity.this, autoDisplayEnabled);
                updateAutoDisplaySwitchEnabled(autoDisplayEnabled);
            }
        });

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
                if (PrefUtils.getUseMetric(SettingsActivity.this) != (position == 1)) {
                    PrefUtils.setUseMetric(SettingsActivity.this, position == 1);
                    mUnitSpinner.setDropDownVerticalOffset(
                            Utils.convertDpToPx(SettingsActivity.this, mUnitSpinner.getSelectedItemPosition() * -48));

                    updateFloatingServicePrefs();
                }
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

                    updateFloatingServicePrefs();
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

        mShowSpeedometerSwitch.setChecked(PrefUtils.getShowSpeedometer(this));
        ((View) mShowSpeedometerSwitch.getParent()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mShowSpeedometerSwitch.setChecked(!mShowSpeedometerSwitch.isChecked());

                PrefUtils.setShowSpeedometer(SettingsActivity.this, mShowSpeedometerSwitch.isChecked());

                updateFloatingServicePrefs();
            }
        });

        mDebuggingSwitch.setChecked(PrefUtils.isDebuggingEnabled(this));
        ((View) mDebuggingSwitch.getParent()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDebuggingSwitch.setChecked(!mDebuggingSwitch.isChecked());

                PrefUtils.setDebugging(SettingsActivity.this, mDebuggingSwitch.isChecked());

                updateFloatingServicePrefs();
            }
        });

        beepSwitch.setChecked(PrefUtils.isBeepAlertEnabled(this));
        beepSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefUtils.setBeepAlertEnabled(SettingsActivity.this, beepSwitch.isChecked());
            }
        });
        testBeepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.playBeep();
            }
        });


        invalidateStates();

        if (BuildConfig.VERSION_CODE > PrefUtils.getVersionCode(this)) {
            showChangelog();
        }
    }

    private void updateFloatingServicePrefs() {
        if (isServiceReady()) {
            Intent intent = new Intent(SettingsActivity.this, FloatingService.class);
            intent.putExtra(FloatingService.EXTRA_PREF_CHANGE, true);
            startService(intent);
        }
    }

    private void updateAutoDisplaySwitchEnabled(boolean enabled) {
        enableDisableAllChildren(enabled, mAutoDisplayOptionsContainer);
        updateOpenAppSelectionEnabled(Utils.isAccessibilityServiceEnabled(this, AppDetectionService.class), enabled);
        mEnabledServiceImage.setAlpha(enabled ? 1f : 0.38f);
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
            case R.id.menu_settings_changelog:
                showChangelog();
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

    private void showChangelog() {
        ChangelogDialog.newInstance().show(getFragmentManager(), "CHANGELOG_DIALOG");
        PrefUtils.setVersionCode(this, BuildConfig.VERSION_CODE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            invalidateStates();
        }
    }

    private void updateOpenAppSelectionEnabled(boolean accessibilityServiceEnabled, boolean autoDisplayEnabled) {
        enableDisableAllChildren(accessibilityServiceEnabled && autoDisplayEnabled, mOpenAppSelectionContainer);
    }

    private void invalidateStates() {
        boolean permissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
        mEnabledLocationImage.setImageResource(permissionGranted ? R.drawable.ic_done_green_40dp : R.drawable.ic_cross_red_40dp);
        mEnableLocationButton.setEnabled(!permissionGranted);

        @SuppressLint({"NewApi", "LocalSuppress"}) boolean overlayEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        mEnabledFloatingImage.setImageResource(overlayEnabled ? R.drawable.ic_done_green_40dp : R.drawable.ic_cross_red_40dp);
        mEnableFloatingButton.setEnabled(!overlayEnabled);

        boolean serviceEnabled = Utils.isAccessibilityServiceEnabled(this, AppDetectionService.class);
        mEnabledServiceImage.setVisibility(serviceEnabled ? View.VISIBLE : View.GONE);
        mEnableServiceButton.setVisibility(serviceEnabled ? View.GONE : View.VISIBLE);

        updateAutoDisplaySwitchEnabled(PrefUtils.isAutoDisplayEnabled(this));

        if (permissionGranted && overlayEnabled) {
            enableService(true);
        }
    }

    private void enableDisableAllChildren(boolean enable, ViewGroup viewgroup) {
        for (int i = 0; i < viewgroup.getChildCount(); i++) {
            View child = viewgroup.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup) {
                enableDisableAllChildren(enable, (ViewGroup) child);
            }
        }
    }

    private void enableService(boolean start) {
        Intent intent = new Intent(this, FloatingService.class);
        if (!start) {
            intent.putExtra(FloatingService.EXTRA_CLOSE, true);
        }
        startService(intent);
    }

    private boolean isServiceReady() {
        boolean permissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
        @SuppressLint({"NewApi", "LocalSuppress"}) boolean overlayEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        return permissionGranted && overlayEnabled;
    }

    void openSettings(String settingsAction, String packageName) {
        Intent intent = new Intent(settingsAction);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
    }
}
