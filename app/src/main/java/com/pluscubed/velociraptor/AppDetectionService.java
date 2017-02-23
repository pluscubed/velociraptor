package com.pluscubed.velociraptor;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.crashlytics.android.Crashlytics;
import com.pluscubed.velociraptor.ui.SpeedLimitService;
import com.pluscubed.velociraptor.utils.PrefUtils;

import java.util.List;
import java.util.Set;

public class AppDetectionService extends AccessibilityService {

    public static final String GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps";
    private static final String ANDROID_AUTO_ACTIVITY = "com.google.android.gms.car.CarHomeActivity";
    private static AppDetectionService INSTANCE;

    private Set<String> enabledApps;
    private boolean gmapsNavigating;

    private long lastTimestamp;

    public static AppDetectionService get() {
        return INSTANCE;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        INSTANCE = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        INSTANCE = null;
    }

    public void updateSelectedApps() {
        enabledApps = PrefUtils.getApps(this);
    }

    public void setGmapsNavigating(boolean gmapsNavigating) {
        this.gmapsNavigating = gmapsNavigating;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!PrefUtils.isAutoDisplayEnabled(this)) {
            return;
        }

        if (enabledApps == null) {
            updateSelectedApps();
        }

        if ((event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || System.currentTimeMillis() <= lastTimestamp + 2000))
                || event.getPackageName() == null
                || event.getClassName() == null
                || enabledApps == null) {
            return;
        }

        ComponentName componentName = new ComponentName(
                event.getPackageName().toString(),
                event.getClassName().toString()
        );

        ActivityInfo activityInfo = tryGetActivity(componentName);
        boolean isActivity = activityInfo != null;

        if (!isActivity && (!componentName.getPackageName().equals(GOOGLE_MAPS_PACKAGE))) {
            return;
        }

        Intent intent = new Intent(this, SpeedLimitService.class);

        boolean shouldStartService = enabledApps.contains(componentName.getPackageName());

        if (PrefUtils.isAutoDisplayEnabled(this)
                && componentName.getClassName().equals(ANDROID_AUTO_ACTIVITY)) {
            shouldStartService = true;
            intent.putExtra(SpeedLimitService.EXTRA_VIEW, SpeedLimitService.VIEW_AUTO);
        }

        if (componentName.getPackageName().equals(GOOGLE_MAPS_PACKAGE)) {
            if (PrefUtils.isGmapsOnlyInNavigation(this) && !gmapsNavigating) {
                enableGoogleMapsMonitoring(false);
                intent.putExtra(SpeedLimitService.EXTRA_HIDE_LIMIT, false);

                shouldStartService = false;
            } else {
                enableGoogleMapsMonitoring(true);

                lastTimestamp = System.currentTimeMillis();

                if (searchGmapsSpeedLimitSign(getRootInActiveWindow())) {
                    intent.putExtra(SpeedLimitService.EXTRA_HIDE_LIMIT, true);
                } else {
                    intent.putExtra(SpeedLimitService.EXTRA_HIDE_LIMIT, false);
                }
            }
        } else {
            enableGoogleMapsMonitoring(false);
            intent.putExtra(SpeedLimitService.EXTRA_HIDE_LIMIT, false);
        }


        if (!shouldStartService && !componentName.getPackageName().equals(BuildConfig.APPLICATION_ID)) {
            intent.putExtra(SpeedLimitService.EXTRA_CLOSE, true);
        }

        startService(intent);
    }

    private void enableGoogleMapsMonitoring(boolean enable) {
        try {
            AccessibilityServiceInfo serviceInfo = getServiceInfo();
            if (serviceInfo != null) {
                if (enable) {
                    serviceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                    serviceInfo.notificationTimeout = 2000;
                } else {
                    serviceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
                    serviceInfo.notificationTimeout = 0;
                }
                setServiceInfo(serviceInfo);
            }
        } catch (Exception e) {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(e);
            }
        }
    }

    private boolean searchGmapsSpeedLimitSign(AccessibilityNodeInfo source) throws SecurityException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 || source == null) {
            return false;
        }

        List<AccessibilityNodeInfo> speedLimitNodes =
                source.findAccessibilityNodeInfosByViewId("com.google.android.apps.maps:id/bottommapoverlay_container");

        source.recycle();

        for (AccessibilityNodeInfo info : speedLimitNodes) {
            if (info.isVisibleToUser()) {
                return searchSpeedLimitText(info, 0);
            }
        }

        return false;
    }

    private boolean searchSpeedLimitText(AccessibilityNodeInfo source, int depth) throws SecurityException {
        if (depth > 10 || source == null) {
            return false;
        }

        if (source.getText() != null) {
            String text = source.getText().toString();
            if (text.contains("SPEED LIMIT")) {
                return true;
            }
        }

        for (int i = 0; i < source.getChildCount(); i++) {
            if (searchSpeedLimitText(source.getChild(i), depth + 1)) {
                return true;
            }
        }

        source.recycle();

        return false;
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void onInterrupt() {
    }
}