package com.pluscubed.velociraptor

import android.app.Application
import com.bumptech.glide.Glide
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pluscubed.velociraptor.settings.appselection.AppInfo
import com.pluscubed.velociraptor.settings.appselection.AppInfoIconLoader
import com.pluscubed.velociraptor.settings.appselection.SelectedAppDatabase
import com.pluscubed.velociraptor.utils.PrefUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        FirebaseApp.initializeApp(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Glide.get(this)
                .register(AppInfo::class.java, InputStream::class.java, AppInfoIconLoader.Factory())

        if (PrefUtils.isAllMapApps(this)) {
            GlobalScope.launch {
                try {
                    val mapApps =
                            withContext(Dispatchers.IO) { SelectedAppDatabase.getMapApps(this@App) }
                                    .filter { appInfoEntity ->
                                        appInfoEntity.packageName != null && !appInfoEntity.packageName.isEmpty()
                                    }.map { appInfo -> appInfo.packageName }

                    PrefUtils.setApps(this@App, mapApps.toHashSet())
                } catch (e: Exception) {
                    e.printStackTrace()
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
    }
}
