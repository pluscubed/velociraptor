package com.pluscubed.velociraptor.detection;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.crashlytics.android.Crashlytics;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.limit.LimitService;
import com.pluscubed.velociraptor.utils.PrefUtils;

import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class AppDetectionService extends AccessibilityService {

    public static final String GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps";
    public static final String GMAPS_BOTTOM_CONTAINER_ID = "com.google.android.apps.maps:id/bottommapoverlay_container";
    public static final String GMAPS_SPEED_LIMIT_TEXT = "SPEED LIMIT";
    private static AppDetectionService INSTANCE;

    private Set<String> enabledApps;
    private boolean isGmapsNavigating;

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
        this.isGmapsNavigating = gmapsNavigating;
        enableGoogleMapsMonitoring(true);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (enabledApps == null) {
            updateSelectedApps();
        }

        if (event.getPackageName() == null
                || event.getClassName() == null
                || (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && !event.getPackageName().equals(GOOGLE_MAPS_PACKAGE)
                || enabledApps == null)) {
            return;
        }

        Timber.d(event.toString());

        ComponentName componentName = new ComponentName(
                event.getPackageName().toString(),
                event.getClassName().toString()
        );

        boolean isActivity = componentName.getPackageName().toLowerCase().contains("activity")
                || tryGetActivity(componentName) != null;

        if (!isActivity && !componentName.getPackageName().equals(GOOGLE_MAPS_PACKAGE)) {
            return;
        }

        Intent intent = new Intent(this, LimitService.class);

        boolean shouldStartService = enabledApps.contains(componentName.getPackageName());

        if (componentName.getPackageName().equals(GOOGLE_MAPS_PACKAGE)) {
            if (PrefUtils.isGmapsOnlyInNavigation(this) && !isGmapsNavigating) {
                enableGoogleMapsMonitoring(false);
                intent.putExtra(LimitService.EXTRA_HIDE_LIMIT, false);

                shouldStartService = false;
            } else {
                enableGoogleMapsMonitoring(true);

                if (searchGmapsSpeedLimitSign(getRootInActiveWindow())) {
                    intent.putExtra(LimitService.EXTRA_HIDE_LIMIT, true);
                } else {
                    intent.putExtra(LimitService.EXTRA_HIDE_LIMIT, false);
                }
            }
        } else {
            enableGoogleMapsMonitoring(false);
            intent.putExtra(LimitService.EXTRA_HIDE_LIMIT, false);
        }


        if (!shouldStartService && !componentName.getPackageName().equals(BuildConfig.APPLICATION_ID)) {
            intent.putExtra(LimitService.EXTRA_CLOSE, true);
        }

        try {
            startService(intent);
        } catch (Exception e) {
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(e);
            }
        }
    }

    private void enableGoogleMapsMonitoring(boolean enable) {
        try {
            AccessibilityServiceInfo serviceInfo = getServiceInfo();
            if (serviceInfo != null) {
                if (enable) {
                    serviceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                    serviceInfo.notificationTimeout = 500;
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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean searchGmapsSpeedLimitSign(AccessibilityNodeInfo source) throws SecurityException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 || source == null) {
            return false;
        }

        List<AccessibilityNodeInfo> speedLimitNodes =
                source.findAccessibilityNodeInfosByViewId(GMAPS_BOTTOM_CONTAINER_ID);

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
            if (text.contains(GMAPS_SPEED_LIMIT_TEXT)) {
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