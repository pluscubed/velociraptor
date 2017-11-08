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

import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

public class SelectedAppDatabase {

    /**
     * Returns list of map apps (packageName, id, name, enabled)
     */
    public static Single<List<AppInfo>> getMapApps(final Context context) {
        return Single.fromCallable(() -> getMapAppsSync(context)).subscribeOn(Schedulers.io())
                .flatMapObservable(appInfos -> Observable.from(appInfos)).toSortedList().toSingle();
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
        return Single.create((Single.OnSubscribe<List<ApplicationInfo>>) singleSubscriber -> singleSubscriber.onSuccess(context.getPackageManager().getInstalledApplications(0))).subscribeOn(Schedulers.io())
                .flatMapObservable(appInfos -> Observable.from(appInfos))
                .map(applicationInfo -> {
                    AppInfo appInfo = new AppInfo();
                    appInfo.packageName = applicationInfo.packageName;
                    appInfo.name = applicationInfo.loadLabel(context.getPackageManager()).toString();
                    return appInfo;
                })
                .filter(appInfoEntity -> !appInfoEntity.packageName.equals(BuildConfig.APPLICATION_ID))
                .toSortedList().toSingle();
    }
}
