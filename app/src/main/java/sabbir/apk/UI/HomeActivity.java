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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sabbir.apk.InterNet.API.GitHub.RoutineManagerApi;
import sabbir.apk.InterNet.Deta.ClassState;
import sabbir.apk.InterNet.Deta.ScheduleItem;

import sabbir.apk.R;
import sabbir.apk.UI.Adapter.ClassAdapter;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MaterialCardView cardCurrentClass;
    private RecyclerView rvUpcoming;
    private RecyclerView rvPrevious;
    private TextView tvCurrentSubject;
    private TextView tvCurrentTime;
    private TextView tvCurrentInstructor;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // UI binding
        cardCurrentClass = findViewById(R.id.card_current_class);
        rvUpcoming = findViewById(R.id.rv_upcoming);
        rvPrevious = findViewById(R.id.rv_previous);
        tvCurrentSubject = findViewById(R.id.tv_current_subject);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvCurrentInstructor = findViewById(R.id.tv_current_instructor);

        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvPrevious.setLayoutManager(new LinearLayoutManager(this));

        File routineFile = RoutineManagerApi.getRoutineFile(getApplicationContext());

        if (routineFile == null || !routineFile.exists()) {
            Log.w(TAG, "Routine file not found, loading demo schedule");
            applyTimeState(buildDemoSchedule());
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
            JSONObject instructors = routineJson.getJSONObject("instructors");
            JSONArray schedule = routineJson.getJSONArray("schedule");

            List<ScheduleItem> allItems = new ArrayList<>();

            for (int i = 0; i < schedule.length(); i++) {
                JSONObject obj = schedule.getJSONObject(i);

                ScheduleItem item = new ScheduleItem();
                item.subject = obj.getString("subject");
                item.start = LocalTime.parse(obj.getString("start_time"));
                item.end = LocalTime.parse(obj.getString("end_time"));

                String code = obj.isNull("instructor_code")
                        ? null
                        : obj.getString("instructor_code");

                item.instructor = code == null
                        ? "N/A"
                        : instructors.optString(code, "Unknown");

                allItems.add(item);
            }

            applyTimeState(allItems);

        } catch (JSONException e) {
            Log.e(TAG, "Invalid routine JSON", e);
        }
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
            tvCurrentTime.setText(formatTimeRange(current));
            tvCurrentInstructor.setText(current.instructor);
        }

        rvUpcoming.setAdapter(
                new ClassAdapter(upcoming, R.layout.item_class_upcoming)
        );

        rvPrevious.setAdapter(
                new ClassAdapter(previous, R.layout.item_class_previous)
        );
    }

    private List<ScheduleItem> buildDemoSchedule() {
        LocalTime now = LocalTime.now();
        List<ScheduleItem> items = new ArrayList<>();

        items.add(createItem("Calculus II", "Dr. Rahman", now.minusMinutes(120), now.minusMinutes(90)));
        items.add(createItem("Physics Lab", "A. Choudhury", now.minusMinutes(70), now.minusMinutes(40)));
        items.add(createItem("Data Structures", "M. Hasan", now.minusMinutes(15), now.plusMinutes(30)));
        items.add(createItem("Operating Systems", "R. Sarker", now.plusMinutes(45), now.plusMinutes(90)));
        items.add(createItem("Software Engineering", "N. Akter", now.plusMinutes(100), now.plusMinutes(150)));

        return items;
    }

    private ScheduleItem createItem(String subject, String instructor, LocalTime start, LocalTime end) {
        ScheduleItem item = new ScheduleItem();
        item.subject = subject;
        item.instructor = instructor;
        item.start = start;
        item.end = end;
        return item;
    }

    private String formatTimeRange(ScheduleItem item) {
        return item.start.format(timeFormatter) + " – " + item.end.format(timeFormatter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
