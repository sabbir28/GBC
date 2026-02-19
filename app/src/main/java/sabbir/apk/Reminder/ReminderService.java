package sabbir.apk.Reminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import sabbir.apk.R;

public class ReminderService extends Service {

    private static final String CHANNEL_ID = "class_reminders";
    private static final String CHANNEL_NAME = "Class Reminders";
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        String subject = intent.getStringExtra(ReminderReceiver.EXTRA_SUBJECT);
        String instructor = intent.getStringExtra(ReminderReceiver.EXTRA_INSTRUCTOR);
        String startTime = intent.getStringExtra(ReminderReceiver.EXTRA_START_TIME);

        createChannel();

        Notification notification = buildNotification(action, subject, instructor, startTime);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), notification);
        }

        // Log the reminder event in Firebase Analytics
        logReminderEvent(action, subject, instructor, startTime);

        stopSelf(startId);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification(String action, String subject, String instructor, String startTime) {
        boolean isAlarm = TextUtils.equals(action, ReminderReceiver.ACTION_FIRST_CLASS_ALARM);
        String title = isAlarm ? getString(R.string.reminder_alarm_title) : getString(R.string.reminder_title);

        String subjectLabel = TextUtils.isEmpty(subject)
                ? getString(R.string.reminder_subject_fallback)
                : subject;
        String instructorLabel = TextUtils.isEmpty(instructor)
                ? getString(R.string.reminder_instructor_fallback)
                : instructor;

        String timeLabel = getString(R.string.reminder_time_fallback);
        if (!TextUtils.isEmpty(startTime)) {
            try {
                timeLabel = LocalTime.parse(startTime).format(TIME_FORMATTER);
            } catch (Exception ignored) {
                timeLabel = startTime;
            }
        }

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_class_reminder);
        contentView.setTextViewText(R.id.tv_notification_title, title);
        contentView.setTextViewText(R.id.tv_notification_subject, subjectLabel);
        contentView.setTextViewText(R.id.tv_notification_instructor,
                getString(R.string.reminder_instructor_label, instructorLabel));
        contentView.setTextViewText(R.id.tv_notification_time,
                getString(R.string.reminder_time_label, timeLabel));

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_schedule_24)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(contentView)
                .setContentTitle(title)
                .setContentText(subjectLabel)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(isAlarm ? NotificationCompat.CATEGORY_ALARM : NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(getString(R.string.reminder_channel_description));
        channel.enableLights(true);
        channel.setLightColor(Color.parseColor("#F59E0B"));
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }

    private void logReminderEvent(String action, String subject, String instructor, String startTime) {
        if (mFirebaseAnalytics == null) return;

        Bundle bundle = new Bundle();
        bundle.putString("action", action);
        bundle.putString("subject", subject != null ? subject : "");
        bundle.putString("instructor", instructor != null ? instructor : "");
        bundle.putString("start_time", startTime != null ? startTime : "");
        mFirebaseAnalytics.logEvent("class_reminder_triggered", bundle);
    }
}
