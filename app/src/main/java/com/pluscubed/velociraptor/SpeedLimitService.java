package com.pluscubed.velociraptor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.pluscubed.velociraptor.settings.SettingsActivity;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.pluscubed.velociraptor.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class SpeedLimitService extends Service {
    public static final int PENDING_SETTINGS = 5;

    public static final String EXTRA_NOTIF_START = "com.pluscubed.velociraptor.EXTRA_NOTIF_START";
    public static final String EXTRA_NOTIF_CLOSE = "com.pluscubed.velociraptor.EXTRA_NOTIF_CLOSE";
    public static final String EXTRA_CLOSE = "com.pluscubed.velociraptor.EXTRA_CLOSE";
    public static final String EXTRA_PREF_CHANGE = "com.pluscubed.velociraptor.EXTRA_PREF_CHANGE";

    public static final String EXTRA_VIEW = "com.pluscubed.velociraptor.EXTRA_VIEW";
    public static final int VIEW_FLOATING = 0;
    public static final int VIEW_AUTO = 1;
    private static final int NOTIFICATION_FOREGROUND = 303;
    private int speedLimitViewType;
    private SpeedLimitView speedLimitView;
    private String mDebuggingRequestInfo;
    private GoogleApiClient mGoogleApiClient;
    private Subscription mLocationSubscription;
    private LocationListener mLocationListener;
    private int mLastSpeedLimit = -1;
    private Location mLastSpeedLocation;
    private Location mLastLimitLocation;
    private long mLastRequestTime;
    private long mSpeedingStart = -1;
    private SpeedLimitApi mSpeedLimitApi;
    private boolean mInitialized;
    private boolean mNotifStart;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("InflateParams")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (!mNotifStart && intent.getBooleanExtra(EXTRA_CLOSE, false) ||
                    intent.getBooleanExtra(EXTRA_NOTIF_CLOSE, false)) {
                onStop();
                stopSelf();
                return super.onStartCommand(intent, flags, startId);
            } else if (intent.getBooleanExtra(EXTRA_NOTIF_START, false)) {
                mNotifStart = true;
            } else if (intent.getBooleanExtra(EXTRA_PREF_CHANGE, false)) {
                speedLimitView.updatePrefs();

                updateLimitText(false);
                updateSpeedometer(mLastSpeedLocation);
            }

            int viewType = intent.getIntExtra(EXTRA_VIEW, speedLimitViewType);
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
        }


        if (mInitialized || prequisitesNotMet())
            return super.onStartCommand(intent, flags, startId);

        startNotification();

        speedLimitView = new FloatingView(this);

        mDebuggingRequestInfo = "";

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                SpeedLimitService.this.onLocationChanged(location);
            }
        };

        mSpeedLimitApi = new SpeedLimitApi(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    @SuppressWarnings("MissingPermission")
                    public void onConnected(@Nullable Bundle bundle) {
                        LocationRequest locationRequest = new LocationRequest();
                        locationRequest.setInterval(1000);
                        locationRequest.setFastestInterval(0);
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, mLocationListener);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener).setResultCallback(new ResultCallback<Status>() {
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

        mGoogleApiClient.connect();

        mInitialized = true;

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

    private boolean prequisitesNotMet() {
        if (ContextCompat.checkSelfPermission(SpeedLimitService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))) {
            showToast(getString(R.string.permissions_warning));
            stopSelf();
            return true;
        }

        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showToast(getString(R.string.location_settings_warning));
            stopSelf();
            return true;
        }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            showToast(getString(R.string.network_warning));
            stopSelf();
            return true;
        }
        return false;
    }

    private void onLocationChanged(final Location location) {
        updateSpeedometer(location);
        updateDebuggingText(location, null, null);

        if (mLocationSubscription == null &&
                (mLastLimitLocation == null || location.distanceTo(mLastLimitLocation) > 100) &&
                System.currentTimeMillis() > mLastRequestTime + 5000) {

            mLastRequestTime = System.currentTimeMillis();
            mLocationSubscription = mSpeedLimitApi.getSpeedLimit(location)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleSubscriber<SpeedLimitApi.ApiResponse>() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onSuccess(SpeedLimitApi.ApiResponse apiResponse) {
                            mLastLimitLocation = location;

                            mLastSpeedLimit = apiResponse.speedLimit;

                            updateLimitText(true);

                            updateDebuggingText(location, apiResponse, null);

                            mLocationSubscription = null;
                        }

                        @Override
                        public void onError(Throwable error) {
                            updateLimitText(false);

                            updateDebuggingText(location, null, error);

                            mLocationSubscription = null;
                        }
                    });
        }
    }

    private void updateDebuggingText(Location location, SpeedLimitApi.ApiResponse apiResponse, Throwable error) {
        String text = "Location: " + location +
                "\nEndpoints:\n" + mSpeedLimitApi.getApiInformation();

        if (error == null && apiResponse != null) {
            if (apiResponse.roadNames != null) {
                mDebuggingRequestInfo = ("Name(s): " + TextUtils.join(", ", apiResponse.roadNames));
            } else {
                mDebuggingRequestInfo = ("Success, no road data");
            }
            mDebuggingRequestInfo += "\nHERE Maps: " + apiResponse.useHere;
        } else if (error != null) {
            mDebuggingRequestInfo = ("Last Error: " + error);
        }

        text += mDebuggingRequestInfo;
        speedLimitView.setDebuggingText(text);
    }

    private void updateLimitText(boolean success) {
        String text = "--";
        if (mLastSpeedLimit != -1) {
            text = String.valueOf(mLastSpeedLimit);
            if (!success) {
                text = String.format("(%s)", text);
            }
        }

        speedLimitView.setLimitText(text);
    }

    private void updateSpeedometer(Location location) {
        if (location != null && location.hasSpeed()) {
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

            int limitAndPercentTolerance = (int) (mLastSpeedLimit * percentToleranceFactor);
            int speedingLimitWarning;
            if (PrefUtils.getToleranceMode(this)) {
                speedingLimitWarning = limitAndPercentTolerance + constantTolerance;
            } else {
                speedingLimitWarning = Math.min(limitAndPercentTolerance, mLastSpeedLimit + constantTolerance);
            }

            if (mLastSpeedLimit != -1 && speed > speedingLimitWarning) {
                speedLimitView.setSpeeding(true);
                if (mSpeedingStart == -1) {
                    mSpeedingStart = System.currentTimeMillis();
                } else if (System.currentTimeMillis() > mSpeedingStart + 2000L && PrefUtils.isBeepAlertEnabled(this)) {
                    Utils.playBeep();
                    mSpeedingStart = Long.MAX_VALUE - 2000L;
                }
            } else {
                speedLimitView.setSpeeding(false);
                mSpeedingStart = -1;
            }


            if (mLastSpeedLimit != -1) {
                percentage = Math.round((float) speed / speedingLimitWarning * 100);
            }
            speedLimitView.setSpeed(speed, percentage);

            mLastSpeedLocation = location;
        }
    }

    void showToast(final String string) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SpeedLimitService.this.getApplicationContext(), string, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        onStop();
        super.onDestroy();
    }

    private void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                        mGoogleApiClient.disconnect();
                    }
                }
            });
        } else if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

        if (speedLimitView != null)
            speedLimitView.stop();

        if (mLocationSubscription != null) {
            mLocationSubscription.unsubscribe();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        speedLimitView.changeConfig();
    }


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            VIEW_FLOATING,
            VIEW_AUTO
    })
    public @interface SpeedLimitViewType {
    }

}
