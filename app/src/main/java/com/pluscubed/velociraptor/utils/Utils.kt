package com.pluscubed.velociraptor.utils

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.pluscubed.velociraptor.BuildConfig
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.limit.LimitService
import retrofit2.Response

object Utils {

    fun isAccessibilityServiceEnabled(
        context: Context,
        c: Class<out AccessibilityService>
    ): Boolean {
        var accessibilityEnabled = 0
        val service = BuildConfig.APPLICATION_ID + "/" + c.name
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }

        val splitter = TextUtils.SimpleStringSplitter(':')

        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                splitter.setString(settingValue)
                while (splitter.hasNext()) {
                    val accessibilityService = splitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }

        //Accessibility is disabled

        return false
    }

    fun convertDpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    fun convertMphToKmh(speed: Int): Int {
        return (speed * 1.609344 + 0.5).toInt()
    }

    fun convertKmhToMph(speed: Int): Int {
        return (speed / 1.609344 + 0.5).toInt()
    }

    fun compare(lhs: Int, rhs: Int): Int {
        return if (lhs < rhs) -1 else if (lhs == rhs) 0 else 1
    }

    fun playBeeps() {
        playTone()
        Handler().postDelayed({ playTone() }, 300)
    }

    private fun playTone() {
        try {
            val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100)
        } catch (ignored: RuntimeException) {
        }
    }

    @JvmOverloads
    fun getUnitText(context: Context, amount: String = ""): String {
        return if (PrefUtils.getUseMetric(context)) context.getString(
            R.string.kmph,
            amount
        ) else context.getString(R.string.mph, amount).trim { it <= ' ' }
    }

    fun updateFloatingServicePrefs(context: Context?) {
        if (context != null && isServiceReady(context)) {
            val intent = Intent(context, LimitService::class.java)
            intent.putExtra(LimitService.EXTRA_PREF_CHANGE, true)
            context.startService(intent)
        }
    }

    fun isServiceReady(context: Context): Boolean {
        val permissionGranted = isLocationPermissionGranted(context)
        @SuppressLint("NewApi", "LocalSuppress") val overlayEnabled =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        return permissionGranted && overlayEnabled
    }

    fun isLocationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

    fun <T : Any> getResponseBody(response: Response<T?>): T {
        if (!response.isSuccessful)
            throw Exception("Error code: ${response.code()}, ${response.errorBody()?.string()}")
        val body = response.body()
        if (body == null)
            throw Exception("Null response")
        return body
    }
}
