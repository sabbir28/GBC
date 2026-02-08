package sabbir.apk.UI;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInstaller;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sabbir.apk.InterNet.API.GitHub.RoutineManagerApi;
import sabbir.apk.InterNet.Deta.ClassState;
import sabbir.apk.InterNet.Deta.DownloadListener;
import sabbir.apk.InterNet.Deta.ReleaseAssetInfo;
import sabbir.apk.InterNet.Deta.ScheduleItem;
import sabbir.apk.InterNet.Updater.DownloadService;
import sabbir.apk.InterNet.Updater.Updater;
import sabbir.apk.R;
import sabbir.apk.Reminder.ReminderScheduler;
import sabbir.apk.UI.Adapter.ClassAdapter;

public final class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private static final int REQUEST_INSTALL_PACKAGES_CODE = 9001;

    private DownloadService downloadService;
    private boolean isBound = false;

    // Morning session time slots
    private static final LocalTime[] SLOT_STARTS = {
            LocalTime.of(9, 15),
            LocalTime.of(10, 0),
            LocalTime.of(10, 45),
            LocalTime.of(11, 30),
            LocalTime.of(12, 15)
    };

    private static final LocalTime[] SLOT_ENDS = {
            LocalTime.of(10, 0),
            LocalTime.of(10, 45),
            LocalTime.of(11, 30),
            LocalTime.of(12, 15),
            LocalTime.of(13, 0)
    };

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Handler uiTickHandler;
    private Runnable tickRunnable;

    private final List<ScheduleItem> todaySchedule = new ArrayList<>();

    private MaterialCardView cardCurrentClass;
    private RecyclerView rvUpcoming;
    private RecyclerView rvPrevious;
    private TextView tvCurrentSubject;
    private TextView tvCurrentInstructor;
    private TextView tvCurrentTime;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    private ReleaseAssetInfo pendingUpdateAsset = null;
    private Dialog updateDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupToolbarAndDrawer();
        setupRecyclerViews();

        validateTimeSlots();

        File routineFile = RoutineManagerApi.getRoutineFile(this);
        if (routineFile == null || !routineFile.exists() || !routineFile.canRead()) {
            showErrorState("Routine file not found or inaccessible");
        } else {
            loadRoutineAsync(routineFile);
        }

        // Check for update once on start
        checkForAppUpdate();
    }

    private void initViews() {
        cardCurrentClass    = findViewById(R.id.card_current_class);
        rvUpcoming          = findViewById(R.id.rv_upcoming);
        rvPrevious          = findViewById(R.id.rv_previous);
        tvCurrentSubject    = findViewById(R.id.tv_current_subject);
        tvCurrentInstructor = findViewById(R.id.tv_current_instructor);
        tvCurrentTime       = findViewById(R.id.tv_current_time);
        drawerLayout        = findViewById(R.id.drawer_layout);
        navigationView      = findViewById(R.id.navigation_view);
        toolbar             = findViewById(R.id.toolbar);
    }

    private void setupToolbarAndDrawer() {
        setSupportActionBar(toolbar);

        androidx.appcompat.app.ActionBarDrawerToggle toggle = new androidx.appcompat.app.ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close
        );

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START, true);
            handleNavigationItemClick(item.getItemId());
            return true;
        });
    }

    private void setupRecyclerViews() {
        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvPrevious.setLayoutManager(new LinearLayoutManager(this));
        rvUpcoming.setNestedScrollingEnabled(false);
        rvPrevious.setNestedScrollingEnabled(false);
    }

    private void validateTimeSlots() {
        if (SLOT_STARTS.length != SLOT_ENDS.length) {
            throw new IllegalStateException("Slot arrays length mismatch");
        }
        for (int i = 0; i < SLOT_STARTS.length; i++) {
            if (!SLOT_STARTS[i].isBefore(SLOT_ENDS[i])) {
                throw new IllegalStateException("Invalid slot: start ≥ end at index " + i);
            }
        }
    }

    private void loadRoutineAsync(@NonNull File file) {
        ioExecutor.execute(() -> {
            try {
                String json = readFileToString(file);
                JSONObject root = new JSONObject(json);
                mainHandler.post(() -> renderTodaySchedule(root));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load routine", e);
                mainHandler.post(() -> showErrorState("Failed to load routine"));
            }
        });
    }

    private String readFileToString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private void renderTodaySchedule(JSONObject root) {
        try {
            JSONObject scheduleObj = root.getJSONObject("schedule");
            DayOfWeek today = LocalDate.now().getDayOfWeek();
            JSONArray todayArray = scheduleObj.optJSONArray(today.name());

            if (todayArray == null || todayArray.length() == 0) {
                showEmptyState("No classes scheduled today");
                return;
            }

            todaySchedule.clear();

            int slotCount = Math.min(todayArray.length(), SLOT_STARTS.length);

            for (int i = 0; i < slotCount; i++) {
                JSONObject entry = todayArray.getJSONObject(i);

                ScheduleItem item = new ScheduleItem();
                item.subject    = entry.optString("subject_name", "Free Period");
                if ("null".equals(item.subject)) {
                    item.subject = "Free Period";
                }
                item.instructor = entry.optString("instructor_name", "—");
                item.start      = SLOT_STARTS[i];
                item.end        = SLOT_ENDS[i];
                todaySchedule.add(item);
            }

            if (todaySchedule.isEmpty()) {
                showEmptyState("No classes today");
                return;
            }

            ReminderScheduler.scheduleTodayReminders(this, todaySchedule, LocalDate.now());

            startUiTicker();
        } catch (JSONException e) {
            Log.e(TAG, "Invalid routine JSON structure", e);
            showErrorState("Invalid schedule format");
        }
    }

    // ──────────────────────────────────────────────
    //               Real-time UI ticker
    // ──────────────────────────────────────────────

    private void startUiTicker() {
        stopUiTicker();

        uiTickHandler = new Handler(Looper.getMainLooper());
        tickRunnable = new Runnable() {
            @Override
            public void run() {
                updateUiBasedOnCurrentTime();
                uiTickHandler.postDelayed(this, 1000);
            }
        };

        uiTickHandler.post(tickRunnable);
    }

    private void stopUiTicker() {
        if (uiTickHandler != null && tickRunnable != null) {
            uiTickHandler.removeCallbacks(tickRunnable);
        }
    }

    private void updateUiBasedOnCurrentTime() {
        LocalTime now = LocalTime.now();

        ScheduleItem current = null;
        List<ScheduleItem> upcoming = new ArrayList<>();
        List<ScheduleItem> past = new ArrayList<>();

        for (ScheduleItem item : todaySchedule) {
            if (now.isAfter(item.end)) {
                item.state = ClassState.PAST;
                past.add(item);
            } else if (now.isBefore(item.start)) {
                item.state = ClassState.UPCOMING;
                upcoming.add(item);
            } else {
                item.state = ClassState.CURRENT;
                current = item;
            }
        }

        if (current != null) {
            showCurrentClass(current, now);
        } else if (!upcoming.isEmpty()) {
            showNextClass(upcoming.get(0));
        } else {
            showDayFinished();
        }

        Collections.reverse(past);
        updateRecyclerViews(upcoming, past);
    }

    private void showCurrentClass(ScheduleItem item, LocalTime now) {
        cardCurrentClass.setVisibility(View.VISIBLE);
        long secondsRemaining = Duration.between(now, item.end).getSeconds();
        if (secondsRemaining < 0) secondsRemaining = 0;

        tvCurrentSubject.setText(item.subject);
        tvCurrentInstructor.setText(item.instructor);
        tvCurrentTime.setText(formatRemainingTime(secondsRemaining));
    }

    private void showNextClass(ScheduleItem next) {
        cardCurrentClass.setVisibility(View.VISIBLE);
        tvCurrentSubject.setText("Next: " + next.subject);
        tvCurrentInstructor.setText(next.instructor);
        tvCurrentTime.setText("Starts at " + next.start.format(TIME_FORMATTER));
    }

    private void showDayFinished() {
        cardCurrentClass.setVisibility(View.VISIBLE);
        tvCurrentSubject.setText("All classes completed");
        tvCurrentInstructor.setText("");
        tvCurrentTime.setText("Morning session finished");
    }

    private void showEmptyState(String message) {
        cardCurrentClass.setVisibility(View.VISIBLE);
        tvCurrentSubject.setText(message);
        tvCurrentInstructor.setText("");
        tvCurrentTime.setText("");
        updateRecyclerViews(new ArrayList<>(), new ArrayList<>());
    }

    private void showErrorState(String message) {
        showEmptyState(message);
    }

    private void updateRecyclerViews(List<ScheduleItem> upcoming, List<ScheduleItem> past) {
        View upcomingHeader = findViewById(R.id.tv_upcoming_header);
        View pastHeader     = findViewById(R.id.tv_previous_header);

        if (upcoming.isEmpty()) {
            rvUpcoming.setVisibility(View.GONE);
            if (upcomingHeader != null) upcomingHeader.setVisibility(View.GONE);
        } else {
            rvUpcoming.setVisibility(View.VISIBLE);
            if (upcomingHeader != null) upcomingHeader.setVisibility(View.VISIBLE);
            rvUpcoming.setAdapter(new ClassAdapter(upcoming, R.layout.item_class_upcoming));
        }

        if (past.isEmpty()) {
            rvPrevious.setVisibility(View.GONE);
            if (pastHeader != null) pastHeader.setVisibility(View.GONE);
        } else {
            rvPrevious.setVisibility(View.VISIBLE);
            if (pastHeader != null) pastHeader.setVisibility(View.VISIBLE);
            rvPrevious.setAdapter(new ClassAdapter(past, R.layout.item_class_previous));
        }
    }

    private String formatRemainingTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        if (h > 0) {
            return String.format(Locale.US, "Ends in %02d:%02d:%02d", h, m, s);
        } else if (m > 0) {
            return String.format(Locale.US, "Ends in %02d:%02d", m, s);
        } else {
            return String.format(Locale.US, "Ends in %02d sec", s);
        }
    }

    private void handleNavigationItemClick(int itemId) {
        if (itemId == R.id.menu_today) {
            // already here
        } else if (itemId == R.id.menu_schedule) {
            startActivity(new Intent(this, Schedule.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } else {
            startActivity(new Intent(this, Setting.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }
    }

    // ──────────────────────────────────────────────
    //               Self-update logic
    // ──────────────────────────────────────────────

    private void checkForAppUpdate() {
        Updater.getInstalledApkSha256Async(this, new Updater.Sha256Callback() {
            @Override
            public void onSuccess(String installedSha256) {
                Updater.fetchLatestApkAssetAsync(new Updater.ReleaseAssetCallback() {
                    @Override
                    public void onSuccess(ReleaseAssetInfo latest) {
                        if (!installedSha256.equals(latest.sha256)) {
                            Log.i(TAG, "Update available - SHA mismatch");
                            pendingUpdateAsset = latest;
                            runOnUiThread(() -> showUpdateDialog(latest));
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

        updateDialog = new Dialog(this);
        updateDialog.setContentView(R.layout.dialog_update);
        updateDialog.setCancelable(false);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = (int) (dm.widthPixels * 0.92f);

        Window window = updateDialog.getWindow();
        if (window != null) {
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.45f);
        }

        ImageView ivIcon      = updateDialog.findViewById(R.id.iv_update_icon);
        TextView tvInfo       = updateDialog.findViewById(R.id.tv_update_info);
        Button btnLater       = updateDialog.findViewById(R.id.btn_later);
        Button btnUpdate      = updateDialog.findViewById(R.id.btn_update);  // We'll repurpose this as "In App"

        // NEW: Add a second update button for browser
        Button btnBrowser     = updateDialog.findViewById(R.id.btn_browser); // ← you need to add this in layout

        if (tvInfo == null || btnLater == null || btnUpdate == null) {
            Log.w(TAG, "Update dialog layout missing required views");
            return;
        }

        // If you didn't add btn_browser yet, this will be null → handle gracefully or crash in dev
        boolean hasBrowserButton = (btnBrowser != null);

        tvInfo.setText(
                "New version available!\n\n" +
                        "Updated: " + formatTimeSince(latest.updatedAt) + "\n" +
                        "Downloads: " + latest.downloadCount + "\n\n" +
                        "Choose how to update:"
        );

        btnLater.setOnClickListener(v -> {
            if (!Updater.canIgnoreUpdate(getApplicationContext())) {
                Toast.makeText(this, "Update is required to continue", Toast.LENGTH_LONG).show();
                return;
            }
            Updater.recordIgnore(getApplicationContext());
            updateDialog.dismiss();
            updateDialog = null;
        });

        // Button 1: Download in App (original logic)
        btnUpdate.setText("Download in App");
        btnUpdate.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    startActivityForResult(
                            new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                    .setData(Uri.parse("package:" + getPackageName())),
                            REQUEST_INSTALL_PACKAGES_CODE);
                    return;
                }
            }

            startDownloadService();
            updateDialog.dismiss();
            updateDialog = null;
            Toast.makeText(this, "Downloading update in background...", Toast.LENGTH_SHORT).show();
        });

        // Button 2: Open in Browser
        if (hasBrowserButton) {
            btnBrowser.setVisibility(View.VISIBLE);
            btnBrowser.setText("Open in Browser");
            btnBrowser.setOnClickListener(v -> {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sabbir28.github.io/app/"));
                    startActivity(browserIntent);
                    updateDialog.dismiss();
                    updateDialog = null;
                    Toast.makeText(this, "Opening download page...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to open browser", e);
                }
            });
        } else {
            // If you didn't add the button yet, you can fallback or log
            Log.w(TAG, "btn_browser not found in layout → browser option disabled");
        }

        updateDialog.show();
    }

    private void startDownloadService() {
        Intent intent = new Intent(this, DownloadService.class);
        ContextCompat.startForegroundService(this, intent);

        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            downloadService = ((DownloadService.LocalBinder) binder).getService();
            isBound = true;

            downloadService.setListener(new DownloadListener() {
                @Override
                public void onProgress(long downloaded, long total, int percent, long remaining) {
                    // You can show progress in notification or UI if you want
                    Log.d(TAG, "Download progress: " + percent + "%");
                }

                @Override
                public void onCompleted(File file) {
                    runOnUiThread(() -> {
                        Toast.makeText(HomeActivity.this, "Download completed. Installing...", Toast.LENGTH_LONG).show();
                        installApk(file);
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(HomeActivity.this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Download error", e);
                    });
                }
            });

            downloadService.startDownload(pendingUpdateAsset.downloadUrl, "update-" + System.currentTimeMillis() + ".apk");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            downloadService = null;
            isBound = false;
        }
    };

    private void installApk(File apkFile) {
        if (!apkFile.exists() || !apkFile.canRead()) {
            Toast.makeText(this, "APK file not found:\n" + apkFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "APK ready:\n" + apkFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        Uri apkUri = Uri.fromFile(apkFile);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                apkUri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        apkFile
                );
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "FileProvider failed - check manifest & xml", e);
                Toast.makeText(this, "Install setup error - contact developer", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Intent installIntent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(installIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot start installer:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Install intent failed", e);
        }
    }

    private String formatTimeSince(String isoUtcTime) {
        try {
            Instant updated = Instant.parse(isoUtcTime);
            Instant now = Instant.now();
            long minutes = Duration.between(updated, now).toMinutes();

            if (minutes < 60) return minutes + " min ago";
            long hours = minutes / 60;
            if (hours < 24) return hours + " hr ago";
            long days = hours / 24;
            return days + " days ago";
        } catch (Exception e) {
            return "Unknown date";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INSTALL_PACKAGES_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (getPackageManager().canRequestPackageInstalls()) {
                    // Now we can install
                    startDownloadService();
                } else {
                    Toast.makeText(this, "Permission denied. Cannot install updates.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!todaySchedule.isEmpty()) {
            startUiTicker();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopUiTicker();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound && downloadService != null) {
            unbindService(connection);
            isBound = false;
        }
        if (updateDialog != null && updateDialog.isShowing()) {
            updateDialog.dismiss();
        }
        ioExecutor.shutdownNow();
        stopUiTicker();
    }
}