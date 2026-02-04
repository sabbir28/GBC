package sabbir.apk.Reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import sabbir.apk.InterNet.Deta.ScheduleItem;

public final class ReminderScheduler {

    private static final int REMINDER_OFFSET_MINUTES = 10;
    private static final int FIRST_CLASS_ALARM_MINUTES = 20;

    private ReminderScheduler() {
        // Utility class.
    }

    public static void scheduleTodayReminders(Context context, List<ScheduleItem> items, LocalDate date) {
        SharedPreferences prefs =
                context.getSharedPreferences(ReminderPreferences.PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled =
                prefs.getBoolean(ReminderPreferences.KEY_NOTIFICATIONS, true);
        boolean classRemindersEnabled =
                prefs.getBoolean(ReminderPreferences.KEY_CLASS_REMINDERS, true);
        boolean morningAlarmEnabled =
                prefs.getBoolean(ReminderPreferences.KEY_MORNING_ALARM, true);

        if (!notificationsEnabled) {
            cancelTodayReminders(context, items, date);
            return;
        }

        if (items == null || items.isEmpty()) {
            return;
        }

        if (morningAlarmEnabled) {
            LocalTime firstClassStart = items.get(0).start;
            scheduleAlarm(
                    context,
                    date,
                    firstClassStart.minusMinutes(FIRST_CLASS_ALARM_MINUTES),
                    ReminderReceiver.ACTION_FIRST_CLASS_ALARM,
                    buildRequestCode(date, -1)
            );
        }

        if (classRemindersEnabled) {
            for (int i = 0; i < items.size(); i++) {
                ScheduleItem item = items.get(i);
                LocalTime reminderTime = item.start.minusMinutes(REMINDER_OFFSET_MINUTES);
                scheduleAlarm(
                        context,
                        date,
                        reminderTime,
                        ReminderReceiver.ACTION_CLASS_REMINDER,
                        buildRequestCode(date, i),
                        item
                );
            }
        }
    }

    private static void scheduleAlarm(
            Context context,
            LocalDate date,
            LocalTime time,
            String action,
            int requestCode
    ) {
        scheduleAlarm(context, date, time, action, requestCode, null);
    }

    private static void scheduleAlarm(
            Context context,
            LocalDate date,
            LocalTime time,
            String action,
            int requestCode,
            ScheduleItem item
    ) {
        LocalDateTime target = LocalDateTime.of(date, time);
        long triggerAtMillis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long nowMillis = System.currentTimeMillis();
        if (triggerAtMillis <= nowMillis) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(action);
        if (item != null) {
            intent.putExtra(ReminderReceiver.EXTRA_SUBJECT, item.subject);
            intent.putExtra(ReminderReceiver.EXTRA_INSTRUCTOR, item.instructor);
            intent.putExtra(ReminderReceiver.EXTRA_START_TIME, item.start.toString());
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    private static void cancelTodayReminders(Context context, List<ScheduleItem> items, LocalDate date) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null || items == null) {
            return;
        }

        for (int i = -1; i < items.size(); i++) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    buildRequestCode(date, i),
                    new Intent(context, ReminderReceiver.class),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    private static int buildRequestCode(LocalDate date, int index) {
        return date.getDayOfYear() * 100 + (index + 1);
    }
}
