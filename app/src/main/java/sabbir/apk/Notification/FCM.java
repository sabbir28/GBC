package sabbir.apk.Notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


import sabbir.apk.MainActivity;
import sabbir.apk.R;

import java.util.Map;

import sabbir.apk.Analytics.FirebaseEventLogger;

public class FCM extends FirebaseMessagingService {

    private static final String TAG = "FCM";
    private static final String CHANNEL_ID = "default_channel";

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseEventLogger eventLogger;

    @Override
    public void onCreate() {
        super.onCreate();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        eventLogger = new FirebaseEventLogger(mFirebaseAnalytics);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        logTokenGenerated(token);
        sendRegistrationToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Handle data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleData(remoteMessage.getData());

            if (remoteMessage.getNotification() == null) {
                String fallbackTitle = remoteMessage.getData().get("title");
                String fallbackBody = remoteMessage.getData().get("body");
                if (fallbackTitle != null || fallbackBody != null) {
                    sendNotification(
                            fallbackTitle != null ? fallbackTitle : "GBC Message",
                            fallbackBody != null ? fallbackBody : "Open app for details"
                    );
                }
            }
        }

        // Handle notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Notification Title: " + title);
            Log.d(TAG, "Notification Body: " + body);

            logNotificationReceived(title, body);
            sendNotification(title, body);
            FirebaseInAppMessaging.getInstance().triggerEvent("notification_received");
        }
    }

    private void handleData(Map<String, String> data) {
        if (data.containsKey("action")) {
            String action = data.get("action");
            Log.d(TAG, "Action received: " + action);
            logDataPayload(action, data);
        }

        if ("true".equals(data.get("long_job"))) {
            scheduleJob(data);
        } else {
            handleNow(data);
        }
    }

    private void sendRegistrationToServer(String token) {
        Log.d(TAG, "Send token to server: " + token);
        // TODO: Replace with actual backend API call
    }

    private void scheduleJob(Map<String, String> data) {
        Log.d(TAG, "Scheduling long-running job with data: " + data.toString());
        // TODO: Implement WorkManager job
    }

    private void handleNow(Map<String, String> data) {
        Log.d(TAG, "Handling short task now: " + data.toString());
    }

    private void sendNotification(String title, String body) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.bm_college_logo)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Default Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for default notifications");
            notificationManager.createNotificationChannel(channel);
        }

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        }
    }

    // ------------------- Firebase Analytics Logging -------------------

    private void logTokenGenerated(String token) {
        if (eventLogger == null) return;
        eventLogger.logStringEvent("fcm_token_generated", "fcm_token", token);
    }

    private void logNotificationReceived(String title, String body) {
        if (eventLogger == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("title", title != null ? title : "");
        bundle.putString("body", body != null ? body : "");
        mFirebaseAnalytics.logEvent("notification_received", bundle);
    }

    private void logDataPayload(String action, Map<String, String> data) {
        if (eventLogger == null) return;
        eventLogger.logMapEvent("data_payload_received", data);
        eventLogger.logStringEvent("data_payload_action", "action", action);
    }
}
