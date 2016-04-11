package com.pluscubed.velociraptor;

import android.Manifest;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.NotificationCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.gigamole.library.ArcProgressStackView;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.pluscubed.velociraptor.hereapi.Link;
import com.pluscubed.velociraptor.hereapi.LinkInfo;
import com.pluscubed.velociraptor.osm.Element;
import com.pluscubed.velociraptor.osm.OsmApi;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class FloatingService extends Service {
    public static final int PENDING_CLOSE = 5;
    public static final String HERE_ROUTING_API = "http://route.st.nlp.nokia.com/routing/6.2/";
    public static final String OSM_OVERPASS_API = "http://overpass-api.de/api/";
    private static final int NOTIFICATION_FLOATING_WINDOW = 303;
    private WindowManager mWindowManager;

    private View mFloatingView;
    private View mSpeedometer;
    private TextView mLimitText;
    private TextView mStreetText;
    private TextView mSpeedometerText;
    private TextView mSpeedometerUnits;
    private ArcProgressStackView mArcView;

    private GoogleApiClient mGoogleApiClient;

    private HereService mHereService;
    private Subscription mLocationSubscription;
    private LocationListener mLocationListener;

    private int mLastSpeedLimit;
    private Location mLastSpeedLimitLocation;
    private OsmService mOsmService;

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

        if (ContextCompat.checkSelfPermission(FloatingService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))) {
            showToast(R.string.permissions_warning);
            stopSelf();
            return;
        }

        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showToast(R.string.location_settings_warning);
            stopSelf();
            return;
        }

        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_CLOSE, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_content))
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_speedometer)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_FLOATING_WINDOW, notification);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layout;
        switch (PrefUtils.getSignStyle(this)) {
            case PrefUtils.STYLE_US:
                layout = R.layout.floating_us;
                break;
            case PrefUtils.STYLE_INTERNATIONAL:
            default:
                layout = R.layout.floating_international;
                break;
        }

        mFloatingView = LayoutInflater.from(new ContextThemeWrapper(this, R.style.Theme_Velociraptor))
                .inflate(layout, null, false);

        mLimitText = (TextView) mFloatingView.findViewById(R.id.text);
        mStreetText = (TextView) mFloatingView.findViewById(R.id.subtext);
        mArcView = (ArcProgressStackView) mFloatingView.findViewById(R.id.arcview);
        mSpeedometerText = (TextView) mFloatingView.findViewById(R.id.speed);
        mSpeedometerUnits = (TextView) mFloatingView.findViewById(R.id.speedUnits);
        mSpeedometer = mFloatingView.findViewById(R.id.speedometer);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;

        initFloatingViewPosition();

        mWindowManager.addView(mFloatingView, params);

        mFloatingView.setOnTouchListener(new FloatingOnTouchListener());

        updateUnits();

        final ArrayList<ArcProgressStackView.Model> models = new ArrayList<>();
        models.add(new ArcProgressStackView.Model("", 0, ContextCompat.getColor(this, R.color.colorPrimary), ContextCompat.getColor(this, R.color.colorAccent)));
        mArcView.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        mArcView.setInterpolator(new FastOutSlowInInterpolator());
        mArcView.setModels(models);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Retrofit hereRest = new Retrofit.Builder()
                .baseUrl(HERE_ROUTING_API)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        mHereService = hereRest.create(HereService.class);

        final Retrofit osmRest = new Retrofit.Builder()
                .baseUrl(OSM_OVERPASS_API)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        mOsmService = osmRest.create(OsmService.class);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                float metersPerSeconds = location.getSpeed();

                final int speed;
                int percentage;
                if (PrefUtils.getUseMetric(FloatingService.this)) {
                    speed = (int) ((metersPerSeconds * 60 * 60 / 1000) + 0.5f); //km/h
                    percentage = (int) ((float) speed / 200 * 100 + 0.5f);
                } else {
                    speed = (int) ((metersPerSeconds * 60 * 60 / 1000 / 1.609344) + 0.5f); //mph
                    percentage = (int) ((float) speed / 120 * 100 + 0.5f);
                }

                updateUnits();

                mSpeedometerText.setText(String.valueOf(speed));

                mArcView.getModels().get(0).setProgress(percentage);
                mArcView.setAnimatorListener(new AnimatorListenerAdapter() {
                });
                mArcView.animateProgress();

                if (mLastSpeedLimit != 0 && mLastSpeedLimit * (1 + (double) PrefUtils.getOverspeedPercent(FloatingService.this) / 100) < speed) {
                    mSpeedometerText.setTextColor(ContextCompat.getColor(FloatingService.this, R.color.red500));
                } else {
                    mSpeedometerText.setTextColor(ContextCompat.getColor(FloatingService.this, R.color.primary_text_default_material_light));
                }

                mSpeedometer.setVisibility(PrefUtils.getShowSpeedometer(FloatingService.this) ? View.VISIBLE : View.GONE);


                if (mLocationSubscription == null &&
                        (mLastSpeedLimitLocation == null || location.distanceTo(mLastSpeedLimitLocation) > 50)) {

                    mLocationSubscription = getOsmSpeedLimit(location)
                            .switchIfEmpty(getHereSpeedLimit(location).toObservable())
                            .toSingle()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleSubscriber<Integer>() {
                                @Override
                                public void onSuccess(Integer speedLimit) {
                                    mLastSpeedLimitLocation = location;

                                    if (speedLimit != null) {
                                        mLastSpeedLimit = speedLimit;
                                        mLimitText.setText(String.valueOf(speedLimit));
                                    } else {
                                        mLastSpeedLimit = 0;
                                        mLimitText.setText("--");
                                    }

                                    mLocationSubscription = null;
                                }

                                @Override
                                public void onError(Throwable error) {
                                    error.printStackTrace();
                                    Crashlytics.logException(error);

                                    mLocationSubscription = null;
                                }
                            });
                }
            }
        };


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        LocationRequest locationRequest = new LocationRequest();
                        locationRequest.setInterval(1000);
                        locationRequest.setFastestInterval(1000);
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, mLocationListener);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
                        }
                    }
                })
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    private String getOsmQuery(Location location) {
        return "[out:json];" +
                "way(around:" +
                (int) (location.getAccuracy() + 0.5f) + ","
                + location.getLatitude() + ","
                + location.getLongitude() +
                ")" +
                "[\"highway\"][\"maxspeed\"];(._;>;);out;";
    }

    private Observable<Integer> getOsmSpeedLimit(final Location location) {
        return mOsmService.getOsm(getOsmQuery(location))
                .subscribeOn(Schedulers.io())
                .toObservable()
                .onErrorResumeNext(Observable.<OsmApi>empty())
                .doOnNext(new Action1<OsmApi>() {
                    @Override
                    public void call(OsmApi osmApi) {
                        if (!BuildConfig.DEBUG)
                            Answers.getInstance().logCustom(new CustomEvent("OSM Request")
                                    .putCustomAttribute("location", location.toString()));
                    }
                })
                .flatMap(new Func1<OsmApi, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(OsmApi osmApi) {
                        boolean useMetric = PrefUtils.getUseMetric(FloatingService.this);

                        List<Element> elements = osmApi.getElements();
                        if (!elements.isEmpty()) {
                            Element element = elements.get(elements.size() - 1);
                            String maxspeed = element.getTags().getMaxspeed();
                            if (maxspeed.matches("^-?\\d+$")) {
                                //is integer -> km/h
                                Integer limit = Integer.valueOf(maxspeed);
                                if (!useMetric) {
                                    limit = (int) (limit / 1.609344);
                                }
                                return Observable.just(limit);
                            } else if (maxspeed.contains("mph")) {
                                String[] split = maxspeed.split(" ");
                                Integer limit = Integer.valueOf(split[0]);
                                if (useMetric) {
                                    limit = (int) (limit * 1.609344);
                                }
                                return Observable.just(limit);
                            }
                        }
                        return Observable.empty();
                    }
                });
    }

    private Single<Integer> getHereSpeedLimit(final Location location) {
        final String query = location.getLatitude() + "," + location.getLongitude();
        return mHereService.getLinkInfo(query, getString(R.string.here_app_id), getString(R.string.here_app_code))
                .subscribeOn(Schedulers.io())
                .onErrorReturn(new Func1<Throwable, LinkInfo>() {
                    @Override
                    public LinkInfo call(Throwable throwable) {
                        return null;
                    }
                })
                .doOnSuccess(new Action1<LinkInfo>() {
                    @Override
                    public void call(LinkInfo linkInfo) {
                        if (!BuildConfig.DEBUG)
                            Answers.getInstance().logCustom(new CustomEvent("OSM Request")
                                    .putCustomAttribute("location", location.toString()));
                    }
                })
                .map(new Func1<LinkInfo, Integer>() {
                    @Override
                    public Integer call(LinkInfo linkInfo) {
                        Link link = linkInfo.getResponse().getLink().get(0);
                        Double speedLimit = link.getSpeedLimit();
                        if (speedLimit != null) {
                            double factor = PrefUtils.getUseMetric(FloatingService.this) ? 3.6 : 2.23;
                            return (int) (speedLimit * factor + 0.5d);
                        }
                        return null;
                    }
                });
    }

    private void showToast(final int string) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FloatingService.this.getApplicationContext(), string, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUnits() {
        if (PrefUtils.getUseMetric(FloatingService.this)) {
            mSpeedometerUnits.setText("km/h");
        } else {
            mSpeedometerUnits.setText("mph");
        }
    }

    private void initFloatingViewPosition() {
        mFloatingView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();

                String[] split = PrefUtils.getFloatingLocation(FloatingService.this).split(",");
                boolean left = Boolean.parseBoolean(split[0]);
                float yRatio = Float.parseFloat(split[1]);

                Point screenSize = new Point();
                mWindowManager.getDefaultDisplay().getSize(screenSize);
                params.x = left ? 0 : screenSize.x - mFloatingView.getWidth();
                params.y = (int) (yRatio * screenSize.y + 0.5f);

                mWindowManager.updateViewLayout(mFloatingView, params);

                mFloatingView.setVisibility(View.VISIBLE);

                mFloatingView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
            mGoogleApiClient.disconnect();
        }

        if (mFloatingView != null && mFloatingView.isShown())
            mWindowManager.removeView(mFloatingView);

        if (mLocationSubscription != null) {
            mLocationSubscription.unsubscribe();
        }
    }

    void animateViewToSideSlot() {
        Point screenSize = new Point();
        mWindowManager.getDefaultDisplay().getSize(screenSize);

        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();
        int endX;
        if (params.x + mFloatingView.getWidth() / 2 >= screenSize.x / 2) {
            endX = screenSize.x - mFloatingView.getWidth();
        } else {
            endX = 0;
        }

        PrefUtils.setFloatingLocation(FloatingService.this, (float) params.y / screenSize.y, endX == 0);

        ValueAnimator valueAnimator = ValueAnimator.ofInt(params.x, endX)
                .setDuration(300);
        valueAnimator.setInterpolator(new LinearOutSlowInInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) mFloatingView.getLayoutParams();
                params.x = (int) animation.getAnimatedValue();

                if (mFloatingView.isShown()) {
                    mWindowManager.updateViewLayout(mFloatingView, params);
                }
            }
        });

        valueAnimator.start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        initFloatingViewPosition();
    }

    private interface HereService {
        @GET("getlinkinfo.json")
        Single<LinkInfo> getLinkInfo(@Query("waypoint") String waypoint, @Query("app_id") String appId, @Query("app_code") String appCode);
    }

    private interface OsmService {
        @GET("interpreter")
        Single<OsmApi> getOsm(@Query("data") String data);
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
                        animateViewToSideSlot();
                    }
                    return true;
            }
            return false;
        }
    }

}
