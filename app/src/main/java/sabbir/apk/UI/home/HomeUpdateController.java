package sabbir.apk.UI.home;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.time.Duration;
import java.time.Instant;

import sabbir.apk.InterNet.Deta.DownloadListener;
import sabbir.apk.InterNet.Deta.ReleaseAssetInfo;
import sabbir.apk.InterNet.Updater.DownloadService;
import sabbir.apk.InterNet.Updater.Updater;
import sabbir.apk.R;
import sabbir.apk.UI.HomeActivity;

public final class HomeUpdateController {

    public static final int REQUEST_INSTALL_PACKAGES_CODE = 9001;
    private static final String TAG = "HomeUpdateController";

    private final HomeActivity activity;
    private final UiHost host;

    private DownloadService downloadService;
    private boolean isBound;
    private Dialog updateDialog;
    private ReleaseAssetInfo pendingUpdateAsset;

    public interface UiHost {
        void requestUnknownSourcesPermission(int requestCode);
    }

    public HomeUpdateController(@NonNull HomeActivity activity, @NonNull UiHost host) {
        this.activity = activity;
        this.host = host;
    }

    public void checkForAppUpdate() {
        Updater.getInstalledApkSha256Async(activity, new Updater.Sha256Callback() {
            @Override
            public void onSuccess(String installedSha256) {
                Updater.fetchLatestApkAssetAsync(new Updater.ReleaseAssetCallback() {
                    @Override
                    public void onSuccess(ReleaseAssetInfo latest) {
                        if (!installedSha256.equals(latest.sha256)) {
                            Log.i(TAG, "Update available - SHA mismatch");
                            pendingUpdateAsset = latest;
                            activity.runOnUiThread(() -> showUpdateDialog(latest));
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.w(TAG, "Failed to fetch latest release", e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Cannot compute installed APK hash", e);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void showUpdateDialog(ReleaseAssetInfo latest) {
        if (updateDialog != null && updateDialog.isShowing()) {
            return;
        }

        updateDialog = new Dialog(activity);
        updateDialog.setContentView(R.layout.dialog_update);
        updateDialog.setCancelable(false);

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int width = (int) (dm.widthPixels * 0.92f);

        Window window = updateDialog.getWindow();
        if (window != null) {
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.45f);
        }

        ImageView ivIcon = updateDialog.findViewById(R.id.iv_update_icon);
        TextView tvInfo = updateDialog.findViewById(R.id.tv_update_info);
        Button btnLater = updateDialog.findViewById(R.id.btn_later);
        Button btnUpdate = updateDialog.findViewById(R.id.btn_update);
        Button btnBrowser = updateDialog.findViewById(R.id.btn_browser);

        if (ivIcon != null) {
            ivIcon.setVisibility(View.VISIBLE);
        }

        if (tvInfo == null || btnLater == null || btnUpdate == null) {
            Log.w(TAG, "Update dialog layout missing required views");
            return;
        }

        tvInfo.setText(
                "New version available!\n\n" +
                        "Updated: " + formatTimeSince(latest.updatedAt) + "\n" +
                        "Downloads: " + latest.downloadCount + "\n\n" +
                        "Choose how to update:"
        );

        btnLater.setOnClickListener(v -> {
            if (!Updater.canIgnoreUpdate(activity.getApplicationContext())) {
                Toast.makeText(activity, "Update is required to continue", Toast.LENGTH_LONG).show();
                return;
            }
            Updater.recordIgnore(activity.getApplicationContext());
            dismissDialog();
        });

        btnUpdate.setText("Download in App");
        btnUpdate.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && !activity.getPackageManager().canRequestPackageInstalls()) {
                host.requestUnknownSourcesPermission(REQUEST_INSTALL_PACKAGES_CODE);
                return;
            }
            startDownloadService();
            dismissDialog();
            Toast.makeText(activity, "Downloading update in background...", Toast.LENGTH_SHORT).show();
        });

        if (btnBrowser != null) {
            btnBrowser.setVisibility(View.VISIBLE);
            btnBrowser.setText("Open in Browser");
            btnBrowser.setOnClickListener(v -> openBrowserFallback());
        } else {
            Log.w(TAG, "btn_browser not found in layout → browser option disabled");
        }

        updateDialog.show();
    }

    public void onInstallPermissionResult() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (activity.getPackageManager().canRequestPackageInstalls()) {
                startDownloadService();
            } else {
                Toast.makeText(activity, "Permission denied. Cannot install updates.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onDestroy() {
        if (isBound && downloadService != null) {
            activity.unbindService(connection);
            isBound = false;
        }
        dismissDialog();
    }

    private void openBrowserFallback() {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sabbir28.github.io/app/"));
            activity.startActivity(browserIntent);
            dismissDialog();
            Toast.makeText(activity, "Opening download page...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(activity, "No browser found", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to open browser", e);
        }
    }

    private void startDownloadService() {
        if (pendingUpdateAsset == null) {
            Toast.makeText(activity, "No update asset available.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(activity, DownloadService.class);
        ContextCompat.startForegroundService(activity, intent);
        activity.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void installApk(File apkFile) {
        if (!apkFile.exists() || !apkFile.canRead()) {
            Toast.makeText(activity, "APK file not found:\n" + apkFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return;
        }

        Uri apkUri = Uri.fromFile(apkFile);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                apkUri = androidx.core.content.FileProvider.getUriForFile(
                        activity,
                        activity.getPackageName() + ".fileprovider",
                        apkFile
                );
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "FileProvider failed - check manifest & xml", e);
                Toast.makeText(activity, "Install setup error - contact developer", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Intent installIntent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            activity.startActivity(installIntent);
        } catch (Exception e) {
            Toast.makeText(activity, "Cannot start installer:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Install intent failed", e);
        }
    }

    private void dismissDialog() {
        if (updateDialog != null && updateDialog.isShowing()) {
            updateDialog.dismiss();
        }
        updateDialog = null;
    }

    private String formatTimeSince(String isoUtcTime) {
        try {
            Instant updated = Instant.parse(isoUtcTime);
            Instant now = Instant.now();
            long minutes = Duration.between(updated, now).toMinutes();

            if (minutes < 60) {
                return minutes + " min ago";
            }
            long hours = minutes / 60;
            if (hours < 24) {
                return hours + " hr ago";
            }
            long days = hours / 24;
            return days + " days ago";
        } catch (Exception e) {
            return "Unknown date";
        }
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            downloadService = ((DownloadService.LocalBinder) binder).getService();
            isBound = true;

            downloadService.setListener(new DownloadListener() {
                @Override
                public void onProgress(long downloaded, long total, int percent, long remaining) {
                    Log.d(TAG, "Download progress: " + percent + "%");
                }

                @Override
                public void onCompleted(File file) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Download completed. Installing...", Toast.LENGTH_LONG).show();
                        installApk(file);
                    });
                }

                @Override
                public void onError(Exception e) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Download error", e);
                    });
                }
            });

            downloadService.startDownload(
                    pendingUpdateAsset.downloadUrl,
                    "update-" + System.currentTimeMillis() + ".apk"
            );
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            downloadService = null;
            isBound = false;
        }
    };
}
