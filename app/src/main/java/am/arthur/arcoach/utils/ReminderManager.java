package am.arthur.arcoach.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class ReminderManager {
    private static final String TAG = "ReminderManager";

    private static final int REMINDER_REQUEST_CODE = 1001;

    /**
     * Установить ежедневное напоминание
     */
    public static void setDailyReminder(Context context, int hourOfDay, int minute) {
        MyLog.d(TAG, "Setting daily reminder for " + hourOfDay + ":" + minute);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            MyLog.e(TAG, "AlarmManager is null!");
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            MyLog.d(TAG, "Time has passed today, setting for tomorrow");
        }

        long triggerTime = calendar.getTimeInMillis();
        MyLog.d(TAG, "Alarm will trigger at: " + calendar.getTime());
        MyLog.d(TAG, "Time until alarm: " + (triggerTime - System.currentTimeMillis()) / 1000 + " seconds");

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                    MyLog.d(TAG, "Exact alarm set successfully (Android 12+)");
                } else {
                    MyLog.e(TAG, "Cannot schedule exact alarms! Permission required.");
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                    MyLog.d(TAG, "Inexact alarm set as fallback");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                MyLog.d(TAG, "Exact alarm set successfully (Android 6+)");
            } else {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
                MyLog.d(TAG, "Repeating alarm set successfully (Android 5)");
            }
        } catch (Exception e) {
            MyLog.e(TAG, "Error setting alarm: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Отменить напоминание
     */
    public static void cancelReminder(Context context) {
        MyLog.d(TAG, "Cancelling reminder");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            MyLog.d(TAG, "Reminder cancelled successfully");
        }
    }
}
