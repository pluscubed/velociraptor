package com.pluscubed.velociraptor.settings.appselection;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import com.pluscubed.velociraptor.BuildConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SelectedAppDatabase {

    /**
     * Returns list of map apps (packageName, id, name, enabled)
     */
    public static Single<List<AppInfo>> getMapApps(final Context context) {
        return Single.fromCallable(new Callable<List<AppInfo>>() {
            @Override
            public List<AppInfo> call() throws Exception {
                return getMapAppsSync(context);
            }
        }).subscribeOn(Schedulers.io())
                .flatMapObservable(new Func1<List<AppInfo>, Observable<AppInfo>>() {
                    @Override
                    public Observable<AppInfo> call(List<AppInfo> appInfos) {
                        return Observable.from(appInfos);
                    }
                }).toSortedList().toSingle();
    }

    /**
     * Returns list of map apps (packageName, name)
     */
    private static List<AppInfo> getMapAppsSync(Context context) {
        List<AppInfo> appInfos = new ArrayList<>();
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
            AppInfo appInfo = new AppInfo();
            appInfo.packageName = info.activityInfo.packageName;
            appInfo.name = info.loadLabel(context.getPackageManager()).toString();
            appInfos.add(appInfo);
        }
        return appInfos;
    }

    /**
     * Returns sorted list of AppInfos (packageName, name, id, enabled)
     */
    public static Single<List<AppInfo>> getInstalledApps(final Context context) {
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
                .map(new Func1<ApplicationInfo, AppInfo>() {
                    @Override
                    public AppInfo call(ApplicationInfo applicationInfo) {
                        AppInfo appInfo = new AppInfo();
                        appInfo.packageName = applicationInfo.packageName;
                        appInfo.name = applicationInfo.loadLabel(context.getPackageManager()).toString();
                        return appInfo;
                    }
                })
                .filter(new Func1<AppInfo, Boolean>() {
                    @Override
                    public Boolean call(AppInfo appInfoEntity) {
                        return !appInfoEntity.packageName.equals(BuildConfig.APPLICATION_ID);
                    }
                })
                .toSortedList().toSingle();
    }
}
