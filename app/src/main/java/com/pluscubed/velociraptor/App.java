package com.pluscubed.velociraptor;

import android.app.Application;

import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.pluscubed.velociraptor.appselection.AppIconLoader;
import com.pluscubed.velociraptor.appselection.AppInfo;
import com.squareup.leakcanary.LeakCanary;

import java.io.InputStream;

import io.fabric.sdk.android.Fabric;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }

        LeakCanary.install(this);

        Glide.get(this)
                .register(AppInfo.class, InputStream.class, new AppIconLoader.Factory());
    }
}
