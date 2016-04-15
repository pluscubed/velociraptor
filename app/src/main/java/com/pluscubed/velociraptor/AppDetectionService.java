package com.pluscubed.velociraptor;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

import com.pluscubed.velociraptor.appselection.AppInfo;
import com.pluscubed.velociraptor.appselection.AppInfoEntity;
import com.pluscubed.velociraptor.appselection.SelectedAppDatabase;
import com.pluscubed.velociraptor.utils.PrefUtils;

import java.util.List;

import rx.functions.Action1;

public class AppDetectionService extends AccessibilityService {

    private static AppDetectionService sInstance;

    private List<AppInfoEntity> mEnabledApps;

    public static AppDetectionService get() {
        return sInstance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;

        updateSelectedApps();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
    }

    public void updateSelectedApps() {
        SelectedAppDatabase.getSelectedApps(this)
                .subscribe(new Action1<List<AppInfoEntity>>() {
                    @Override
                    public void call(List<AppInfoEntity> appInfos) {
                        mEnabledApps = appInfos;
                    }
                });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!PrefUtils.isAutoDisplayEnabled(this)) {
            return;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null) {
                String appPackage = event.getPackageName().toString();
                if (event.getClassName() != null) {
                    String className = event.getClassName().toString();
                    if (className.toLowerCase().contains("activity") || className.toLowerCase().contains("launcher")) {
                        Intent intent = new Intent(this, FloatingService.class);

                        boolean isSelectedApp = false;
                        for (AppInfo info : mEnabledApps) {
                            if (info.packageName.equals(appPackage)) {
                                isSelectedApp = true;
                                break;
                            }
                        }

                        if (isSelectedApp) {
                            startService(intent);
                        } else if (!appPackage.equals(BuildConfig.APPLICATION_ID)) {
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