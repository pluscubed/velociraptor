package com.pluscubed.velociraptor.ui;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;

import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.utils.Utils;

public class AutoView implements LimitView {

    public static final int NOTIFICATION_AUTO = 42;
    public static final String NOTIFICATION_AUTO_TAG = "auto_tag";

    private LimitService service;

    private long autoTimestamp;
    private NotificationManagerCompat notificationManager;

    public AutoView(LimitService service) {
        this.service = service;
        notificationManager = NotificationManagerCompat.from(service);
    }

    @Override
    public void changeConfig() {

    }

    @Override
    public void setSpeed(int speed, int speedLimitWarning) {

    }

    @Override
    public void setSpeeding(boolean speeding) {

    }

    @Override
    public void setDebuggingText(String text) {

    }

    @Override
    public void setLimitText(String text) {
        String notificationMessage = service.getString(R.string.notif_android_auto_limit, Utils.getUnitText(service, text));

        if (autoTimestamp == 0) {
            autoTimestamp = System.currentTimeMillis();
        }

        RemoteInput input = new RemoteInput.Builder("key").build();
        PendingIntent emptyPendingIntent = PendingIntent.getActivity(service, 42, new Intent(), PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.CarExtender.UnreadConversation conv =
                new NotificationCompat.CarExtender.UnreadConversation.Builder(notificationMessage)
                        .setLatestTimestamp(autoTimestamp)
                        .setReadPendingIntent(emptyPendingIntent)
                        .setReplyAction(emptyPendingIntent, input)
                        .addMessage(notificationMessage)
                        .build();
        Notification notification = new NotificationCompat.Builder(service)
                .extend(new NotificationCompat.CarExtender().setUnreadConversation(conv))
                .setContentTitle(service.getString(R.string.notif_android_auto_title))
                .setContentText(notificationMessage)
                .setSmallIcon(R.drawable.ic_speedometer_notif)
                .build();
        notificationManager.notify(NOTIFICATION_AUTO_TAG, NOTIFICATION_AUTO, notification);
    }

    @Override
    public void updatePrefs() {

    }

    @Override
    public void hideLimit(boolean hideLimit) {

    }

    @Override
    public void stop() {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_AUTO_TAG, NOTIFICATION_AUTO);
        }
    }
}
