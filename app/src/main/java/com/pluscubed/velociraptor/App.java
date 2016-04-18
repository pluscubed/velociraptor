package com.pluscubed.velociraptor;

import android.app.Application;
import android.content.Context;

import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.pluscubed.velociraptor.appselection.AppIconLoader;
import com.pluscubed.velociraptor.appselection.AppInfo;
import com.pluscubed.velociraptor.appselection.AppInfoEntity;
import com.pluscubed.velociraptor.appselection.Models;
import com.pluscubed.velociraptor.appselection.SelectedAppDatabase;
import com.pluscubed.velociraptor.utils.PrefUtils;
import com.squareup.leakcanary.LeakCanary;

import java.io.InputStream;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import io.requery.Persistable;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.sql.Configuration;
import io.requery.sql.EntityDataStore;
import io.requery.sql.TableCreationMode;
import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.functions.Func1;

public class App extends Application {

    private EntityDataStore<Persistable> dataStore;

    public static EntityDataStore<Persistable> getData(Context context) {
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

        if (PrefUtils.isFirstRun(this)) {
            SelectedAppDatabase.getMapApps(this)
                    .flatMap(new Func1<List<AppInfoEntity>, Single<?>>() {
                        @Override
                        public Single<?> call(List<AppInfoEntity> mapInfos) {
                            return Observable.from(mapInfos)
                                    .map(new Func1<AppInfoEntity, AppInfoEntity>() {
                                        @Override
                                        public AppInfoEntity call(AppInfoEntity appInfoEntity) {
                                            return getData(App.this).upsert(appInfoEntity);
                                        }
                                    }).toList().toSingle();
                        }
                    }).subscribe(new SingleSubscriber<Object>() {
                @Override
                public void onSuccess(Object value) {

                }

                @Override
                public void onError(Throwable error) {
                    error.printStackTrace();
                    if (!BuildConfig.DEBUG)
                        Crashlytics.logException(error);
                }
            });
        }

        PrefUtils.setFirstRun(this, false);
    }

    /**
     * @return {@link EntityDataStore} single instance for the application.
     */
    EntityDataStore<Persistable> getDataInternal() {
        if (dataStore == null) {
            // override onUpgrade to handle migrating to a new version
            DatabaseSource source = new DatabaseSource(this, Models.DEFAULT, 1);
            if (BuildConfig.DEBUG) {
                // use this in development mode to drop and recreate the tables on every upgrade
                source.setTableCreationMode(TableCreationMode.DROP_CREATE);
            }
            Configuration configuration = source.getConfiguration();
            dataStore = new EntityDataStore<>(configuration);
        }
        return dataStore;
    }
}
