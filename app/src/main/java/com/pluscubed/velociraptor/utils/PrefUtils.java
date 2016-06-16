package com.pluscubed.velociraptor.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;

public abstract class PrefUtils {

    public static final int STYLE_US = 0;
    public static final int STYLE_INTERNATIONAL = 1;
    public static final String PREF_METRIC = "pref_metric";
    public static final String PREF_SIGN_STYLE = "pref_international";
    private static final String PREF_FLOATING_LOCATION = "pref_floating_location";
    private static final String PREF_TOLERANCE_PERCENT = "pref_overspeed";
    private static final String PREF_TOLERANCE_CONSTANT = "pref_tolerance_constant";
    private static final String PREF_TOLERANCE_MODE = "pref_tolerance_mode";
    private static final String PREF_OPACITY = "pref_opacity";
    private static final String PREF_SPEEDOMETER = "pref_speedometer";
    private static final String PREF_AUTO_DISPLAY = "pref_auto_display";
    private static final String PREF_BEEP = "pref_beep";
    private static final String PREF_DEBUGGING = "pref_debugging";
    private static final String PREF_SUPPORTED = "pref_supported";
    private static final String PREF_APPS = "pref_apps";
    private static final String PREF_ANDROID_AUTO = "pref_android_auto";
    private static final String PREF_FIRSTRUN = "pref_initial";
    private static final String PREF_VERSION_CODE = "pref_version_code";

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

    public static int getSpeedingPercent(Context context) {
        return getSharedPreferences(context).getInt(PREF_TOLERANCE_PERCENT, 0);
    }

    public static void setSpeedingPercent(Context context, int amount) {
        edit(context).putInt(PREF_TOLERANCE_PERCENT, amount).apply();
    }

    public static int getSpeedingConstant(Context context) {
        return getSharedPreferences(context).getInt(PREF_TOLERANCE_CONSTANT, 0);
    }

    public static void setOpacity(Context context, int amount) {
        edit(context).putInt(PREF_OPACITY, amount).apply();
    }

    public static int getOpacity(Context context) {
        return getSharedPreferences(context).getInt(PREF_OPACITY, 100);
    }

    public static void setSpeedingConstant(Context context, int amount) {
        edit(context).putInt(PREF_TOLERANCE_CONSTANT, amount).apply();
    }

    public static boolean getToleranceMode(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_TOLERANCE_MODE, true);
    }

    public static void setToleranceMode(Context context, boolean and) {
        edit(context).putBoolean(PREF_TOLERANCE_MODE, and).apply();
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

    public static boolean hasSupported(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_SUPPORTED, false);
    }

    public static void setSupported(Context context, boolean beep) {
        edit(context).putBoolean(PREF_SUPPORTED, beep).apply();
    }

    public static boolean isAutoIntegrationEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_ANDROID_AUTO, false);
    }

    public static void setAutoIntegrationEnabled(Context context, boolean enabled) {
        edit(context).putBoolean(PREF_ANDROID_AUTO, enabled).apply();
    }

    public static Set<String> getApps(Context context) {
        return new HashSet<>(getSharedPreferences(context).getStringSet(PREF_APPS, new HashSet<String>()));
    }

    public static void setApps(Context context, Set<String> packageNames) {
        edit(context).putStringSet(PREF_APPS, packageNames).apply();
    }

    @IntDef({STYLE_US, STYLE_INTERNATIONAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SignStyle {
    }
}
