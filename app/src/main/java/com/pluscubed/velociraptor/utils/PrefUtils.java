package com.pluscubed.velociraptor.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class PrefUtils {

    public static final String PREF_FLOATING_LOCATION = "pref_floating_location";
    public static final String PREF_METRIC = "pref_metric";
    public static final String PREF_OVERSPEED = "pref_overspeed";
    public static final String PREF_SIGN_STYLE = "pref_international";
    public static final String PREF_SPEEDOMETER = "pref_speedometer";
    public static final String PREF_AUTO_DISPLAY = "pref_auto_display";
    public static final String PREF_BEEP = "pref_beep";
    public static final String PREF_DEBUGGING = "pref_debugging";

    public static final String PREF_FIRSTRUN = "pref_first";
    public static final String PREF_VERSION_CODE = "pref_version_code";

    public static final int STYLE_US = 0;
    public static final int STYLE_INTERNATIONAL = 1;

    private static SharedPreferences.Editor edit(Context context) {
        return getSharedPreferences(context).edit();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void setFirstRun(Context context, boolean firstRun) {
        edit(context).putBoolean(PREF_FIRSTRUN, firstRun).apply();
    }

    public static boolean isFirstRun(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_FIRSTRUN, true);
    }

    public static void setVersionCode(Context context, int versionCode) {
        edit(context).putInt(PREF_VERSION_CODE, versionCode).apply();
    }

    public static int getVersionCode(Context context) {
        return getSharedPreferences(context).getInt(PREF_VERSION_CODE, 0);
    }

    public static void setFloatingLocation(Context context, float screenYRatio, boolean left) {
        edit(context).putString(PREF_FLOATING_LOCATION, left + "," + screenYRatio).apply();
    }

    public static String getFloatingLocation(Context context) {
        return getSharedPreferences(context).getString(PREF_FLOATING_LOCATION, "true,0");
    }

    public static boolean getUseMetric(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_METRIC, false);
    }

    public static void setUseMetric(Context context, boolean metric) {
        edit(context).putBoolean(PREF_METRIC, metric).apply();
    }

    @SuppressWarnings("WrongConstant")
    @SignStyle
    public static int getSignStyle(Context context) {
        return getSharedPreferences(context).getInt(PREF_SIGN_STYLE, STYLE_US);
    }

    public static void setSignStyle(Context context, @SignStyle int style) {
        edit(context).putInt(PREF_SIGN_STYLE, style).apply();
    }

    public static int getOverspeedPercent(Context context) {
        return getSharedPreferences(context).getInt(PREF_OVERSPEED, 0);
    }

    public static void setOverspeedPercent(Context context, int amount) {
        edit(context).putInt(PREF_OVERSPEED, amount).apply();
    }

    public static boolean getShowSpeedometer(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_SPEEDOMETER, true);
    }

    public static void setShowSpeedometer(Context context, boolean show) {
        edit(context).putBoolean(PREF_SPEEDOMETER, show).apply();
    }

    public static boolean isAutoDisplayEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_AUTO_DISPLAY, true);
    }

    public static void setAutoDisplay(Context context, boolean autoDisplay) {
        edit(context).putBoolean(PREF_AUTO_DISPLAY, autoDisplay).apply();
    }

    public static boolean isDebuggingEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_DEBUGGING, false);
    }

    public static void setDebugging(Context context, boolean debugging) {
        edit(context).putBoolean(PREF_DEBUGGING, debugging).apply();
    }

    public static boolean isBeepAlertEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_BEEP, true);
    }

    public static void setBeepAlertEnabled(Context context, boolean beep) {
        edit(context).putBoolean(PREF_BEEP, beep).apply();
    }

    @IntDef({STYLE_US, STYLE_INTERNATIONAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SignStyle {
    }
}
