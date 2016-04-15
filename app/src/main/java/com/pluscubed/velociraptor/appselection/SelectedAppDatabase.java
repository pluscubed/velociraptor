package com.pluscubed.velociraptor.appselection;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;

import com.pluscubed.velociraptor.App;
import com.pluscubed.velociraptor.BuildConfig;

import java.util.List;

import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SelectedAppDatabase {

    @NonNull
    public static Single<List<AppInfoEntity>> getSelectedApps(final Context context) {
        return App.getData(context).select(AppInfoEntity.class)
                .get().toObservable()
                .subscribeOn(Schedulers.io())
                .toList().toSingle()
                .flatMap(new Func1<List<AppInfoEntity>, Single<List<AppInfoEntity>>>() {
                    @Override
                    public Single<List<AppInfoEntity>> call(List<AppInfoEntity> appInfos) {
                        if (appInfos.isEmpty()) {
                            Uri gmmIntentUri = Uri.parse("geo:37.421999,-122.084056");
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            PackageManager manager = context.getPackageManager();
                            List<ResolveInfo> mapApps;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                mapApps = manager.queryIntentActivities(mapIntent, PackageManager.MATCH_ALL);
                            } else {
                                mapApps = manager.queryIntentActivities(mapIntent, PackageManager.MATCH_DEFAULT_ONLY);
                            }

                            for (ResolveInfo info : mapApps) {
                                AppInfoEntity appInfo = new AppInfoEntity();
                                appInfo.packageName = info.activityInfo.packageName;
                                appInfos.add(appInfo);
                            }
                            appInfos = (List<AppInfoEntity>) App.getData(context).insert(appInfos).toBlocking().value();
                        }
                        return Single.just(appInfos);
                    }
                });
    }

    /**
     * Returns sorted list of AppInfos.
     */
    public static Single<List<AppInfoEntity>> getInstalledApps(final Context context) {
        return Single.create(new Single.OnSubscribe<List<ApplicationInfo>>() {
            @Override
            public void call(SingleSubscriber<? super List<ApplicationInfo>> singleSubscriber) {
                singleSubscriber.onSuccess(context.getPackageManager().getInstalledApplications(0));
            }
        }).subscribeOn(Schedulers.io())
                .flatMapObservable(new Func1<List<ApplicationInfo>, Observable<ApplicationInfo>>() {
                    @Override
                    public Observable<ApplicationInfo> call(List<ApplicationInfo> appInfos) {
                        return Observable.from(appInfos);
                    }
                })
                .map(new Func1<ApplicationInfo, AppInfoEntity>() {
                    @Override
                    public AppInfoEntity call(ApplicationInfo applicationInfo) {
                        AppInfoEntity appInfo = new AppInfoEntity();
                        appInfo.packageName = applicationInfo.packageName;
                        appInfo.name = applicationInfo.loadLabel(context.getPackageManager()).toString();
                        return appInfo;
                    }
                })
                .toList().toSingle()
                .flatMapObservable(new Func1<List<AppInfoEntity>, Observable<AppInfoEntity>>() {
                    @Override
                    public Observable<AppInfoEntity> call(List<AppInfoEntity> appInfos) {
                        List<AppInfoEntity> enabledApps = getSelectedApps(context).toBlocking().value();
                        for (AppInfo enabledApp : enabledApps) {
                            for (AppInfo info : appInfos) {
                                if (info.packageName.equals(enabledApp.packageName)) {
                                    info.enabled = true;
                                    info.id = enabledApp.id;
                                }
                            }
                        }

                        return Observable.from(appInfos);
                    }
                })
                .filter(new Func1<AppInfoEntity, Boolean>() {
                    @Override
                    public Boolean call(AppInfoEntity appInfoEntity) {
                        return !appInfoEntity.packageName.equals(BuildConfig.APPLICATION_ID);
                    }
                })
                .toSortedList().toSingle();
    }
}
