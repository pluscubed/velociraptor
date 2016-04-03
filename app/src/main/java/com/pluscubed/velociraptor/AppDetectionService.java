package com.pluscubed.velociraptor;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class AppDetectionService extends AccessibilityService {

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null) {
                String appPackage = event.getPackageName().toString();
                if (event.getClassName() != null) {
                    String className = event.getClassName().toString();
                    if (className.toLowerCase().contains("activity") || className.toLowerCase().contains("launcher")) {
                        Intent intent = new Intent(this, FloatingService.class);
                        if (appPackage.equals(BuildConfig.APPLICATION_ID) || appPackage.equals("com.google.android.apps.maps")) {
                            startService(intent);
                        } else {
                            stopService(intent);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}