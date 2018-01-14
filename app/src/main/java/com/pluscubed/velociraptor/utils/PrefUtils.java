package com.pluscubed.velociraptor.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public abstract class PrefUtils {

    public static final int STYLE_US = 0;
    public static final int STYLE_INTERNATIONAL = 1;
    private static final String PREF_METRIC = "pref_metric";
    private static final String PREF_SIGN_STYLE = "pref_international";
    private static final String PREF_FLOATING_LOCATION = "pref_floating_location";
    private static final String PREF_TOLERANCE_PERCENT = "pref_overspeed";
    private static final String PREF_TOLERANCE_CONSTANT = "pref_tolerance_constant";
    private static final String PREF_TOLERANCE_MODE = "pref_tolerance_mode";
    private static final String PREF_OPACITY = "pref_opacity";
    private static final String PREF_SPEEDOMETER = "pref_speedometer";
    private static final String PREF_LIMITS = "pref_limits";
    private static final String PREF_BEEP = "pref_beep";
    private static final String PREF_DEBUGGING = "pref_debugging";
    private static final String PREF_APPS = "pref_apps";
    private static final String PREF_FIRSTRUN = "pref_initial";
    private static final String PREF_VERSION_CODE = "pref_version_code";
    private static final String PREF_SPEEDLIMIT_SIZE = "pref_speedlimit_size";
    private static final String PREF_SPEEDOMETER_SIZE = "pref_speedometer_size";
    private static final String PREF_GMAPS_ONLY_NAVIGATION = "pref_gmaps_only_nav";
    private static final String PREF_TERMS_ACCEPTED = "pref_terms_accepted";

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

    public static void setTermsAccepted(Context context, boolean firstRun) {
        edit(context).putBoolean(PREF_TERMS_ACCEPTED, firstRun).apply();
    }

    public static boolean isTermsAccepted(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_TERMS_ACCEPTED, false);
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
        boolean metricDefault;
        Locale current = Locale.getDefault();
        if (current.equals(Locale.US) ||
                current.equals(Locale.UK) ||
                current.getISO3Country().equalsIgnoreCase("mmr")) {
            metricDefault = false;
        } else {
            metricDefault = true;
        }
        return getSharedPreferences(context).getBoolean(PREF_METRIC, metricDefault);
    }

    public static void setUseMetric(Context context, boolean metric) {
        edit(context).putBoolean(PREF_METRIC, metric).apply();
    }

    @SuppressWarnings("WrongConstant")
    @SignStyle
    public static int getSignStyle(Context context) {
        int styleDefault;
        Locale current = Locale.getDefault();
        if (current.equals(Locale.US) || current.equals(Locale.CANADA)) {
            styleDefault = STYLE_US;
        } else {
            styleDefault = STYLE_INTERNATIONAL;
        }

        return getSharedPreferences(context).getInt(PREF_SIGN_STYLE, styleDefault);
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

    public static boolean getShowLimits(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_LIMITS, true);
    }

    public static void setShowLimits(Context context, boolean show) {
        edit(context).putBoolean(PREF_LIMITS, show).apply();
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

    public static Set<String> getApps(Context context) {
        return new HashSet<>(getSharedPreferences(context).getStringSet(PREF_APPS, new HashSet<String>()));
    }

    public static void setApps(Context context, Set<String> packageNames) {
        edit(context).putStringSet(PREF_APPS, packageNames).apply();
    }

    public static float getSpeedometerSize(Context context) {
        return getSharedPreferences(context).getFloat(PREF_SPEEDOMETER_SIZE, 1f);
    }

    public static void setSpeedometerSize(Context context, float size) {
        edit(context).putFloat(PREF_SPEEDOMETER_SIZE, size).apply();
    }

    public static float getSpeedLimitSize(Context context) {
        return getSharedPreferences(context).getFloat(PREF_SPEEDLIMIT_SIZE, 1f);
    }

    public static void setSpeedLimitSize(Context context, float size) {
        edit(context).putFloat(PREF_SPEEDLIMIT_SIZE, size).apply();
    }

    public static boolean isGmapsOnlyInNavigation(Context context) {
        return getSharedPreferences(context).getBoolean(PREF_GMAPS_ONLY_NAVIGATION, false);
    }

    public static void setGmapsOnlyInNavigation(Context context, boolean only) {
        edit(context).putBoolean(PREF_GMAPS_ONLY_NAVIGATION, only).apply();
    }

    @IntDef({STYLE_US, STYLE_INTERNATIONAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SignStyle {
    }
}
