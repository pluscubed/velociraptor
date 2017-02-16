package com.pluscubed.velociraptor;

import android.app.Application;

import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.pluscubed.velociraptor.settings.appselection.AppIconLoader;
import com.pluscubed.velociraptor.settings.appselection.AppInfo;
import com.pluscubed.velociraptor.settings.appselection.SelectedAppDatabase;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.squareup.leakcanary.LeakCanary;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import rx.Observable;
import rx.SingleSubscriber;
import rx.functions.Func1;
import timber.log.Timber;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        LeakCanary.install(this);

        Glide.get(this)
                .register(AppInfo.class, InputStream.class, new AppIconLoader.Factory());

        if (PrefUtils.isFirstRun(this)) {
            SelectedAppDatabase.getMapApps(this)
                    .flatMapObservable(new Func1<List<AppInfo>, Observable<AppInfo>>() {
                        @Override
                        public Observable<AppInfo> call(List<AppInfo> mapInfos) {
                            return Observable.from(mapInfos);
                        }
                    })
                    .filter(new Func1<AppInfo, Boolean>() {
                        @Override
                        public Boolean call(AppInfo appInfoEntity) {
                            return appInfoEntity.packageName != null &&
                                    !appInfoEntity.packageName.isEmpty();
                        }
                    })
                    .map(new Func1<AppInfo, String>() {
                        @Override
                        public String call(AppInfo appInfo) {
                            return appInfo.packageName;
                        }
                    })
                    .toList().toSingle()
                    .subscribe(new SingleSubscriber<List<String>>() {
                        @Override
                        public void onSuccess(List<String> value) {
                            PrefUtils.setApps(App.this, new HashSet<>(value));
                        }

                        @Override
                        public void onError(Throwable error) {
                            error.printStackTrace();
                            if (!BuildConfig.DEBUG)
                                Crashlytics.logException(error);
                        }
                    });
        }
    }
}
