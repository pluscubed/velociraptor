package com.pluscubed.velociraptor.settings;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
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
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.billing.BillingConstants;
import com.pluscubed.velociraptor.billing.BillingManager;
import com.pluscubed.velociraptor.detection.AppDetectionService;
import com.pluscubed.velociraptor.limit.LimitService;
import com.pluscubed.velociraptor.settings.appselection.AppSelectionActivity;
import com.pluscubed.velociraptor.utils.NotificationUtils;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Emitter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class SettingsActivity extends AppCompatActivity {
    public static final int PENDING_SERVICE = 4;
    public static final int PENDING_SERVICE_CLOSE = 3;
    public static final int PENDING_SETTINGS = 2;
    public static final int NOTIFICATION_CONTROLS = 42;

    public static final String OSM_EDITDATA_URL = "http://openstreetmap.org";
    public static final String OSM_COVERAGE_URL = "http://product.itoworld.com/map/124";
    public static final String HERE_EDITDATA_URL = "https://mapcreator.here.com/mapcreator";
    public static final String TOMTOM_EDITDATA_URL = "https://www.tomtom.com/mapshare/tools";

    private static final int REQUEST_LOCATION = 105;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    //Permissions
    @BindView(R.id.card_m_permissions)
    View mPermCard;
    @BindView(R.id.button_floating_enabled)
    Button enableFloatingButton;
    @BindView(R.id.image_floating_enabled)
    ImageView enabledFloatingImage;
    @BindView(R.id.button_location_enabled)
    Button enableLocationButton;
    @BindView(R.id.image_location_enabled)
    ImageView enabledLocationImage;
    @BindView(R.id.button_enable_service)
    Button enableServiceButton;
    @BindView(R.id.image_service_enabled)
    ImageView enabledServiceImage;

    //General
    @BindView(R.id.switch_limits)
    SwitchCompat showSpeedLimitsSwitch;

    @BindView(R.id.switch_speedometer)
    SwitchCompat showSpeedometerSwitch;

    @BindView(R.id.switch_beep)
    SwitchCompat beepSwitch;
    @BindView(R.id.button_test_beep)
    Button testBeepButton;

    @BindView(R.id.linear_tolerance)
    LinearLayout toleranceView;
    @BindView(R.id.text_overview_tolerance)
    TextView toleranceOverview;

    @BindView(R.id.linear_size)
    LinearLayout sizeView;
    @BindView(R.id.text_overview_size)
    TextView sizeOverview;

    @BindView(R.id.linear_opacity)
    LinearLayout opacityView;
    @BindView(R.id.text_overview_opacity)
    TextView opacityOverview;

    @BindView(R.id.spinner_unit)
    Spinner unitSpinner;
    @BindView(R.id.spinner_style)
    Spinner styleSpinner;

    //Providers
    @BindView(R.id.here_title)
    TextView hereTitle;
    @BindView(R.id.here_provider_desc)
    TextView herePriceDesc;
    @BindView(R.id.here_subscribe)
    Button hereSubscribeButton;
    @BindView(R.id.here_editdata)
    Button hereEditDataButton;

    @BindView(R.id.tomtom_title)
    TextView tomtomTitle;
    @BindView(R.id.tomtom_provider_desc)
    TextView tomtomPriceDesc;
    @BindView(R.id.tomtom_subscribe)
    Button tomtomSubscribeButton;
    @BindView(R.id.tomtom_editdata)
    Button tomtomEditDataButton;

    @BindView(R.id.osm_title)
    TextView osmTitle;
    @BindView(R.id.osm_editdata)
    Button osmEditDataButton;
    @BindView(R.id.osm_donate)
    Button osmDonateButton;
    @BindView(R.id.osm_coverage)
    Button osmCoverageButton;

    //Advanced
    @BindView(R.id.switch_debugging)
    SwitchCompat debuggingSwitch;

    @BindView(R.id.linear_app_selection)
    ViewGroup appSelectionContainer;
    @BindView(R.id.button_app_selection)
    Button appSelectionButton;

    @BindView(R.id.linear_gmaps_navigation)
    ViewGroup gmapsOnlyNavigationContainer;
    @BindView(R.id.switch_gmaps_navigation)
    SwitchCompat gmapsOnlyNavigationSwitch;

    @BindView(R.id.switch_notif_controls)
    View notifControlsContainer;


    private NotificationManager notificationManager;
    private BillingManager billingManager;

    @Override
    protected void onDestroy() {
        if (billingManager != null) {
            billingManager.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (billingManager != null && billingManager.getBillingClientResponseCode() == BillingClient.BillingResponse.OK) {
            billingManager.queryPurchases();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPermCard.setVisibility(View.GONE);
        }

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifControlsContainer.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, LimitService.class);
            intent.putExtra(LimitService.EXTRA_NOTIF_START, true);
            PendingIntent pending = PendingIntent.getService(SettingsActivity.this,
                    PENDING_SERVICE, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent intentClose = new Intent(SettingsActivity.this, LimitService.class);
            intentClose.putExtra(LimitService.EXTRA_NOTIF_CLOSE, true);
            PendingIntent pendingClose = PendingIntent.getService(SettingsActivity.this,
                    PENDING_SERVICE_CLOSE, intentClose, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent settings = new Intent(SettingsActivity.this, SettingsActivity.class);
            PendingIntent settingsIntent = PendingIntent.getActivity(SettingsActivity.this,
                    PENDING_SETTINGS, settings, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationUtils.initChannels(this);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(SettingsActivity.this, NotificationUtils.CHANNEL_TOGGLES)
                            .setSmallIcon(R.drawable.ic_speedometer_notif)
                            .setContentTitle(getString(R.string.controls_notif_title))
                            .setContentText(getString(R.string.controls_notif_desc))
                            .addAction(0, getString(R.string.show), pending)
                            .addAction(0, getString(R.string.hide), pendingClose)
                            .setDeleteIntent(pendingClose)
                            .setContentIntent(settingsIntent);
            Notification notification = builder.build();
            notificationManager.notify(NOTIFICATION_CONTROLS, notification);
        });


        appSelectionButton.setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, AppSelectionActivity.class)));

        enableServiceButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (ActivityNotFoundException e) {
                Snackbar.make(enableServiceButton, R.string.open_settings_failed_accessibility, Snackbar.LENGTH_LONG).show();
            }
        });

        enableFloatingButton.setOnClickListener(v -> {
            try {
                //Open the current default browswer App Info page
                openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, BuildConfig.APPLICATION_ID);
            } catch (ActivityNotFoundException ignored) {
                Snackbar.make(enableFloatingButton, R.string.open_settings_failed_overlay, Snackbar.LENGTH_LONG).show();
            }
        });

        enableLocationButton.setOnClickListener(v -> ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION));

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_text, new String[]{"mph", "km/h"});
        unitAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);
        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (PrefUtils.getUseMetric(SettingsActivity.this) != (position == 1)) {
                    PrefUtils.setUseMetric(SettingsActivity.this, position == 1);
                    unitSpinner.setDropDownVerticalOffset(
                            Utils.convertDpToPx(SettingsActivity.this, unitSpinner.getSelectedItemPosition() * -48));

                    Utils.updateFloatingServicePrefs(SettingsActivity.this);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        unitSpinner.setSelection(PrefUtils.getUseMetric(this) ? 1 : 0);
        unitSpinner.setDropDownVerticalOffset(Utils.convertDpToPx(this, unitSpinner.getSelectedItemPosition() * -48));

        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_text,
                new String[]{getString(R.string.united_states), getString(R.string.international)});
        styleAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        styleSpinner.setAdapter(styleAdapter);
        styleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != PrefUtils.getSignStyle(SettingsActivity.this)) {
                    PrefUtils.setSignStyle(SettingsActivity.this, position);
                    styleSpinner.setDropDownVerticalOffset(
                            Utils.convertDpToPx(SettingsActivity.this, styleSpinner.getSelectedItemPosition() * -48));

                    Utils.updateFloatingServicePrefs(SettingsActivity.this);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        styleSpinner.setSelection(PrefUtils.getSignStyle(this));
        styleSpinner.setDropDownVerticalOffset(Utils.convertDpToPx(this, styleSpinner.getSelectedItemPosition() * -48));

        toleranceView.setOnClickListener(v -> new ToleranceDialogFragment().show(getFragmentManager(), "dialog_tolerance"));

        sizeView.setOnClickListener(v -> new SizeDialogFragment().show(getFragmentManager(), "dialog_size"));

        opacityView.setOnClickListener(v -> new OpacityDialogFragment().show(getFragmentManager(), "dialog_opacity"));

        showSpeedometerSwitch.setChecked(PrefUtils.getShowSpeedometer(this));
        ((View) showSpeedometerSwitch.getParent()).setOnClickListener(v -> {
            showSpeedometerSwitch.setChecked(!showSpeedometerSwitch.isChecked());

            PrefUtils.setShowSpeedometer(SettingsActivity.this, showSpeedometerSwitch.isChecked());

            Utils.updateFloatingServicePrefs(SettingsActivity.this);
        });

        showSpeedLimitsSwitch.setChecked(PrefUtils.getShowLimits(this));
        ((View) showSpeedLimitsSwitch.getParent()).setOnClickListener(v -> {
            showSpeedLimitsSwitch.setChecked(!showSpeedLimitsSwitch.isChecked());

            PrefUtils.setShowLimits(SettingsActivity.this, showSpeedLimitsSwitch.isChecked());

            Utils.updateFloatingServicePrefs(SettingsActivity.this);
        });


        debuggingSwitch.setChecked(PrefUtils.isDebuggingEnabled(this));
        ((View) debuggingSwitch.getParent()).setOnClickListener(v -> {
            debuggingSwitch.setChecked(!debuggingSwitch.isChecked());

            PrefUtils.setDebugging(SettingsActivity.this, debuggingSwitch.isChecked());

            Utils.updateFloatingServicePrefs(SettingsActivity.this);
        });

        beepSwitch.setChecked(PrefUtils.isBeepAlertEnabled(this));
        beepSwitch.setOnClickListener(v -> PrefUtils.setBeepAlertEnabled(SettingsActivity.this, beepSwitch.isChecked()));
        testBeepButton.setOnClickListener(v -> Utils.playBeeps());

        gmapsOnlyNavigationSwitch.setChecked(isNotificationAccessGranted() && PrefUtils.isGmapsOnlyInNavigation(this));
        gmapsOnlyNavigationContainer.setOnClickListener(v -> {
            if (!gmapsOnlyNavigationSwitch.isEnabled()) {
                return;
            }

            boolean accessGranted = isNotificationAccessGranted();
            if (accessGranted) {
                gmapsOnlyNavigationSwitch.toggle();
                PrefUtils.setGmapsOnlyInNavigation(SettingsActivity.this, gmapsOnlyNavigationSwitch.isChecked());
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                new MaterialDialog.Builder(SettingsActivity.this)
                        .content(R.string.gmaps_only_nav_notif_access)
                        .positiveText(R.string.grant)
                        .onPositive((dialog, which) -> {
                            try {
                                String settingsAction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ?
                                        Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS : "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
                                Intent intent = new Intent(settingsAction);
                                startActivity(intent);
                            } catch (ActivityNotFoundException ignored) {
                                Snackbar.make(enableFloatingButton, R.string.open_settings_failed_notif_access, Snackbar.LENGTH_LONG).show();
                            }
                        })
                        .show();
            } else {
                Snackbar.make(findViewById(android.R.id.content), R.string.gmaps_only_nav_jellybean, Snackbar.LENGTH_LONG).show();
            }
        });

        hereEditDataButton.setOnClickListener(view -> openLink(HERE_EDITDATA_URL));

        hereSubscribeButton.setOnClickListener(view -> {
            if (!isBillingManagerReady()) {
                Snackbar.make(findViewById(android.R.id.content), R.string.in_app_unavailable, Snackbar.LENGTH_SHORT).show();
            }
            billingManager.initiatePurchaseFlow(BillingConstants.SKU_HERE, BillingClient.SkuType.SUBS);
        });

        tomtomSubscribeButton.setOnClickListener(view -> {
            if (!isBillingManagerReady()) {
                Snackbar.make(findViewById(android.R.id.content), R.string.in_app_unavailable, Snackbar.LENGTH_SHORT).show();
            }
            billingManager.initiatePurchaseFlow(BillingConstants.SKU_TOMTOM, BillingClient.SkuType.SUBS);
        });

        tomtomEditDataButton.setOnClickListener(view -> openLink(TOMTOM_EDITDATA_URL));

        osmCoverageButton.setOnClickListener(v -> {
            if (Utils.isLocationPermissionGranted(SettingsActivity.this)) {
                FusedLocationProviderClient fusedLocationProvider =
                        LocationServices.getFusedLocationProviderClient(SettingsActivity.this);
                fusedLocationProvider
                        .getLastLocation()
                        .addOnCompleteListener(SettingsActivity.this, task -> {
                            String uriString = OSM_COVERAGE_URL;
                            if (task.isSuccessful() && task.getResult() != null) {
                                Location lastLocation = task.getResult();
                                uriString += "?lon=" + lastLocation.getLongitude() + "&lat=" + lastLocation.getLatitude() + "&zoom=12";
                            }
                            openLink(uriString);
                        });
            } else {
                openLink(OSM_COVERAGE_URL);
            }
        });

        osmEditDataButton.setOnClickListener(
                view -> new MaterialDialog.Builder(SettingsActivity.this)
                        .content(R.string.osm_edit)
                        .positiveText(R.string.share_link)
                        .onPositive((dialog, which) -> {
                            Intent shareIntent = new Intent();
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_TEXT, OSM_EDITDATA_URL);
                            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_link)));
                        })
                        .show()
        );

        osmDonateButton.setOnClickListener(view -> showSupportDialog());


        invalidateStates();

        if (BuildConfig.VERSION_CODE > PrefUtils.getVersionCode(this)
                && !PrefUtils.isFirstRun(this)) {
            showChangelog();
        }

        if (PrefUtils.getVersionCode(this) <= 39 || !PrefUtils.isTermsAccepted(this)) {
            showTermsDialog();
        }

        Drawable checkIcon = AppCompatResources.getDrawable(this, R.drawable.ic_done_green_20dp);
        Drawable crossIcon = AppCompatResources.getDrawable(this, R.drawable.ic_cross_red_20dp);
        osmTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, checkIcon, null);
        hereTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, crossIcon, null);
        tomtomTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, crossIcon, null);

        billingManager = new BillingManager(this, new BillingManager.BillingUpdatesListener() {
            @Override
            public void onBillingClientSetupFinished() {
                billingManager.querySkuDetailsAsync(
                        BillingClient.SkuType.SUBS,
                        Arrays.asList(BillingConstants.SKU_HERE, BillingConstants.SKU_TOMTOM),
                        (responseCode, skuDetailsList) -> {
                            if (responseCode != BillingClient.BillingResponse.OK) {
                                return;
                            }
                            for (SkuDetails details : skuDetailsList) {

                                if (details.getSku().equals(BillingConstants.SKU_HERE)) {
                                    herePriceDesc.setText(getString(R.string.here_desc, getString(R.string.per_month, details.getPrice())));
                                }

                                if (details.getSku().equals(BillingConstants.SKU_TOMTOM)) {
                                    tomtomPriceDesc.setText(getString(R.string.tomtom_desc, getString(R.string.per_month, details.getPrice())));
                                }
                            }
                        });
            }

            @Override
            public void onConsumeFinished(String token, int result) {

            }

            @Override
            public void onPurchasesUpdated(List<Purchase> purchases) {
                Set<String> purchased = new HashSet<>();

                for (Purchase purchase : purchases) {
                    purchased.add(purchase.getSku());
                }

                setSubscriptionState(hereSubscribeButton, hereTitle, purchased.contains(BillingConstants.SKU_HERE));
                setSubscriptionState(tomtomSubscribeButton, tomtomTitle, purchased.contains(BillingConstants.SKU_TOMTOM));
            }
        });

        PrefUtils.setFirstRun(this, false);
        PrefUtils.setVersionCode(this, BuildConfig.VERSION_CODE);
    }

    private boolean isBillingManagerReady() {
        return billingManager != null
                && billingManager.getBillingClientResponseCode() == BillingClient.BillingResponse.OK;
    }

    private void setSubscriptionState(Button button, TextView title, boolean subscribed) {
        if (subscribed) {
            button.setEnabled(false);
            button.setText(R.string.subscribed);
            Drawable checkIcon = AppCompatResources.getDrawable(this, R.drawable.ic_done_green_20dp);
            title.setCompoundDrawablesWithIntrinsicBounds(null, null, checkIcon, null);
        } else {
            button.setEnabled(true);
            button.setText(R.string.subscribe);
            Drawable crossIcon = AppCompatResources.getDrawable(this, R.drawable.ic_cross_red_20dp);
            title.setCompoundDrawablesWithIntrinsicBounds(null, null, crossIcon, null);
        }
    }


    private void openLink(String uriString) {
        Intent intent = new Intent();
        intent.setData(Uri.parse(uriString));
        intent.setAction(Intent.ACTION_VIEW);
        try {
            SettingsActivity.this.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(enableFloatingButton, getString(R.string.open_link_failed, uriString), Snackbar.LENGTH_LONG).show();
        }
    }

    private boolean isNotificationAccessGranted() {
        return NotificationManagerCompat.getEnabledListenerPackages(SettingsActivity.this).contains(BuildConfig.APPLICATION_ID);
    }

    public Observable<SkuDetails> querySkuDetails(BillingManager manager, String itemType, String... skuList) {
        return Observable.create((Action1<Emitter<List<SkuDetails>>>) listEmitter -> {
            manager.querySkuDetailsAsync(
                    itemType,
                    Arrays.asList(skuList),
                    (responseCode, skuDetailsList) -> {
                        if (responseCode != BillingClient.BillingResponse.OK) {
                            listEmitter.onError(new Throwable("Billing error: " + responseCode));
                        }
                        listEmitter.onNext(skuDetailsList);
                        listEmitter.onCompleted();
                    });
        }, Emitter.BackpressureMode.BUFFER)
                .flatMap(Observable::from);
    }

    public void showSupportDialog() {
        if (!isBillingManagerReady()) {
            Snackbar.make(findViewById(android.R.id.content), R.string.in_app_unavailable, Snackbar.LENGTH_SHORT).show();
        }

        Observable<SkuDetails> monthlyDonations = querySkuDetails(billingManager, BillingClient.SkuType.SUBS,
                BillingConstants.SKU_D1_MONTHLY, BillingConstants.SKU_D3_MONTHLY);
        Observable<SkuDetails> oneTimeDonations = querySkuDetails(billingManager, BillingClient.SkuType.INAPP,
                BillingConstants.SKU_D1, BillingConstants.SKU_D3, BillingConstants.SKU_D5, BillingConstants.SKU_D10, BillingConstants.SKU_D20);

        monthlyDonations.concatWith(oneTimeDonations)
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(skuDetailsList -> {
                    String content = getString(R.string.support_dev_dialog);

                    MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
                            .icon(AppCompatResources.getDrawable(this, R.drawable.ic_favorite_black_24dp))
                            .title(R.string.support_development)
                            .content(Html.fromHtml(content));

                    List<String> purchaseDisplay = new ArrayList<>();
                    for (SkuDetails details : skuDetailsList) {
                        String amount = details.getPrice();
                        if (details.getType().equals(BillingClient.SkuType.SUBS))
                            amount = getString(R.string.per_month, amount);
                        else {
                            amount = getString(R.string.one_time, amount);
                        }
                        purchaseDisplay.add(amount);
                    }

                    builder.items(purchaseDisplay)
                            .itemsCallback((dialog, itemView, which, text) -> {
                                SkuDetails skuDetails = skuDetailsList.get(which);
                                billingManager.initiatePurchaseFlow(skuDetails.getSku(), skuDetails.getType());
                            });

                    builder.show();
                }, error -> {
                    Snackbar.make(findViewById(android.R.id.content), R.string.in_app_unavailable, Snackbar.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        startLimitService(false);
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

    private void showAboutDialog() {
        new MaterialDialog.Builder(this)
                .title(getString(R.string.about_dialog_title, BuildConfig.VERSION_NAME))
                .positiveText(R.string.dismiss)
                .content(Html.fromHtml(getString(R.string.about_body)))
                .neutralText(R.string.licenses)
                .onNeutral((dialog, which) -> startActivity(new Intent(SettingsActivity.this, OssLicensesMenuActivity.class)))
                .iconRes(R.mipmap.ic_launcher)
                .show();
    }

    private void showTermsDialog() {
        new MaterialDialog.Builder(this)
                .positiveText(R.string.accept)
                .content(Html.fromHtml(getString(R.string.terms_body)))
                .onPositive((dialog, which) -> {
                    PrefUtils.setTermsAccepted(SettingsActivity.this, true);
                })
                .iconRes(R.mipmap.ic_launcher)
                .canceledOnTouchOutside(false)
                .show();
    }

    private void showChangelog() {
        ChangelogDialogFragment.newInstance().show(getFragmentManager(), "CHANGELOG_DIALOG");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            invalidateStates();
        }
    }

    private void invalidateStates() {
        boolean permissionGranted = Utils.isLocationPermissionGranted(this);
        enabledLocationImage.setImageResource(permissionGranted ? R.drawable.ic_done_green_40dp : R.drawable.ic_cross_red_40dp);
        enableLocationButton.setEnabled(!permissionGranted);

        @SuppressLint({"NewApi", "LocalSuppress"}) boolean overlayEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        enabledFloatingImage.setImageResource(overlayEnabled ? R.drawable.ic_done_green_40dp : R.drawable.ic_cross_red_40dp);
        enableFloatingButton.setEnabled(!overlayEnabled);

        boolean serviceEnabled = Utils.isAccessibilityServiceEnabled(this, AppDetectionService.class);
        enabledServiceImage.setImageResource(serviceEnabled ? R.drawable.ic_done_green_40dp : R.drawable.ic_cross_red_40dp);
        enableServiceButton.setEnabled(!serviceEnabled);

        String constant = getString(PrefUtils.getUseMetric(this) ? R.string.kmph : R.string.mph,
                String.valueOf(PrefUtils.getSpeedingConstant(this)));
        String percent = getString(R.string.percent, String.valueOf(PrefUtils.getSpeedingPercent(this)));
        String mode = PrefUtils.getToleranceMode(this) ? "+" : getString(R.string.or);
        String overview = getString(R.string.tolerance_desc, percent, mode, constant);
        toleranceOverview.setText(overview);

        String limitSizePercent = getString(R.string.percent, String.valueOf((int) (PrefUtils.getSpeedLimitSize(this) * 100)));
        String speedLimitSize = getString(R.string.size_limit_overview, limitSizePercent);
        String speedometerSizePercent = getString(R.string.percent, String.valueOf((int) (PrefUtils.getSpeedometerSize(this) * 100)));
        String speedometerSize = getString(R.string.size_speedometer_overview, speedometerSizePercent);
        sizeOverview.setText(speedLimitSize + "\n" + speedometerSize);

        opacityOverview.setText(getString(R.string.percent, String.valueOf(PrefUtils.getOpacity(this))));

        if (permissionGranted && overlayEnabled) {
            startLimitService(true);
        }
    }

    private void startLimitService(boolean start) {
        Intent intent = new Intent(this, LimitService.class);
        if (!start) {
            intent.putExtra(LimitService.EXTRA_CLOSE, true);
        }
        startService(intent);
    }

    private void openSettings(String settingsAction, String packageName) {
        Intent intent = new Intent(settingsAction);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
    }
}
