package sabbir.apk.UI;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.view.View;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
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

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
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
import sabbir.apk.InterNet.Deta.ScheduleItem;
import sabbir.apk.R;
import sabbir.apk.UI.Adapter.ClassAdapter;

public final class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    // Morning session time slots (you can later extract to config / resources)
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this); // modern edge-to-edge support
        setContentView(R.layout.activity_home);

        initViews();
        setupToolbarAndDrawer();
        setupRecyclerViews();

        validateTimeSlots();

        File routineFile = RoutineManagerApi.getRoutineFile(this);
        if (routineFile == null || !routineFile.exists() || !routineFile.canRead()) {
            showErrorState("Routine file not found or inaccessible");
            return;
        }

        loadRoutineAsync(routineFile);
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

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
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
            if (i > 0 && !SLOT_STARTS[i].equals(SLOT_ENDS[i - 1])) {
                Log.w(TAG, "Warning: non-contiguous slots at index " + i);
            }
        }
    }

    private void loadRoutineAsync(@NonNull File file) {
        ioExecutor.execute(() -> {
            try {
                String json = FileUtils.readAllText(file);
                JSONObject root = new JSONObject(json);

                mainHandler.post(() -> renderTodaySchedule(root));
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Failed to load routine", e);
                mainHandler.post(() -> showErrorState("Failed to load routine"));
            }
        });
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
                if (item.subject.equals("null"))
                {
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

            startUiTicker();
        } catch (JSONException e) {
            Log.e(TAG, "Invalid routine JSON structure", e);
            showErrorState("Invalid schedule format");
        }
    }

    // ──────────────────────────────────────────────
    //               Real-time ticker
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

        // Reverse past items so most recent is on top
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
        View pastHeader = findViewById(R.id.tv_previous_header);

        if (upcoming.isEmpty()) {
            rvUpcoming.setVisibility(View.GONE);
            upcomingHeader.setVisibility(View.GONE);
        } else {
            rvUpcoming.setVisibility(View.VISIBLE);
            upcomingHeader.setVisibility(View.VISIBLE);
            rvUpcoming.setAdapter(new ClassAdapter(upcoming, R.layout.item_class_upcoming));
        }

        if (past.isEmpty()) {
            rvPrevious.setVisibility(View.GONE);
            pastHeader.setVisibility(View.GONE);
        } else {
            rvPrevious.setVisibility(View.VISIBLE);
            pastHeader.setVisibility(View.VISIBLE);
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
            // already here → do nothing or refresh
        } else if (itemId == R.id.menu_schedule) {
            startActivity(new Intent(this, Schedule.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } else {
            startActivity(new Intent(this, Setting.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }
    }

    // Lifecycle

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
        ioExecutor.shutdownNow();
        stopUiTicker();
    }

    // Small utility class (you can move it elsewhere)
    private static class FileUtils {
        static String readAllText(File file) throws IOException {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()));
        }
    }
}