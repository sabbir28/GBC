package sabbir.apk.UI;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sabbir.apk.InterNet.API.GitHub.RoutineManagerApi;
import sabbir.apk.R;

public class Schedule extends AppCompatActivity {

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

    private static final DayOfWeek[] DAY_ORDER = {
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
    };

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private LinearLayout scheduleContainer;
    private TextView scheduleStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_schedule);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> {
            finish();
            overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_slide_out);
        });

        scheduleContainer = findViewById(R.id.schedule_container);
        scheduleStatus = findViewById(R.id.tv_schedule_status);

        loadSchedule();
    }

    private void loadSchedule() {
        scheduleStatus.setVisibility(View.VISIBLE);
        scheduleStatus.setText(R.string.schedule_loading);

        ioExecutor.execute(() -> {
            JSONObject routine = RoutineManagerApi.readRoutine(this);
            mainHandler.post(() -> {
                if (routine == null) {
                    showStatus(getString(R.string.schedule_missing));
                } else {
                    renderSchedule(routine);
                }
            });
        });
    }

    private void renderSchedule(JSONObject routine) {
        scheduleContainer.removeAllViews();

        try {
            JSONObject scheduleObj = routine.getJSONObject("schedule");
            boolean hasAnyDay = false;

            for (DayOfWeek day : DAY_ORDER) {
                JSONArray dayArray = scheduleObj.optJSONArray(day.name());
                if (dayArray == null) {
                    continue;
                }

                hasAnyDay = true;
                View dayCard = LayoutInflater.from(this)
                        .inflate(R.layout.item_schedule_day, scheduleContainer, false);
                TextView dayName = dayCard.findViewById(R.id.tv_day_name);
                LinearLayout dayEntries = dayCard.findViewById(R.id.day_entries_container);

                dayName.setText(day.getDisplayName(TextStyle.FULL, Locale.getDefault()));

                if (dayArray.length() == 0) {
                    addEmptyDayMessage(dayEntries);
                } else {
                    int slotCount = Math.min(dayArray.length(), SLOT_STARTS.length);
                    for (int i = 0; i < slotCount; i++) {
                        JSONObject entry = dayArray.optJSONObject(i);
                        boolean isLast = i == slotCount - 1;
                        if (entry == null) {
                            addEntryView(dayEntries, null, i, isLast);
                        } else {
                            addEntryView(dayEntries, entry, i, isLast);
                        }
                    }
                }

                scheduleContainer.addView(dayCard);
            }

            if (!hasAnyDay) {
                showStatus(getString(R.string.schedule_missing));
            } else {
                scheduleStatus.setVisibility(View.GONE);
            }
        } catch (JSONException e) {
            showStatus(getString(R.string.schedule_error));
        }
    }

    private void addEmptyDayMessage(LinearLayout container) {
        TextView emptyText = new TextView(this);
        emptyText.setText(R.string.schedule_day_empty);
        emptyText.setTextAppearance(this, R.style.TextAppearance_App_Body);
        emptyText.setPadding(0, 12, 0, 12);
        container.addView(emptyText);
    }

    private void addEntryView(LinearLayout container, JSONObject entry, int slotIndex, boolean isLast) {
        View entryView = LayoutInflater.from(this)
                .inflate(R.layout.item_schedule_entry, container, false);

        TextView timeView = entryView.findViewById(R.id.tv_entry_time);
        TextView subjectView = entryView.findViewById(R.id.tv_entry_subject);
        TextView instructorView = entryView.findViewById(R.id.tv_entry_instructor);
        TextView roomView = entryView.findViewById(R.id.tv_entry_room);
        View divider = entryView.findViewById(R.id.divider);

        String timeLabel = SLOT_STARTS[slotIndex].format(TIME_FORMATTER)
                + " – " + SLOT_ENDS[slotIndex].format(TIME_FORMATTER);
        timeView.setText(timeLabel);

        String subjectCode = normalize(entry, "subject_code");
        String subjectName = normalize(entry, "subject_name");
        String instructorName = normalize(entry, "instructor_name");
        String room = normalize(entry, "room");

        if (subjectName.isEmpty() && subjectCode.isEmpty()) {
            subjectView.setText(R.string.schedule_free_period);
            instructorView.setText(R.string.schedule_instructor_empty);
            roomView.setText(R.string.schedule_room_empty);
        } else {
            String subjectLabel = subjectName;
            if (!subjectCode.isEmpty() && !subjectName.isEmpty()) {
                subjectLabel = subjectCode + " — " + subjectName;
            } else if (!subjectCode.isEmpty()) {
                subjectLabel = subjectCode;
            }
            subjectView.setText(subjectLabel);
            instructorView.setText(formatLabel(getString(R.string.schedule_instructor_label), instructorName));
            roomView.setText(formatLabel(getString(R.string.schedule_room_label), room));
        }

        divider.setVisibility(isLast ? View.GONE : View.VISIBLE);
        container.addView(entryView);
    }

    private String formatLabel(String label, String value) {
        if (value.isEmpty()) {
            return label + " —";
        }
        return label + " " + value;
    }

    private String normalize(JSONObject entry, String key) {
        if (entry == null) {
            return "";
        }
        String value = entry.optString(key, "");
        if (value == null || "null".equalsIgnoreCase(value)) {
            return "";
        }
        return value.trim();
    }

    private void showStatus(String message) {
        scheduleContainer.removeAllViews();
        scheduleStatus.setText(message);
        scheduleStatus.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }
}
