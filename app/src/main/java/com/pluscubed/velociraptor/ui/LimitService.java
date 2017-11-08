package com.pluscubed.velociraptor.ui;

import android.annotation.SuppressLint;
import android.app.Notification;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.api.LimitFetcher;
import com.pluscubed.velociraptor.api.LimitResponse;
import com.pluscubed.velociraptor.settings.SettingsActivity;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import rx.SingleSubscriber;
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
    public static final int VIEW_AUTO = 1;

    public static final String EXTRA_HIDE_LIMIT = "com.pluscubed.velociraptor.HIDE_LIMIT";

    private static final int NOTIFICATION_FOREGROUND = 303;

    private int speedLimitViewType = -1;
    private LimitView speedLimitView;

    private String debuggingRequestInfo;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Subscription getSpeedLimitSubscription;

    private int lastSpeedLimit = -1;
    private Location lastLocationWithSpeed;
    private Location lastLocationWithFetchAttempt;

    private long speedingStartTimestamp = -1;
    private LimitFetcher limitFetcher;

    private boolean isRunning;
    private boolean isStartedFromNotification;
    private boolean isLimitHidden;

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
                    case VIEW_AUTO:
                        speedLimitView = new AutoView(this);
                        break;
                }
            }

            if (intent.getExtras() != null && intent.getExtras().containsKey(EXTRA_HIDE_LIMIT)) {
                isLimitHidden = intent.getBooleanExtra(EXTRA_HIDE_LIMIT, false);
                speedLimitView.hideLimit(isLimitHidden);
                if (isLimitHidden) {
                    lastSpeedLimit = -1;
                }
            }

            if (intent.getBooleanExtra(EXTRA_NOTIF_START, false)) {
                isStartedFromNotification = true;
            } else if (intent.getBooleanExtra(EXTRA_PREF_CHANGE, false)) {
                speedLimitView.updatePrefs();

                updateLimitText(false);
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
            showToast("Velociraptor cannot obtain location");
        }

        isRunning = true;

        return super.onStartCommand(intent, flags, startId);
    }

    private void startNotification() {
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_SETTINGS, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_content))
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_speedometer_notif)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_FOREGROUND, notification);
    }

    private boolean prequisitesMet() {
        if (!Utils.isLocationPermissionGranted(LimitService.this)
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))) {
            showToast(getString(R.string.permissions_warning));
            stopSelf();
            return false;
        }

        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showToast(getString(R.string.location_settings_warning));
        }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            showToast(getString(R.string.network_warning));
        }
        return true;
    }

    private void onLocationChanged(final Location location) {
        updateSpeedometer(location);
        updateDebuggingText(location, null, null);

        if (getSpeedLimitSubscription == null && !isLimitHidden &&
                (lastLocationWithFetchAttempt == null || location.distanceTo(lastLocationWithFetchAttempt) > 10)) {

            getSpeedLimitSubscription = limitFetcher.getSpeedLimit(location)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleSubscriber<LimitResponse>() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onSuccess(LimitResponse limitResponse) {
                            lastSpeedLimit = limitResponse.speedLimit();
                            lastLocationWithFetchAttempt = location;

                            updateLimitText(true);
                            updateDebuggingText(location, limitResponse, null);
                            getSpeedLimitSubscription = null;
                        }

                        @Override
                        public void onError(Throwable error) {
                            Timber.d(error);

                            lastLocationWithFetchAttempt = location;

                            updateLimitText(false);
                            updateDebuggingText(location, null, error);
                            getSpeedLimitSubscription = null;
                        }
                    });
        }
    }

    private void updateDebuggingText(Location location, LimitResponse limitResponse, Throwable error) {
        String text = "Location: " + location +
                "\nEndpoints:\n" + limitFetcher.getApiInformation();

        if (error == null && limitResponse != null) {
            debuggingRequestInfo = ("Road name: " + limitResponse.roadName());
            debuggingRequestInfo += "\nFrom cache: " + limitResponse.fromCache();
        } else if (error != null) {
            debuggingRequestInfo = ("Last error: " + error);
        }

        text += debuggingRequestInfo;
        speedLimitView.setDebuggingText(text);
    }

    private void updateLimitText(boolean success) {
        String text = "--";
        if (lastSpeedLimit != -1) {
            text = String.valueOf(lastSpeedLimit);
            if (!success) {
                text = String.format("(%s)", text);
            }
        }

        speedLimitView.setLimitText(text);
    }

    private void updateSpeedometer(Location location) {
        if (location == null || !location.hasSpeed()) {
            return;
        }

        float metersPerSeconds = location.getSpeed();

        //In km/h
        final int speed = (int) Math.round((double) metersPerSeconds * 60 * 60 / 1000);
        int speedometerPercentage = Math.round((float) speed / 200 * 100);

        float percentToleranceFactor = 1 + (float) PrefUtils.getSpeedingPercent(this) / 100;
        int constantTolerance = PrefUtils.getSpeedingConstant(this);

        int percentToleratedLimit = (int) (lastSpeedLimit * percentToleranceFactor);
        int warningLimit;
        if (PrefUtils.getToleranceMode(this)) {
            warningLimit = percentToleratedLimit + constantTolerance;
        } else {
            warningLimit = Math.min(percentToleratedLimit, lastSpeedLimit + constantTolerance);
        }

        if (lastSpeedLimit != -1 && speed > warningLimit) {
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

        speedLimitView.setSpeed(speed, speedometerPercentage);

        lastLocationWithSpeed = location;
    }

    void showToast(final String string) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(LimitService.this.getApplicationContext(), string, Toast.LENGTH_LONG).show());
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

        if (getSpeedLimitSubscription != null) {
            getSpeedLimitSubscription.unsubscribe();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (speedLimitView != null)
            speedLimitView.changeConfig();
    }

}
