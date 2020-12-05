package com.pluscubed.velociraptor.detection

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils

import com.pluscubed.velociraptor.limit.LimitService
import com.pluscubed.velociraptor.utils.PrefUtils

class GmapsNavNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) {
            return
        }

        val packageName = sbn.packageName
        if (TextUtils.isEmpty(packageName)
                || packageName != AppDetectionService.GOOGLE_MAPS_PACKAGE
                || sbn.notification.priority != Notification.PRIORITY_MAX
                || !PrefUtils.isGmapsOnlyInNavigation(this)
        ) {
            return
        }

        AppDetectionService.get()?.setGmapsNavigating(true)

        val intent = Intent(this, LimitService::class.java)
        intent.putExtra(LimitService.EXTRA_NOTIF_START, true)
        startService(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) {
            return
        }

        val packageName = sbn.packageName
        if (TextUtils.isEmpty(packageName)
                || packageName != AppDetectionService.GOOGLE_MAPS_PACKAGE
                || sbn.notification.priority != Notification.PRIORITY_MAX
                || !PrefUtils.isGmapsOnlyInNavigation(this)
        ) {
            return
        }

        AppDetectionService.get()?.setGmapsNavigating(false)

        val intent = Intent(this, LimitService::class.java)
        intent.putExtra(LimitService.EXTRA_NOTIF_CLOSE, true)
        startService(intent)
    }

}