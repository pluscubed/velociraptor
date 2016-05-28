package com.pluscubed.velociraptor;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

import com.pluscubed.velociraptor.utils.PrefUtils;

import java.util.Set;

public class AppDetectionService extends AccessibilityService {

    private static AppDetectionService sInstance;

    private Set<String> mEnabledApps;

    public static AppDetectionService get() {
        return sInstance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
    }

    public void updateSelectedApps() {
        mEnabledApps = PrefUtils.getApps(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!PrefUtils.isAutoDisplayEnabled(this)) {
            return;
        }

        if (mEnabledApps == null) {
            updateSelectedApps();
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getPackageName() != null
                && mEnabledApps != null) {
            String appPackage = event.getPackageName().toString();
            if (event.getClassName() != null) {
                String className = event.getClassName().toString();
                if (className.toLowerCase().contains("activity") || className.toLowerCase().contains("launcher")) {
                    Intent intent = new Intent(this, FloatingService.class);

                    boolean isSelectedApp = false;
                    for (String packageName : mEnabledApps) {
                        if (packageName.equals(appPackage)) {
                            isSelectedApp = true;
                            break;
                        }
                    }

                    if (className.equals("com.google.android.gms.car.CarHomeActivity")) {
                        isSelectedApp = true;
                        intent.putExtra(FloatingService.EXTRA_AUTO, true);
                    }

                    if (!isSelectedApp && !appPackage.equals(BuildConfig.APPLICATION_ID)) {
                        intent.putExtra(FloatingService.EXTRA_CLOSE, true);
                    }
                    startService(intent);
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}