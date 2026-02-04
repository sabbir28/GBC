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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sabbir.apk.InterNet.API.GitHub.RoutineManagerApi;
import sabbir.apk.InterNet.Deta.ClassState;
import sabbir.apk.InterNet.Deta.ScheduleItem;

import sabbir.apk.R;
import sabbir.apk.UI.Adapter.ClassAdapter;
import sabbir.apk.UI.Adapter.DayAdapter;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MaterialCardView cardCurrentClass;
    private RecyclerView rvUpcoming;
    private RecyclerView rvPrevious;
    private RecyclerView rvDays;
    private TextView tvUpcomingHeader;
    private TextView tvCurrentSubject;
    private TextView tvCurrentTime;
    private TextView tvCurrentInstructor;

    private DayAdapter dayAdapter;
    private final Map<String, List<ScheduleItem>> scheduleByDay = new LinkedHashMap<>();
    private final List<String> dayOrder = Arrays.asList(
            "Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday", "Saturday", "Sunday"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // UI binding
        cardCurrentClass = findViewById(R.id.card_current_class);
        rvUpcoming = findViewById(R.id.rv_upcoming);
        rvPrevious = findViewById(R.id.rv_previous);
        rvDays = findViewById(R.id.rv_days);
        tvUpcomingHeader = findViewById(R.id.tv_upcoming_header);
        tvCurrentSubject = findViewById(R.id.tv_current_subject);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvCurrentInstructor = findViewById(R.id.tv_current_instructor);

        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvPrevious.setLayoutManager(new LinearLayoutManager(this));
        rvDays.setLayoutManager(new LinearLayoutManager(this));

        File routineFile = RoutineManagerApi.getRoutineFile(getApplicationContext());

        if (routineFile == null || !routineFile.exists()) {
            Log.e(TAG, "Routine file not found");
            return;
        }

        loadRoutineAsync(routineFile);
    }

    private void loadRoutineAsync(File file) {
        executor.execute(() -> {
            try {
                String json = readFile(file);
                JSONObject routineJson = new JSONObject(json);

                mainHandler.post(() -> renderRoutine(routineJson));

            } catch (IOException | JSONException e) {
                Log.e(TAG, "Failed to load routine", e);
            }
        });
    }

    private String readFile(File file) throws IOException {
        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private void renderRoutine(JSONObject routineJson) {
        try {
            JSONObject schedule = routineJson.getJSONObject("schedule");

            List<String> days = resolveDays(schedule);
            scheduleByDay.clear();

            for (String day : days) {
                JSONArray dayEntries = schedule.optJSONArray(day);
                if (dayEntries == null) {
                    continue;
                }

                List<ScheduleItem> items = new ArrayList<>();

                for (int i = 0; i < dayEntries.length(); i++) {
                    JSONObject obj = dayEntries.getJSONObject(i);

                    ScheduleItem item = new ScheduleItem();
                    String subjectName = obj.isNull("subject_name")
                            ? null
                            : obj.optString("subject_name", null);
                    String subjectCode = obj.isNull("subject_code")
                            ? null
                            : obj.optString("subject_code", null);

                    if (subjectName == null && subjectCode == null) {
                        item.subject = "Free Period";
                    } else {
                        item.subject = subjectName != null && !subjectName.isEmpty()
                                ? subjectName
                                : subjectCode;
                    }

                    item.instructor = obj.isNull("instructor_name")
                            ? null
                            : obj.optString("instructor_name", null);

                    item.room = obj.isNull("room")
                            ? "—"
                            : obj.optString("room", "—");

                    item.period = "Period " + (i + 1);

                    if (obj.has("start_time") && obj.has("end_time")
                            && !obj.isNull("start_time")
                            && !obj.isNull("end_time")) {
                        item.start = LocalTime.parse(obj.getString("start_time"));
                        item.end = LocalTime.parse(obj.getString("end_time"));
                    }

                    items.add(item);
                }

                scheduleByDay.put(day, items);
            }

            setupDaysSidebar(days);
            String defaultDay = resolveToday(days);
            displayDay(defaultDay);

        } catch (JSONException e) {
            Log.e(TAG, "Invalid routine JSON", e);
        }
    }

    private void setupDaysSidebar(List<String> days) {
        dayAdapter = new DayAdapter(days, resolveToday(days), this::displayDay);
        rvDays.setAdapter(dayAdapter);
    }

    private void displayDay(String day) {
        dayAdapter.setSelectedDay(day);
        tvUpcomingHeader.setText(day + " Routine");
        List<ScheduleItem> items = scheduleByDay.get(day);
        if (items == null) {
            items = new ArrayList<>();
        }
        applyTimeState(items);
    }

    private List<String> resolveDays(JSONObject schedule) {
        List<String> days = new ArrayList<>();
        for (String day : dayOrder) {
            if (schedule.has(day)) {
                days.add(day);
            }
        }
        if (!days.isEmpty()) {
            return days;
        }

        for (java.util.Iterator<String> it = schedule.keys(); it.hasNext(); ) {
            days.add(it.next());
        }
        return days;
    }

    private String resolveToday(List<String> days) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        String dayName;
        switch (today) {
            case MONDAY:
                dayName = "Monday";
                break;
            case TUESDAY:
                dayName = "Tuesday";
                break;
            case WEDNESDAY:
                dayName = "Wednesday";
                break;
            case THURSDAY:
                dayName = "Thursday";
                break;
            case FRIDAY:
                dayName = "Friday";
                break;
            case SATURDAY:
                dayName = "Saturday";
                break;
            default:
                dayName = "Sunday";
                break;
        }
        if (days.contains(dayName)) {
            return dayName;
        }
        return days.isEmpty() ? dayName : days.get(0);
    }

    private void applyTimeState(List<ScheduleItem> allItems) {
        LocalTime now = LocalTime.now();

        ScheduleItem current = null;
        List<ScheduleItem> upcoming = new ArrayList<>();
        List<ScheduleItem> previous = new ArrayList<>();

        for (ScheduleItem item : allItems) {
            if (item.isCurrent(now)) {
                item.state = ClassState.CURRENT;
                current = item;
            } else if (item.isPast(now)) {
                item.state = ClassState.PAST;
                previous.add(item);
            } else {
                item.state = ClassState.UPCOMING;
                upcoming.add(item);
            }
        }

        // UI contract
        cardCurrentClass.setVisibility(current == null ? View.GONE : View.VISIBLE);
        if (current != null) {
            tvCurrentSubject.setText(current.subject);
            if (current.start != null && current.end != null) {
                tvCurrentTime.setText(current.start + " – " + current.end);
            } else {
                String timeLabel = current.period == null ? "Scheduled" : current.period;
                if (current.room != null && !current.room.trim().isEmpty()
                        && !current.room.equals("—")) {
                    timeLabel = timeLabel + " • Room " + current.room;
                }
                tvCurrentTime.setText(timeLabel);
            }
            String instructor = current.instructor == null || current.instructor.trim().isEmpty()
                    ? "TBA"
                    : current.instructor;
            tvCurrentInstructor.setText(instructor);
        }

        rvUpcoming.setAdapter(
                new ClassAdapter(upcoming, R.layout.item_class_upcoming)
        );

        rvPrevious.setAdapter(
                new ClassAdapter(previous, R.layout.item_class_previous)
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
