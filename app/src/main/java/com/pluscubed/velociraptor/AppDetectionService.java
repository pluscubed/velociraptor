package com.pluscubed.velociraptor;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

public class AppDetectionService extends AccessibilityService {

    private List<ResolveInfo> mMapApps;
    private long mLastMapAppQuery;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        queryMapApps();
    }

    private void queryMapApps() {
        Uri gmmIntentUri = Uri.parse("geo:37.421999,-122.084056");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        PackageManager manager = getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mMapApps = manager.queryIntentActivities(mapIntent, PackageManager.MATCH_ALL);
        } else {
            mMapApps = manager.queryIntentActivities(mapIntent, PackageManager.MATCH_DEFAULT_ONLY);
        }

        mLastMapAppQuery = System.currentTimeMillis();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (System.currentTimeMillis() - mLastMapAppQuery > 300000) {
            queryMapApps();
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null) {
                String appPackage = event.getPackageName().toString();
                if (event.getClassName() != null) {
                    String className = event.getClassName().toString();
                    if (className.toLowerCase().contains("activity") || className.toLowerCase().contains("launcher")) {
                        Intent intent = new Intent(this, FloatingService.class);

                        boolean isMapApp = false;
                        for (ResolveInfo info : mMapApps) {
                            if (info.activityInfo.packageName.equals(appPackage)) {
                                isMapApp = true;
                                break;
                            }
                        }

                        if (appPackage.equals(BuildConfig.APPLICATION_ID) || isMapApp) {
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