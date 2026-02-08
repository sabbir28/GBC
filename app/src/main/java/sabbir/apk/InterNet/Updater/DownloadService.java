package sabbir.apk.InterNet.Updater;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;


import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import sabbir.apk.InterNet.Deta.DownloadListener;

public final class DownloadService extends Service {

    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIFY_ID = 1001;

    private DownloadListener listener;

    public final class LocalBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setListener(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFY_ID, buildNotification(0));
    }

    @NonNull
    private Notification buildNotification(int percent) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading update")
                .setContentText(percent + "% completed")
                .setProgress(100, percent, false)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(int percent) {
        NotificationManagerCompat
                .from(this)
                .notify(NOTIFY_ID, buildNotification(percent));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "File Download",
                            NotificationManager.IMPORTANCE_LOW
                    );

            getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);
        }
    }

    public void startDownload(String url, String fileName) {

        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

                conn.connect();

                long totalBytes = conn.getContentLengthLong();

                File outFile = new File(getExternalFilesDir(null), fileName);

                try (
                        InputStream is = conn.getInputStream();
                        FileOutputStream fos = new FileOutputStream(outFile)
                ) {
                    byte[] buffer = new byte[8192];
                    long downloaded = 0;
                    int read;

                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        downloaded += read;

                        int percent =
                                (int) ((downloaded * 100) / totalBytes);

                        long remaining =
                                totalBytes - downloaded;

                        updateNotification(percent);

                        if (listener != null) {
                            listener.onProgress(
                                    downloaded,
                                    totalBytes,
                                    percent,
                                    remaining
                            );
                        }
                    }

                    if (listener != null) {
                        listener.onCompleted(outFile);
                    }

                    stopSelf();

                }

            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        }).start();
    }

}

