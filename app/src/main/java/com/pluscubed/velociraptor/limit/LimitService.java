package com.pluscubed.velociraptor.limit;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.android.billingclient.api.Purchase;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.api.LimitFetcher;
import com.pluscubed.velociraptor.api.LimitResponse;
import com.pluscubed.velociraptor.api.raptor.RaptorLimitProvider;
import com.pluscubed.velociraptor.billing.BillingManager;
import com.pluscubed.velociraptor.settings.SettingsActivity;
import com.pluscubed.velociraptor.utils.NotificationUtils;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import org.json.JSONException;

import java.util.List;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class LimitService extends Service {
    public static final int PENDING_SETTINGS = 5;

    public static final String EXTRA_NOTIF_START = "com.pluscubed.velociraptor.EXTRA_NOTIF_START";
    public static final String EXTRA_NOTIF_CLOSE = "com.pluscubed.velociraptor.EXTRA_NOTIF_CLOSE";
    public static final String EXTRA_CLOSE = "com.pluscubed.velociraptor.EXTRA_CLOSE";
    public static final String EXTRA_PREF_CHANGE = "com.pluscubed.velociraptor.EXTRA_PREF_CHANGE";

    public static final String EXTRA_VIEW = "com.pluscubed.velociraptor.EXTRA_VIEW";
    public static final int VIEW_FLOATING = 0;

    public static final String EXTRA_HIDE_LIMIT = "com.pluscubed.velociraptor.HIDE_LIMIT";
    public static final int NOTIFICATION_WARNING = 192;
    private static final int NOTIFICATION_FOREGROUND = 303;
    private int speedLimitViewType = -1;
    private LimitView speedLimitView;

    private String debuggingRequestInfo;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Subscription speedLimitQuerySubscription;

    private int currentSpeedLimit = -1;
    private Location lastLocationWithSpeed;
    private Location lastLocationWithFetchAttempt;

    private long speedingStartTimestamp = -1;
    private LimitFetcher limitFetcher;

    private boolean isRunning;
    private boolean isStartedFromNotification;
    private boolean isLimitHidden;

    private BillingManager billingManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint({"InflateParams"})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (!isStartedFromNotification && intent.getBooleanExtra(EXTRA_CLOSE, false) ||
                    intent.getBooleanExtra(EXTRA_NOTIF_CLOSE, false)) {
                onStop();
                stopSelf();
                return super.onStartCommand(intent, flags, startId);
            }

            int viewType = intent.getIntExtra(EXTRA_VIEW, VIEW_FLOATING);
            if (viewType != speedLimitViewType) {
                speedLimitViewType = viewType;

                switch (speedLimitViewType) {
                    case VIEW_FLOATING:
                        speedLimitView = new FloatingView(this);
                        break;
                }
            }

            if (intent.getExtras() != null && intent.getExtras().containsKey(EXTRA_HIDE_LIMIT)) {
                isLimitHidden = intent.getBooleanExtra(EXTRA_HIDE_LIMIT, false);
                speedLimitView.hideLimit(isLimitHidden);
                if (isLimitHidden) {
                    currentSpeedLimit = -1;
                }
            }

            if (intent.getBooleanExtra(EXTRA_NOTIF_START, false)) {
                isStartedFromNotification = true;
            } else if (intent.getBooleanExtra(EXTRA_PREF_CHANGE, false)) {
                speedLimitView.updatePrefs();

                updateLimitView(false);
                updateSpeedometer(lastLocationWithSpeed);
            }
        }


        if (isRunning || !prequisitesMet() || speedLimitView == null)
            return super.onStartCommand(intent, flags, startId);

        startNotification();

        debuggingRequestInfo = "";

        limitFetcher = new LimitFetcher(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(0);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onLocationChanged(locationResult.getLastLocation());
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
        }

        billingManager = new BillingManager(this, new BillingManager.BillingUpdatesListener() {
            @Override
            public void onBillingClientSetupFinished() {
                if (RaptorLimitProvider.USE_DEBUG_ID) {
                    try {
                        limitFetcher.verifyRaptorService(new Purchase(
                                "{\"productId\": \"debug\", \"purchaseToken\": \"debug\"}",
                                ""
                        ));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onConsumeFinished(String token, int result) {

            }

            @Override
            public void onPurchasesUpdated(List<Purchase> purchases) {
                for (Purchase purchase : purchases) {
                    limitFetcher.verifyRaptorService(purchase);
                }
            }
        });

        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.fetch(3600).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                remoteConfig.activateFetched();
            }
        });

        isRunning = true;

        return super.onStartCommand(intent, flags, startId);
    }

    private void startNotification() {
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_SETTINGS, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationUtils.initChannels(this);
        Notification notification = new NotificationCompat.Builder(this, NotificationUtils.CHANNEL_RUNNING)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_content))
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_speedometer_notif)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_FOREGROUND, notification);
    }

    private boolean prequisitesMet() {
        if (!PrefUtils.isTermsAccepted(this)) {
            if (BuildConfig.VERSION_CODE > PrefUtils.getVersionCode(this)) {
                showWarningNotification(getString(R.string.terms_warning));
            }
            stopSelf();
            return false;
        }

        if (!Utils.isLocationPermissionGranted(LimitService.this)
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))) {
            showWarningNotification(getString(R.string.permissions_warning));
            stopSelf();
            return false;
        }

        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showWarningNotification(getString(R.string.location_settings_warning));
        }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            showWarningNotification(getString(R.string.network_warning));
        }
        return true;
    }

    private synchronized void onLocationChanged(final Location location) {
        updateSpeedometer(location);
        updateDebuggingText(location, null, null);

        if (speedLimitQuerySubscription == null &&
                !isLimitHidden &&
                PrefUtils.getShowLimits(this) &&
                (lastLocationWithFetchAttempt == null || location.distanceTo(lastLocationWithFetchAttempt) > 10)) {

            speedLimitQuerySubscription = limitFetcher.getSpeedLimit(location)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<LimitResponse>() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onNext(LimitResponse limitResponse) {
                            currentSpeedLimit = limitResponse.speedLimit();

                            updateLimitView(true);
                            updateDebuggingText(location, limitResponse, null);
                        }

                        @Override
                        public void onError(Throwable error) {
                            Timber.d(error);

                            updateLimitView(false);
                            updateDebuggingText(location, null, error);
                        }

                        @Override
                        public void onCompleted() {
                            lastLocationWithFetchAttempt = location;
                            speedLimitQuerySubscription = null;
                        }
                    });
        }
    }

    private void updateDebuggingText(Location location, LimitResponse limitResponse, Throwable error) {
        String text = "Location: " + location + "\n";

        if (lastLocationWithFetchAttempt != null) {
            text += "Time since: " + (System.currentTimeMillis() - lastLocationWithFetchAttempt.getTime()) + "\n";
        }

        if (error == null && limitResponse != null) {
            String origin = getLimitProviderString(limitResponse.origin());

            debuggingRequestInfo = "Origin: " + origin +
                    "\nRoad name: " + limitResponse.roadName() +
                    "\nFrom cache: " + limitResponse.fromCache() +
                    "\nCoords: " + limitResponse.coords();
        } else if (error != null) {
            debuggingRequestInfo = ("Last error: " + error);
        }


        text += debuggingRequestInfo;
        speedLimitView.setDebuggingText(text);
    }

    private String getLimitProviderString(int origin) {
        String provider = "";
        switch (origin) {
            case LimitResponse.ORIGIN_HERE:
                provider = getString(R.string.here_provider_short);
                break;
            case LimitResponse.ORIGIN_TOMTOM:
                provider = getString(R.string.tomtom_provider_short);
                break;
            case LimitResponse.ORIGIN_OSM:
                provider = getString(R.string.openstreetmap_short);
                break;
            case -1:
                provider = "";
                break;
            default:
                provider = String.valueOf(origin);
        }
        return provider;
    }

    private void updateLimitView(boolean success) {
        String text = "--";
        if (currentSpeedLimit != -1) {
            text = String.valueOf(convertToUiSpeed(currentSpeedLimit));
            if (!success) {
                text = "(" + text + ")";
            }
        }

        speedLimitView.setLimitText(text);
    }

    private void updateSpeedometer(Location location) {
        if (location == null || !location.hasSpeed()) {
            return;
        }

        float metersPerSeconds = location.getSpeed();

        int kmhSpeed = (int) Math.round((double) metersPerSeconds * 60 * 60 / 1000);
        int speedometerPercentage = Math.round((float) kmhSpeed / 240 * 100);

        float percentToleranceFactor = 1 + (float) PrefUtils.getSpeedingPercent(this) / 100;
        int constantTolerance = PrefUtils.getSpeedingConstant(this);

        int percentToleratedLimit = (int) (currentSpeedLimit * percentToleranceFactor);
        int warningLimit;
        if (PrefUtils.getToleranceMode(this)) {
            warningLimit = percentToleratedLimit + constantTolerance;
        } else {
            warningLimit = Math.min(percentToleratedLimit, currentSpeedLimit + constantTolerance);
        }

        if (currentSpeedLimit != -1 && kmhSpeed > warningLimit) {
            speedLimitView.setSpeeding(true);
            if (speedingStartTimestamp == -1) {
                speedingStartTimestamp = System.currentTimeMillis();
            } else if (System.currentTimeMillis() > speedingStartTimestamp + 2000L && PrefUtils.isBeepAlertEnabled(this)) {
                Utils.playBeeps();
                speedingStartTimestamp = Long.MAX_VALUE - 2000L;
            }
        } else {
            speedLimitView.setSpeeding(false);
            speedingStartTimestamp = -1;
        }

        speedLimitView.setSpeed(convertToUiSpeed(kmhSpeed), speedometerPercentage);

        lastLocationWithSpeed = location;
    }

    private int convertToUiSpeed(int kmhSpeed) {
        int speed = kmhSpeed;
        if (!PrefUtils.getUseMetric(this)) {
            speed = Utils.convertKmhToMph(speed);
        }
        return speed;
    }

    void showWarningNotification(final String string) {
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_SETTINGS, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationUtils.initChannels(this);
        Notification notification = new NotificationCompat.Builder(this, NotificationUtils.CHANNEL_WARNINGS)
                .setContentTitle(getString(R.string.grant_permission))
                .setContentText(string)
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_speedometer_notif)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(string))
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_WARNING, notification);
    }

    @Override
    public void onDestroy() {
        onStop();
        super.onDestroy();
    }

    private void onStop() {
        if (fusedLocationClient != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            } catch (SecurityException ignore) {
            }
        }


        if (speedLimitView != null)
            speedLimitView.stop();

        if (speedLimitQuerySubscription != null)
            speedLimitQuerySubscription.unsubscribe();

        if (billingManager != null)
            billingManager.destroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (speedLimitView != null)
            speedLimitView.changeConfig();
    }

}
