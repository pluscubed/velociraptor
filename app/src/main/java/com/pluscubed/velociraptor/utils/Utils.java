package com.pluscubed.velociraptor.utils;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.limit.LimitService;

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

    public static int convertDpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int convertMphToKmh(int speed) {
        return (int) (speed * 1.609344 + 0.5d);
    }

    public static int convertKmhToMph(int speed) {
        return (int) (speed / 1.609344 + 0.5d);
    }

    public static int compare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static void playBeeps() {
        playTone();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                playTone();
            }
        }, 300);
    }

    private static void playTone() {
        try {
            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100);
        } catch (RuntimeException ignored) {
        }
    }

    public static String getUnitText(Context context) {
        return getUnitText(context, "");
    }

    public static String getUnitText(Context context, String amount) {
        return PrefUtils.getUseMetric(context) ? context.getString(R.string.kmph, amount) : context.getString(R.string.mph, amount).trim();
    }

    public static void updateFloatingServicePrefs(Context context) {
        if (context != null && isServiceReady(context)) {
            Intent intent = new Intent(context, LimitService.class);
            intent.putExtra(LimitService.EXTRA_PREF_CHANGE, true);
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

    public static int levenshteinDistance(CharSequence lhs, CharSequence rhs) {
        int len0 = lhs.length() + 1;
        int len1 = rhs.length() + 1;

        // the array of distances
        int[] cost = new int[len0];
        int[] newcost = new int[len0];

        // initial cost of skipping prefix in String s0
        for (int i = 0; i < len0; i++) cost[i] = i;

        // dynamically computing the array of distances

        // transformation cost for each letter in s1
        for (int j = 1; j < len1; j++) {
            // initial cost of skipping prefix in String s1
            newcost[0] = j;

            // transformation cost for each letter in s0
            for (int i = 1; i < len0; i++) {
                // matching current letters in both strings
                int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;

                // computing cost for each transformation
                int cost_replace = cost[i - 1] + match;
                int cost_insert = cost[i] + 1;
                int cost_delete = newcost[i - 1] + 1;

                // keep minimum cost
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }

            // swap cost/newcost arrays
            int[] swap = cost;
            cost = newcost;
            newcost = swap;
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[len0 - 1];
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
