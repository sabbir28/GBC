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

    private static final LocalTime[] SLOT_START = {
            LocalTime.of(9, 15),
            LocalTime.of(10, 5),
            LocalTime.of(11, 0),
            LocalTime.of(12, 0),
            LocalTime.of(13, 0)
    };

    private static final LocalTime[] SLOT_END = {
            LocalTime.of(10, 0),
            LocalTime.of(10, 50),
            LocalTime.of(11, 45),
            LocalTime.of(12, 45),
            LocalTime.of(13, 45)
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MaterialCardView cardCurrentClass;
    private RecyclerView rvUpcoming;
    private RecyclerView rvPrevious;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        cardCurrentClass = findViewById(R.id.card_current_class);
        rvUpcoming = findViewById(R.id.rv_upcoming);
        rvPrevious = findViewById(R.id.rv_previous);

        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvPrevious.setLayoutManager(new LinearLayoutManager(this));

        File routineFile = RoutineManagerApi.getRoutineFile(this);
        if (routineFile == null || !routineFile.exists()) {
            Log.e(TAG, "Routine file missing");
            return;
        }

        loadRoutineAsync(routineFile);
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

            DayOfWeek today = DayOfWeek.MONDAY; // TEST OVERRIDE
            JSONArray todayArray = schedule.optJSONArray(today.name());

            if (todayArray == null) {
                Log.w(TAG, "No schedule for " + today.name());
                return;
            }

            List<ScheduleItem> all = new ArrayList<>();

            for (int i = 0; i < todayArray.length() && i < SLOT_START.length; i++) {
                JSONObject obj = todayArray.getJSONObject(i);

                if (obj.isNull("subject_name")) {
                    continue;
                }

                ScheduleItem item = new ScheduleItem();
                item.subject = obj.optString("subject_name", "N/A");
                item.instructor = obj.optString("instructor_name", "N/A");
                item.state = obj.optString("room", "-");

                item.start = SLOT_START[i];
                item.end = SLOT_END[i];

                all.add(item);
            }

            applyTimeState(all);

        } catch (JSONException e) {
            Log.e(TAG, "Invalid JSON structure", e);
        }
    }

    private void applyTimeState(List<ScheduleItem> items) {
        LocalTime now = LocalTime.of(11, 34); // TEST TIME

        ScheduleItem current = null;
        List<ScheduleItem> upcoming = new ArrayList<>();
        List<ScheduleItem> previous = new ArrayList<>();

        for (ScheduleItem item : items) {
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

        if (current == null) {
            cardCurrentClass.setVisibility(View.GONE);
        } else {
            cardCurrentClass.setVisibility(View.VISIBLE);
            bindCurrentClass(current);
        }

        rvUpcoming.setAdapter(new ClassAdapter(upcoming, R.layout.item_class_upcoming));
        rvPrevious.setAdapter(new ClassAdapter(previous, R.layout.item_class_previous));
    }

    private void bindCurrentClass(ScheduleItem item) {
        ((TextView) cardCurrentClass.findViewById(R.id.tv_subject))
                .setText(item.subject);

        ((TextView) cardCurrentClass.findViewById(R.id.tv_instructor))
                .setText(item.instructor);

        ((TextView) cardCurrentClass.findViewById(R.id.tv_time))
                .setText(item.start + " - " + item.end);
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
    }
}
