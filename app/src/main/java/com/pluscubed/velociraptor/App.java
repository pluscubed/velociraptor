package com.pluscubed.velociraptor;

import android.app.Application;
import android.content.Context;

import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.pluscubed.velociraptor.appselection.AppIconLoader;
import com.pluscubed.velociraptor.appselection.AppInfo;
import com.pluscubed.velociraptor.appselection.Models;
import com.squareup.leakcanary.LeakCanary;

import java.io.InputStream;

import io.fabric.sdk.android.Fabric;
import io.requery.Persistable;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.rx.RxSupport;
import io.requery.rx.SingleEntityStore;
import io.requery.sql.Configuration;
import io.requery.sql.EntityDataStore;
import io.requery.sql.TableCreationMode;

public class App extends Application {

    private SingleEntityStore<Persistable> dataStore;

    public static SingleEntityStore<Persistable> getData(Context context) {
        return ((App) context.getApplicationContext()).getDataInternal();
    }

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

    /**
     * @return {@link EntityDataStore} single instance for the application.
     */
    SingleEntityStore<Persistable> getDataInternal() {
        if (dataStore == null) {
            // override onUpgrade to handle migrating to a new version
            DatabaseSource source = new DatabaseSource(this, Models.DEFAULT, 1);
            if (BuildConfig.DEBUG) {
                // use this in development mode to drop and recreate the tables on every upgrade
                source.setTableCreationMode(TableCreationMode.DROP_CREATE);
            }
            Configuration configuration = source.getConfiguration();
            dataStore = RxSupport.toReactiveStore(
                    new EntityDataStore<Persistable>(configuration));
        }
        return dataStore;
    }
}
