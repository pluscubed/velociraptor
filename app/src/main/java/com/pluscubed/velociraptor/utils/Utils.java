package com.pluscubed.velociraptor.utils;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.SpeedLimitService;

public abstract class Utils {

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> c) {
        int accessibilityEnabled = 0;
        final String service = BuildConfig.APPLICATION_ID + "/" + c.getName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(), android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessibilityService = splitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }

        //Accessibility is disabled

        return false;
    }

    public static Drawable getVectorDrawableCompat(Context context, @DrawableRes int drawable) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                ContextCompat.getDrawable(context, drawable) :
                VectorDrawableCompat.create(context.getResources(), drawable, context.getTheme());
    }

    public static int convertDpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int compare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static void playBeep() {
        try {
            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP2, 500);
        } catch (RuntimeException ignored) {
            //TODO: Investigate RuntimeException
        }
    }

    public static String getUnitText(Context context) {
        return getUnitText(context, "");
    }

    public static String getUnitText(Context context, String amount) {
        return PrefUtils.getUseMetric(context) ? context.getString(R.string.kmph, amount) : context.getString(R.string.mph, amount).trim();
    }

    public static void updateFloatingServicePrefs(Context context) {
        if (isServiceReady(context)) {
            Intent intent = new Intent(context, SpeedLimitService.class);
            intent.putExtra(SpeedLimitService.EXTRA_PREF_CHANGE, true);
            context.startService(intent);
        }
    }

    public static boolean isServiceReady(Context context) {
        boolean permissionGranted =
                isLocationPermissionGranted(context);
        @SuppressLint({"NewApi", "LocalSuppress"}) boolean overlayEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
        return permissionGranted && overlayEnabled;
    }

    public static boolean isLocationPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
}
