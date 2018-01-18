package com.pluscubed.velociraptor.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.pluscubed.velociraptor.R;

public class NotificationUtils {

    public static final String CHANNEL_TOGGLES = "toggles";
    public static final String CHANNEL_RUNNING = "running";
    public static final String CHANNEL_WARNINGS = "warnings";

    public static void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_TOGGLES,
                context.getString(R.string.channel_toggles),
                NotificationManager.IMPORTANCE_LOW
        );
        notificationManager.createNotificationChannel(channel);

        channel = new NotificationChannel(
                CHANNEL_RUNNING,
                context.getString(R.string.notif_title),
                NotificationManager.IMPORTANCE_LOW
        );
        notificationManager.createNotificationChannel(channel);

        channel = new NotificationChannel(
                CHANNEL_WARNINGS,
                context.getString(R.string.channel_warnings),
                NotificationManager.IMPORTANCE_LOW
        );
        notificationManager.createNotificationChannel(channel);
    }
}
