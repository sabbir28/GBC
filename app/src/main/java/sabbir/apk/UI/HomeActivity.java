package sabbir.apk.UI;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // UI binding
        cardCurrentClass = findViewById(R.id.card_current_class);
        rvUpcoming = findViewById(R.id.rv_upcoming);
        rvPrevious = findViewById(R.id.rv_previous);

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
