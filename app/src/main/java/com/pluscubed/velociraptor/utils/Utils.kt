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
            val toneGen1 = ToneGenerator(AudioManager.STREAM_ALARM, 100)
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

    fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val len0 = lhs.length + 1
        val len1 = rhs.length + 1

        // the array of distances
        var cost = IntArray(len0)
        var newcost = IntArray(len0)

        // initial cost of skipping prefix in String s0
        for (i in 0 until len0) cost[i] = i

        // dynamically computing the array of distances

        // transformation cost for each letter in s1
        for (j in 1 until len1) {
            // initial cost of skipping prefix in String s1
            newcost[0] = j

            // transformation cost for each letter in s0
            for (i in 1 until len0) {
                // matching current letters in both strings
                val match = if (lhs[i - 1] == rhs[j - 1]) 0 else 1

                // computing cost for each transformation
                val cost_replace = cost[i - 1] + match
                val cost_insert = cost[i] + 1
                val cost_delete = newcost[i - 1] + 1

                // keep minimum cost
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace)
            }

            // swap cost/newcost arrays
            val swap = cost
            cost = newcost
            newcost = swap
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[len0 - 1]
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
