package sabbir.apk.UI;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sabbir.apk.InterNet.API.GitHub.RoutineManagerApi;
import sabbir.apk.InterNet.Deta.ClassState;
import sabbir.apk.InterNet.Deta.ScheduleItem;
import sabbir.apk.R;
import sabbir.apk.UI.Adapter.ClassAdapter;

public final class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    /* ===== MORNING SESSION (09:15 → 13:00) ===== */
    private static final LocalTime[] SLOT_START = {
            LocalTime.of(9, 15),
            LocalTime.of(10, 0),
            LocalTime.of(10, 45),
            LocalTime.of(11, 30),
            LocalTime.of(12, 15)
    };

    private static final LocalTime[] SLOT_END = {
            LocalTime.of(10, 0),
            LocalTime.of(10, 45),
            LocalTime.of(11, 30),
            LocalTime.of(12, 15),
            LocalTime.of(13, 0)
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler tickHandler = new Handler(Looper.getMainLooper());

    private Runnable tickRunnable;
    private final List<ScheduleItem> cachedItems = new ArrayList<>();

    private MaterialCardView cardCurrentClass;
    private RecyclerView rvUpcoming;
    private RecyclerView rvPrevious;
    private TextView tvCurrentSubject;
    private TextView tvCurrentInstructor;
    private TextView tvCurrentTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        cardCurrentClass = findViewById(R.id.card_current_class);
        rvUpcoming = findViewById(R.id.rv_upcoming);
        rvPrevious = findViewById(R.id.rv_previous);
        tvCurrentSubject = findViewById(R.id.tv_current_subject);
        tvCurrentInstructor = findViewById(R.id.tv_current_instructor);
        tvCurrentTime = findViewById(R.id.tv_current_time);

        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvPrevious.setLayoutManager(new LinearLayoutManager(this));

        validateSlots();

        File routineFile = RoutineManagerApi.getRoutineFile(this);
        if (routineFile == null || !routineFile.exists()) {
            Log.e(TAG, "Routine file missing");
            showEmptyState("Routine not available");
            return;
        }

        loadRoutineAsync(routineFile);
    }

    /* ===== SLOT INTEGRITY CHECK ===== */
    private void validateSlots() {
        for (int i = 0; i < SLOT_START.length; i++) {
            if (!SLOT_START[i].isBefore(SLOT_END[i])) {
                throw new IllegalStateException("Invalid slot range at index " + i);
            }
            if (i > 0 && !SLOT_START[i].equals(SLOT_END[i - 1])) {
                Log.w(TAG, "Non-contiguous slot at index " + i);
            }
        }
    }

    private void loadRoutineAsync(File file) {
        executor.execute(() -> {
            try {
                JSONObject root = new JSONObject(readFile(file));
                mainHandler.post(() -> renderToday(root));
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Routine load failure", e);
            }
        });
    }

    private void renderToday(JSONObject root) {
        try {
            JSONObject schedule = root.getJSONObject("schedule");

            DayOfWeek today = LocalDate.now().getDayOfWeek();
            JSONArray todayArray = schedule.optJSONArray(today.name());

            if (todayArray == null) {
                showEmptyState("No classes today");
                return;
            }

            cachedItems.clear();

            for (int i = 0; i < todayArray.length() && i < SLOT_START.length; i++) {
                JSONObject obj = todayArray.getJSONObject(i);

                ScheduleItem item = new ScheduleItem();
                item.subject = obj.optString("subject_name", "Free Period");
                item.instructor = obj.optString("instructor_name", "-");
                item.start = SLOT_START[i];
                item.end = SLOT_END[i];

                cachedItems.add(item);
            }

            startTicker();

        } catch (JSONException e) {
            Log.e(TAG, "Invalid JSON structure", e);
        }
    }

    /* ===== REAL-TIME ENGINE ===== */
    private void startTicker() {
        stopTicker();

        tickRunnable = new Runnable() {
            @Override
            public void run() {
                evaluateNow();
                tickHandler.postDelayed(this, 1000);
            }
        };

        tickHandler.post(tickRunnable);
    }

    private void stopTicker() {
        if (tickRunnable != null) {
            tickHandler.removeCallbacks(tickRunnable);
        }
    }

    private void evaluateNow() {
        LocalTime now = LocalTime.now();

        ScheduleItem current = null;
        List<ScheduleItem> upcoming = new ArrayList<>();
        List<ScheduleItem> previous = new ArrayList<>();

        for (ScheduleItem item : cachedItems) {
            if (now.isAfter(item.end)) {
                item.state = ClassState.PAST;
                previous.add(item);
            } else if (now.isBefore(item.start)) {
                item.state = ClassState.UPCOMING;
                upcoming.add(item);
            } else {
                item.state = ClassState.CURRENT;
                current = item;
            }
        }

        if (current != null) {
            cardCurrentClass.setVisibility(View.VISIBLE);
            bindCurrentClass(current, now);
        } else if (!upcoming.isEmpty()) {
            showNextClass(upcoming.get(0));
        } else {
            showDayCompleted();
        }

        rvUpcoming.setAdapter(new ClassAdapter(upcoming, R.layout.item_class_upcoming));
        rvPrevious.setAdapter(new ClassAdapter(previous, R.layout.item_class_previous));
    }

    /* ===== CURRENT CLASS ===== */
    private void bindCurrentClass(ScheduleItem item, LocalTime now) {
        long secondsLeft = Duration.between(now, item.end).getSeconds();
        if (secondsLeft < 0) secondsLeft = 0;

        tvCurrentSubject.setText(item.subject);
        tvCurrentInstructor.setText(item.instructor);
        tvCurrentTime.setText(formatRemaining(secondsLeft));
    }

    /* ===== TIME FORMATTER ===== */
    private String formatRemaining(long seconds) {
        long hrs = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hrs > 0) {
            return String.format("Ends in %02d:%02d:%02d", hrs, mins, secs);
        } else if (mins > 0) {
            return String.format("Ends in %02d:%02d", mins, secs);
        } else {
            return String.format("Ends in %02d sec", secs);
        }
    }

    /* ===== PRE / POST STATES ===== */
    private void showNextClass(ScheduleItem next) {
        cardCurrentClass.setVisibility(View.VISIBLE);
        tvCurrentSubject.setText("Next: " + next.subject);
        tvCurrentInstructor.setText(next.instructor);
        tvCurrentTime.setText("Starts at " + next.start);
    }

    private void showDayCompleted() {
        cardCurrentClass.setVisibility(View.VISIBLE);
        tvCurrentSubject.setText("All classes completed");
        tvCurrentInstructor.setText("");
        tvCurrentTime.setText("Morning session finished");
    }

    private void showEmptyState(String msg) {
        cardCurrentClass.setVisibility(View.VISIBLE);
        tvCurrentSubject.setText(msg);
        tvCurrentInstructor.setText("");
        tvCurrentTime.setText("");
    }

    private static String readFile(File file) throws IOException {
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line);
            }
        }
        return out.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        stopTicker();
    }
}
