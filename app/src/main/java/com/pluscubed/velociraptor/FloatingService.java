package com.pluscubed.velociraptor;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.NotificationCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.pluscubed.velociraptor.hereapi.GetLinkInfo;
import com.pluscubed.velociraptor.hereapi.Link;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FloatingService extends Service {
    public static final int PENDING_CLOSE = 5;
    private static final int NOTIFICATION_FLOATING_WINDOW = 303;

    private WindowManager mWindowManager;

    private View mFloatingView;
    private TextView mLimitText;
    private TextView mStreetText;

    private GoogleApiClient mGoogleApiClient;

    private HereService mService;
    private Subscription mHereLocationSubscription;
    private LocationListener mLocationListener;

    public static int convertDpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_CLOSE, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_content))
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_my_location_black_24dp)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_FLOATING_WINDOW, notification);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mFloatingView = LayoutInflater.from(this).inflate(R.layout.floating, null, false);

        mLimitText = (TextView) mFloatingView.findViewById(R.id.text);
        mStreetText = (TextView) mFloatingView.findViewById(R.id.subtext);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        mWindowManager.addView(mFloatingView, params);

        mFloatingView.setOnTouchListener(new FloatingOnTouchListener());

        Retrofit restAdapter = new Retrofit.Builder()
                .baseUrl("http://route.st.nlp.nokia.com/routing/6.2/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        mService = restAdapter.create(HereService.class);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (mHereLocationSubscription == null) {
                    final String text = location.getLatitude() + "," + location.getLongitude();
                    mHereLocationSubscription = mService.getLinkInfo(text, getString(R.string.here_app_id), getString(R.string.here_app_code))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleSubscriber<GetLinkInfo>() {
                                @Override
                                public void onSuccess(GetLinkInfo getLinkInfo) {
                                    Link link = getLinkInfo.getResponse().getLink().get(0);
                                    Double speedLimit = link.getSpeedLimit();
                                    if (speedLimit != null) {
                                        int limit = (int) (speedLimit * 2.23 + 0.5d);
                                        mLimitText.setText(String.valueOf(limit));
                                    } else {
                                        mLimitText.setText("--");
                                    }

                                    mStreetText.setText(link.getAddress().getLabel());

                                    //mLimitText.append("\n"+text);

                                    mHereLocationSubscription = null;
                                }

                                @Override
                                public void onError(Throwable error) {
                                    error.printStackTrace();
                                    mHereLocationSubscription = null;
                                }
                            });
                }
            }
        };


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        if (ContextCompat.checkSelfPermission(FloatingService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Open settings to grant permission
                            return;
                        }

                        LocationRequest locationRequest = new LocationRequest();
                        locationRequest.setInterval(5000);
                        locationRequest.setFastestInterval(5000);
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, mLocationListener);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
            mGoogleApiClient.disconnect();
        }

        if (mFloatingView.isShown())
            mWindowManager.removeView(mFloatingView);
    }

    void animateViewToSideSlot(final View view) {
        Point screenSize = new Point();
        mWindowManager.getDefaultDisplay().getSize(screenSize);

        WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
        int endX;
        if (params.x + view.getWidth() / 2 >= screenSize.x / 2) {
            endX = screenSize.x - view.getWidth();
        } else {
            endX = 0;
        }

        ValueAnimator valueAnimator = ValueAnimator.ofInt(params.x, endX)
                .setDuration(300);
        valueAnimator.setInterpolator(new LinearOutSlowInInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
                params.x = (int) animation.getAnimatedValue();

                if (view.isShown()) {
                    mWindowManager.updateViewLayout(view, params);
                }
            }
        });

        valueAnimator.start();
    }

    private interface HereService {
        @GET("getlinkinfo.json")
        Single<GetLinkInfo> getLinkInfo(@Query("waypoint") String waypoint, @Query("app_id") String appId, @Query("app_code") String appCode);
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {

        private float mInitialTouchX;
        private float mInitialTouchY;
        private int mInitialX;
        private int mInitialY;
        private long mStartClickTime;
        private boolean mIsClick;

        public FloatingOnTouchListener() {
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mInitialTouchX = event.getRawX();
                    mInitialTouchY = event.getRawY();

                    mInitialX = params.x;
                    mInitialY = params.y;

                    mStartClickTime = System.currentTimeMillis();

                    mIsClick = true;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dX = event.getRawX() - mInitialTouchX;
                    float dY = event.getRawY() - mInitialTouchY;
                    if ((mIsClick && (Math.abs(dX) > 10 || Math.abs(dY) > 10))
                            || System.currentTimeMillis() - mStartClickTime > ViewConfiguration.getLongPressTimeout()) {
                        mIsClick = false;
                    }

                    if (!mIsClick) {
                        params.x = (int) (dX + mInitialX);
                        params.y = (int) (dY + mInitialY);

                        mWindowManager.updateViewLayout(mFloatingView, params);
                    }
                    return true;
                case MotionEvent.ACTION_UP:

                    if (mIsClick && System.currentTimeMillis() - mStartClickTime <= ViewConfiguration.getLongPressTimeout()) {
                        //TODO: On Click
                    } else {
                        animateViewToSideSlot(mFloatingView);
                    }
                    return true;
            }
            return false;
        }
    }

}
