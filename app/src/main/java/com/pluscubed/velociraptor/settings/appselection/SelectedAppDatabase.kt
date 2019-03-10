package com.pluscubed.velociraptor.settings.appselection

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import com.pluscubed.velociraptor.BuildConfig
import java.util.*

object SelectedAppDatabase {

    /**
     * Returns sorted list of map apps (packageName, name)
     */
    fun getMapApps(context: Context): List<AppInfo> {
        return getMapAppsSync(context).sorted()
    }

    /**
     * Returns list of map apps (packageName, name)
     */
    private fun getMapAppsSync(context: Context): List<AppInfo> {
        val appInfos = ArrayList<AppInfo>()
        val gmmIntentUri = Uri.parse("geo:37.421999,-122.084056")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        val manager = context.packageManager
        val mapApps: List<ResolveInfo>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mapApps = manager.queryIntentActivities(mapIntent, PackageManager.MATCH_ALL)
        } else {
            mapApps = manager.queryIntentActivities(mapIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }

        for (info in mapApps) {
            val appInfo = AppInfo()
            appInfo.packageName = info.activityInfo.packageName
            appInfo.name = info.loadLabel(context.packageManager).toString()
            appInfos.add(appInfo)
        }
        return appInfos
    }

    /**
     * Returns sorted list of AppInfos (packageName, name)
     */
    fun getInstalledApps(context: Context): List<AppInfo> {
        return context.packageManager.getInstalledApplications(0)
            .map { applicationInfo ->
                val appInfo = AppInfo()
                appInfo.packageName = applicationInfo.packageName
                appInfo.name = applicationInfo.loadLabel(context.packageManager).toString()
                appInfo
            }
            .filter { appInfoEntity: AppInfo ->
                appInfoEntity.packageName != BuildConfig.APPLICATION_ID
            }
            .sorted()
    }
}
