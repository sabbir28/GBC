package sabbir.apk.UI.home;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import sabbir.apk.InterNet.Deta.ClassState;
import sabbir.apk.InterNet.Deta.ScheduleItem;
import sabbir.apk.R;
import sabbir.apk.UI.Adapter.ClassAdapter;

public final class HomeScheduleController {

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

    private final List<ScheduleItem> todaySchedule = new ArrayList<>();
    private final View cardCurrentClass;
    private final RecyclerView rvUpcoming;
    private final RecyclerView rvPrevious;
    private final TextView tvCurrentSubject;
    private final TextView tvCurrentInstructor;
    private final TextView tvCurrentTime;
    private final View upcomingHeader;
    private final View pastHeader;
    private final ReminderSchedulerHost host;

    private Handler uiTickHandler;
    private Runnable tickRunnable;

    public interface ReminderSchedulerHost {
        void scheduleTodayReminders(List<ScheduleItem> items, LocalDate date);
    }

    public HomeScheduleController(
            @NonNull View cardCurrentClass,
            @NonNull RecyclerView rvUpcoming,
            @NonNull RecyclerView rvPrevious,
            @NonNull TextView tvCurrentSubject,
            @NonNull TextView tvCurrentInstructor,
            @NonNull TextView tvCurrentTime,
            View upcomingHeader,
            View pastHeader,
            @NonNull ReminderSchedulerHost host
    ) {
        this.cardCurrentClass = cardCurrentClass;
        this.rvUpcoming = rvUpcoming;
        this.rvPrevious = rvPrevious;
        this.tvCurrentSubject = tvCurrentSubject;
        this.tvCurrentInstructor = tvCurrentInstructor;
        this.tvCurrentTime = tvCurrentTime;
        this.upcomingHeader = upcomingHeader;
        this.pastHeader = pastHeader;
        this.host = host;
        validateTimeSlots();
    }

    public void renderTodaySchedule(JSONObject root) throws JSONException {
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
            item.subject = sanitizeSubject(entry.optString("subject_name", "Free Period"));
            item.instructor = entry.optString("instructor_name", "—");
            item.start = SLOT_STARTS[i];
            item.end = SLOT_ENDS[i];
            todaySchedule.add(item);
        }

        if (todaySchedule.isEmpty()) {
            showEmptyState("No classes today");
            return;
        }

        host.scheduleTodayReminders(todaySchedule, LocalDate.now());
        startUiTicker();
    }

    public boolean hasSchedule() {
        return !todaySchedule.isEmpty();
    }

    public void startUiTicker() {
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

    public void stopUiTicker() {
        if (uiTickHandler != null && tickRunnable != null) {
            uiTickHandler.removeCallbacks(tickRunnable);
        }
    }

    public void showErrorState(String message) {
        showEmptyState(message);
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
        if (secondsRemaining < 0) {
            secondsRemaining = 0;
        }

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

    private void updateRecyclerViews(List<ScheduleItem> upcoming, List<ScheduleItem> past) {
        if (upcoming.isEmpty()) {
            rvUpcoming.setVisibility(View.GONE);
            if (upcomingHeader != null) {
                upcomingHeader.setVisibility(View.GONE);
            }
        } else {
            rvUpcoming.setVisibility(View.VISIBLE);
            if (upcomingHeader != null) {
                upcomingHeader.setVisibility(View.VISIBLE);
            }
            rvUpcoming.setAdapter(new ClassAdapter(upcoming, R.layout.item_class_upcoming));
        }

        if (past.isEmpty()) {
            rvPrevious.setVisibility(View.GONE);
            if (pastHeader != null) {
                pastHeader.setVisibility(View.GONE);
            }
        } else {
            rvPrevious.setVisibility(View.VISIBLE);
            if (pastHeader != null) {
                pastHeader.setVisibility(View.VISIBLE);
            }
            rvPrevious.setAdapter(new ClassAdapter(past, R.layout.item_class_previous));
        }
    }

    private String formatRemainingTime(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;

        if (h > 0) {
            return String.format(Locale.US, "Ends in %02d:%02d:%02d", h, m, s);
        } else if (m > 0) {
            return String.format(Locale.US, "Ends in %02d:%02d", m, s);
        }
        return String.format(Locale.US, "Ends in %02d sec", s);
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

    private String sanitizeSubject(String subject) {
        if ("null".equals(subject)) {
            return "Free Period";
        }
        return subject;
    }
}
