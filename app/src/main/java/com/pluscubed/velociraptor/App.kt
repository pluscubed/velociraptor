package com.pluscubed.velociraptor

import android.app.Application
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.pluscubed.velociraptor.settings.appselection.AppInfo
import com.pluscubed.velociraptor.settings.appselection.AppInfoIconLoader
import com.pluscubed.velociraptor.settings.appselection.SelectedAppDatabase
import com.pluscubed.velociraptor.utils.PrefUtils
import com.squareup.leakcanary.LeakCanary
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val crashlyticsKit = Crashlytics.Builder()
                .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build()
        Fabric.with(this, crashlyticsKit)

        FirebaseApp.initializeApp(this)
        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()
        FirebaseRemoteConfig.getInstance().setConfigSettings(configSettings)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        LeakCanary.install(this)

        Glide.get(this)
                .register(AppInfo::class.java, InputStream::class.java, AppInfoIconLoader.Factory())

        if (PrefUtils.isFirstRun(this)) {
            GlobalScope.launch {
                try {
                    val mapApps = withContext(Dispatchers.IO) { SelectedAppDatabase.getMapApps(this@App) }
                            .filter { appInfoEntity ->
                                appInfoEntity.packageName != null && !appInfoEntity.packageName.isEmpty()
                            }.map { appInfo -> appInfo.packageName }

                    PrefUtils.setApps(this@App, mapApps.toHashSet())
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (!BuildConfig.DEBUG)
                        Crashlytics.logException(e)
                }
            }
        }
    }
}
