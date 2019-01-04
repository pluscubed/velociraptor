package com.pluscubed.velociraptor.detection;

import android.app.Notification;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.pluscubed.velociraptor.limit.LimitService;
import com.pluscubed.velociraptor.utils.PrefUtils;

public class GmapsNavNotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) {
            return;
        }

        final String packageName = sbn.getPackageName();
        if (TextUtils.isEmpty(packageName)
                || !packageName.equals(AppDetectionService.GOOGLE_MAPS_PACKAGE)
                || sbn.getNotification().priority != Notification.PRIORITY_MAX
                || AppDetectionService.get() == null
                || !PrefUtils.isGmapsOnlyInNavigation(this)) {
            return;
        }

        AppDetectionService.get().setGmapsNavigating(true);

        Intent intent = new Intent(this, LimitService.class);
        intent.putExtra(LimitService.EXTRA_NOTIF_START, true);
        startService(intent);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) {
            return;
        }

        final String packageName = sbn.getPackageName();
        if (TextUtils.isEmpty(packageName)
                || !packageName.equals(AppDetectionService.GOOGLE_MAPS_PACKAGE)
                || sbn.getNotification().priority != Notification.PRIORITY_MAX
                || AppDetectionService.get() == null
                || !PrefUtils.isGmapsOnlyInNavigation(this)) {
            return;
        }

        AppDetectionService.get().setGmapsNavigating(false);

        Intent intent = new Intent(this, LimitService.class);
        intent.putExtra(LimitService.EXTRA_NOTIF_CLOSE, true);
        startService(intent);
    }

}