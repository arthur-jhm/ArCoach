package am.arthur.arcoach.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import am.arthur.arcoach.R;
import am.arthur.arcoach.activities.MainActivity;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";

    private static final String CHANNEL_ID = "workout_reminders";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        MyLog.d(TAG, "onReceive() called - Alarm triggered!");

        UserPreferences prefs = new UserPreferences(context);

        boolean notificationsEnabled = prefs.isNotificationsEnabled();
        boolean reminderEnabled = prefs.isReminderEnabled();

        MyLog.d(TAG, "Notifications enabled: " + notificationsEnabled);
        MyLog.d(TAG, "Reminder enabled: " + reminderEnabled);

        if (!notificationsEnabled || !reminderEnabled) {
            MyLog.d(TAG, "Reminders disabled, skipping notification");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                MyLog.e(TAG, "POST_NOTIFICATIONS permission not granted!");
                return;
            } else {
                MyLog.d(TAG, "POST_NOTIFICATIONS permission granted");
            }
        }

        createNotificationChannel(context);

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_text))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {

                notificationManager.notify(NOTIFICATION_ID, builder.build());
                MyLog.d(TAG, "Notification sent successfully!");

                if (!notificationManager.areNotificationsEnabled()) {
                    MyLog.e(TAG, "Notifications are disabled in system settings!");
                }
            } else {
                MyLog.e(TAG, "Cannot send notification - permission not granted!");
            }
        } catch (SecurityException e) {
            MyLog.e(TAG, "SecurityException when sending notification: " + e.getMessage());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_name);
            String description = context.getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                MyLog.d(TAG, "Notification channel created: " + CHANNEL_ID);

                NotificationChannel createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (createdChannel != null) {
                    MyLog.d(TAG, "Channel importance: " + createdChannel.getImportance());
                    MyLog.d(TAG, "Channel can show badge: " + createdChannel.canShowBadge());
                }
            }
        }
    }
}
