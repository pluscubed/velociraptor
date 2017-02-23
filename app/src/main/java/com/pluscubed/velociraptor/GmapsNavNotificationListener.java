package com.pluscubed.velociraptor;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.pluscubed.velociraptor.ui.SpeedLimitService;
import com.pluscubed.velociraptor.utils.PrefUtils;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GmapsNavNotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        final String packageName = sbn.getPackageName();
        if (!TextUtils.isEmpty(packageName) && packageName.equals(AppDetectionService.GOOGLE_MAPS_PACKAGE)
                && sbn.getNotification().priority == Notification.PRIORITY_MAX
                && AppDetectionService.get() != null && PrefUtils.isGmapsOnlyInNavigation(this)
                && PrefUtils.isAutoDisplayEnabled(this)) {
            AppDetectionService.get().setGmapsNavigating(true);

            Intent intent = new Intent(this, SpeedLimitService.class);
            intent.putExtra(SpeedLimitService.EXTRA_NOTIF_START, true);
            startService(intent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        final String packageName = sbn.getPackageName();
        if (!TextUtils.isEmpty(packageName) && packageName.equals(AppDetectionService.GOOGLE_MAPS_PACKAGE)
                && sbn.getNotification().priority == Notification.PRIORITY_MAX
                && AppDetectionService.get() != null && PrefUtils.isGmapsOnlyInNavigation(this)
                && PrefUtils.isAutoDisplayEnabled(this)) {
            AppDetectionService.get().setGmapsNavigating(false);

            Intent intent = new Intent(this, SpeedLimitService.class);
            intent.putExtra(SpeedLimitService.EXTRA_NOTIF_CLOSE, true);
            startService(intent);
        }
    }

}