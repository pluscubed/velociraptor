package com.pluscubed.velociraptor.detection;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.util.Pair;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.pluscubed.velociraptor.BuildConfig;
import com.pluscubed.velociraptor.limit.LimitService;
import com.pluscubed.velociraptor.utils.LimitedSizeQueue;
import com.pluscubed.velociraptor.utils.PrefUtils;

import java.util.ListIterator;
import java.util.Set;

import timber.log.Timber;

public class AppDetectionService extends AccessibilityService {

    public static final String GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps";
    private static AppDetectionService INSTANCE;

    private Set<String> enabledApps;
    private boolean isGmapsNavigating;

    //time and whether opened
    private LimitedSizeQueue<Pair<Long, Boolean>> pastEvents;

    public static AppDetectionService get() {
        return INSTANCE;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        INSTANCE = this;
        isGmapsNavigating = false;
        pastEvents = new LimitedSizeQueue<>(5);
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
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null || event.getClassName() == null) {
            return;
        }


        if (BuildConfig.DEBUG)
            Timber.d(event.toString());

        ComponentName componentName = new ComponentName(
                event.getPackageName().toString(),
                event.getClassName().toString()
        );

        long eventTime = event.getEventTime();
        try {
            event.recycle();
        } catch (Exception ignored) {
        }

        boolean isActivity = componentName.getPackageName().toLowerCase().contains("activity")
                || tryGetActivity(componentName) != null;

        if (!isActivity && !componentName.getPackageName().equals(GOOGLE_MAPS_PACKAGE)) {
            return;
        }

        Intent intent = new Intent(this, LimitService.class);

        if (enabledApps == null) {
            updateSelectedApps();
        }
        boolean shouldStartService = enabledApps.contains(componentName.getPackageName());

        if (componentName.getPackageName().equals(GOOGLE_MAPS_PACKAGE) && PrefUtils.isGmapsOnlyInNavigation(this) && !isGmapsNavigating) {
            shouldStartService = false;
        }

        if (componentName.getPackageName().equals(BuildConfig.APPLICATION_ID)) {
            shouldStartService = true;
        }

        // Check 5 previous events - if within 200ms and launched the service, don't close
        ListIterator<Pair<Long, Boolean>> iterator = pastEvents.listIterator(pastEvents.size());
        while (iterator.hasPrevious()) {
            Pair<Long, Boolean> pastEvent = iterator.previous();
            if (pastEvent.second && Math.abs(eventTime - pastEvent.first) < 200) {
                shouldStartService = true;
            }
        }

        if (!shouldStartService) {
            intent.putExtra(LimitService.EXTRA_CLOSE, true);
        }
        pastEvents.add(new Pair<>(eventTime, shouldStartService));

        try {
            startService(intent);
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException ignore) {
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return null;
    }

    @Override
    public void onInterrupt() {
    }
}