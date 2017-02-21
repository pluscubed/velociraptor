package com.pluscubed.velociraptor.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.api.ApiResponse;
import com.pluscubed.velociraptor.api.SpeedLimitApi;
import com.pluscubed.velociraptor.settings.SettingsActivity;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SpeedLimitService extends Service {
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
    private SpeedLimitView speedLimitView;

    private String debuggingRequestInfo;

    private GoogleApiClient googleApiClient;

    private Subscription getSpeedLimitSubscription;
    private LocationListener locationListener;

    private int lastSpeedLimit = -1;
    private Location lastLocationWithSpeed;
    private Location lastLocationWithFetchAttempt;

    private long speedingStartTimestamp = -1;
    private SpeedLimitApi speedLimitApi;

    private boolean isRunning;
    private boolean isStartedFromNotification;
    private BroadcastReceiver notifCheckBroadcastReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("InflateParams")
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
                boolean hideLimit = intent.getBooleanExtra(EXTRA_HIDE_LIMIT, false);
                speedLimitView.hideLimit(hideLimit);
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

        locationListener = SpeedLimitService.this::onLocationChanged;

        speedLimitApi = new SpeedLimitApi(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    @SuppressWarnings("MissingPermission")
                    public void onConnected(@Nullable Bundle bundle) {
                        LocationRequest locationRequest = new LocationRequest();
                        locationRequest.setInterval(1000);
                        locationRequest.setFastestInterval(0);
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationListener);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        if (googleApiClient != null && googleApiClient.isConnected()) {
                            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener).setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(@NonNull Status status) {
                                }
                            });
                        }
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        showToast("Velociraptor error: " + connectionResult.getErrorMessage());
                    }
                })
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();

        isRunning = true;
        notifCheckBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isStartedFromNotification) {
                    LocalBroadcastManager
                            .getInstance(context)
                            .sendBroadcastSync(new Intent("pong"));
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(notifCheckBroadcastReceiver, new IntentFilter("ping"));

        return super.onStartCommand(intent, flags, startId);
    }

    private void startNotification() {
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_SETTINGS, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_content))
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_speedometer)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(NOTIFICATION_FOREGROUND, notification);
    }

    private boolean prequisitesMet() {
        if (ContextCompat.checkSelfPermission(SpeedLimitService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
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

        if (getSpeedLimitSubscription == null &&
                (lastLocationWithFetchAttempt == null || location.distanceTo(lastLocationWithFetchAttempt) > 10)) {

            getSpeedLimitSubscription = speedLimitApi.getSpeedLimit(location)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleSubscriber<ApiResponse>() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onSuccess(ApiResponse apiResponse) {
                            lastSpeedLimit = apiResponse.speedLimit;
                            lastLocationWithFetchAttempt = location;

                            updateLimitText(true);
                            updateDebuggingText(location, apiResponse, null);
                            getSpeedLimitSubscription = null;
                        }

                        @Override
                        public void onError(Throwable error) {
                            error.printStackTrace();

                            lastLocationWithFetchAttempt = location;

                            updateLimitText(false);
                            updateDebuggingText(location, null, error);
                            getSpeedLimitSubscription = null;
                        }
                    });
        }
    }

    private void updateDebuggingText(Location location, ApiResponse apiResponse, Throwable error) {
        String text = "Location: " + location +
                "\nEndpoints:\n" + speedLimitApi.getApiInformation();

        if (error == null && apiResponse != null) {
            if (apiResponse.roadName != null) {
                debuggingRequestInfo = ("Name: " + apiResponse.roadName);
            } else {
                debuggingRequestInfo = ("Query success");
            }
            debuggingRequestInfo += "\nHERE Maps: " + apiResponse.useHere;
            debuggingRequestInfo += "\nFrom cache: " + apiResponse.fromCache + ", " + apiResponse.timestamp;
        } else if (error != null) {
            debuggingRequestInfo = ("Last Error: " + error);
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

        final int speed;
        int percentage;
        if (PrefUtils.getUseMetric(this)) {
            speed = (int) Math.round((double) metersPerSeconds * 60 * 60 / 1000); //km/h
            percentage = Math.round((float) speed / 200 * 100);
        } else {
            speed = (int) Math.round((double) metersPerSeconds * 60 * 60 / 1000 / 1.609344); //mph
            percentage = Math.round((float) speed / 120 * 100);
        }

        float percentToleranceFactor = 1 + (float) PrefUtils.getSpeedingPercent(this) / 100;
        int constantTolerance = PrefUtils.getSpeedingConstant(this);

        int limitAndPercentTolerance = (int) (lastSpeedLimit * percentToleranceFactor);
        int speedingLimitWarning;
        if (PrefUtils.getToleranceMode(this)) {
            speedingLimitWarning = limitAndPercentTolerance + constantTolerance;
        } else {
            speedingLimitWarning = Math.min(limitAndPercentTolerance, lastSpeedLimit + constantTolerance);
        }

        if (lastSpeedLimit != -1 && speed > speedingLimitWarning) {
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

        speedLimitView.setSpeed(speed, percentage);

        lastLocationWithSpeed = location;
    }

    void showToast(final String string) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            Toast.makeText(SpeedLimitService.this.getApplicationContext(), string, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDestroy() {
        onStop();
        super.onDestroy();
    }

    private void onStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (googleApiClient != null && googleApiClient.isConnected()) {
                        googleApiClient.disconnect();
                    }
                }
            });
        } else if (googleApiClient != null) {
            googleApiClient.disconnect();
        }

        if (speedLimitView != null)
            speedLimitView.stop();

        if (getSpeedLimitSubscription != null) {
            getSpeedLimitSubscription.unsubscribe();
        }

        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(notifCheckBroadcastReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (speedLimitView != null)
            speedLimitView.changeConfig();
    }

}
