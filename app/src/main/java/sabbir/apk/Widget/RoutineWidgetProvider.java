package sabbir.apk.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.atwebpages.sabbir28.Core.UserManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import sabbir.apk.InterNet.API.GitHub.RoutineManagerApi;
import sabbir.apk.R;
import sabbir.apk.UI.HomeActivity;

public class RoutineWidgetProvider extends AppWidgetProvider {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = buildRemoteViews(context);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            updateAllWidgets(context);
        }
    }

    public static void updateAllWidgets(@NonNull Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName widget = new ComponentName(context, RoutineWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(widget);
        if (ids == null || ids.length == 0) {
            return;
        }
        for (int id : ids) {
            manager.updateAppWidget(id, buildRemoteViews(context));
        }
    }

    private static RemoteViews buildRemoteViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_routine);
        setHeader(context, views);
        setScheduleState(context, views);
        setClickIntent(context, views);
        return views;
    }

    private static void setHeader(Context context, RemoteViews views) {
        UserManager userManager = new UserManager(context);
        String name = safe(userManager.getUserName(), "Student");
        String year = safe(userManager.getUserYear(), "-");
        String section = safe(userManager.getUserSection(), "-");

        views.setTextViewText(R.id.tv_widget_name, name);
        views.setTextViewText(R.id.tv_widget_meta, "Year " + year + " • Section " + section);
    }

    private static void setScheduleState(Context context, RemoteViews views) {
        RoutineWidgetSchedule.ScheduleState state = RoutineWidgetSchedule.loadCurrentState(context);
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        String day = today.name().substring(0, 1) + today.name().substring(1).toLowerCase(Locale.getDefault());
        views.setTextViewText(R.id.tv_widget_day, day);

        switch (state.type) {
            case CURRENT:
                views.setTextViewText(R.id.tv_widget_title, state.subject);
                views.setTextViewText(R.id.tv_widget_subtitle,
                        "Now with " + state.instructor + " • Ends " + state.timeText);
                break;
            case UPCOMING:
                views.setTextViewText(R.id.tv_widget_title, "Next: " + state.subject);
                views.setTextViewText(R.id.tv_widget_subtitle,
                        state.instructor + " • Starts " + state.timeText);
                break;
            case FINISHED:
                views.setTextViewText(R.id.tv_widget_title, "All classes completed");
                views.setTextViewText(R.id.tv_widget_subtitle, "Morning session finished");
                break;
            case EMPTY:
            default:
                views.setTextViewText(R.id.tv_widget_title, "No classes scheduled today");
                views.setTextViewText(R.id.tv_widget_subtitle,
                        RoutineManagerApi.routineFileExists(context) ? "Enjoy your free day" : "Open app to sync routine");
                break;
        }
    }

    private static void setClickIntent(Context context, RemoteViews views) {
        Intent launch = new Intent(context, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    static final class RoutineWidgetSchedule {

        static final LocalTime[] SLOT_STARTS = {
                LocalTime.of(9, 15),
                LocalTime.of(10, 0),
                LocalTime.of(10, 45),
                LocalTime.of(11, 30),
                LocalTime.of(12, 15)
        };

        static final LocalTime[] SLOT_ENDS = {
                LocalTime.of(10, 0),
                LocalTime.of(10, 45),
                LocalTime.of(11, 30),
                LocalTime.of(12, 15),
                LocalTime.of(13, 0)
        };

        static ScheduleState loadCurrentState(Context context) {
            org.json.JSONObject routine = RoutineManagerApi.readRoutine(context);
            if (routine == null) {
                return ScheduleState.empty();
            }

            try {
                org.json.JSONObject schedule = routine.getJSONObject("schedule");
                org.json.JSONArray today = schedule.optJSONArray(LocalDate.now().getDayOfWeek().name());

                if (today == null || today.length() == 0) {
                    return ScheduleState.empty();
                }

                LocalTime now = LocalTime.now();
                ScheduleState firstUpcoming = null;

                for (int i = 0; i < Math.min(today.length(), SLOT_STARTS.length); i++) {
                    org.json.JSONObject item = today.getJSONObject(i);
                    String subject = sanitizeSubject(item.optString("subject_name", "Free Period"));
                    String instructor = item.optString("instructor_name", "—");

                    LocalTime start = SLOT_STARTS[i];
                    LocalTime end = SLOT_ENDS[i];

                    if (!now.isBefore(start) && now.isBefore(end)) {
                        return ScheduleState.current(subject, instructor, end.format(TIME_FORMATTER));
                    }

                    if (now.isBefore(start) && firstUpcoming == null) {
                        firstUpcoming = ScheduleState.upcoming(subject, instructor, start.format(TIME_FORMATTER));
                    }
                }

                if (firstUpcoming != null) {
                    return firstUpcoming;
                }

                return ScheduleState.finished();
            } catch (Exception ignored) {
                return ScheduleState.empty();
            }
        }

        private static String sanitizeSubject(String subject) {
            if ("null".equals(subject)) {
                return "Free Period";
            }
            return subject;
        }
    }

    static final class ScheduleState {
        enum Type {CURRENT, UPCOMING, FINISHED, EMPTY}

        final Type type;
        final String subject;
        final String instructor;
        final String timeText;

        private ScheduleState(Type type, String subject, String instructor, String timeText) {
            this.type = type;
            this.subject = subject;
            this.instructor = instructor;
            this.timeText = timeText;
        }

        static ScheduleState current(String subject, String instructor, String end) {
            return new ScheduleState(Type.CURRENT, subject, instructor, end);
        }

        static ScheduleState upcoming(String subject, String instructor, String start) {
            return new ScheduleState(Type.UPCOMING, subject, instructor, start);
        }

        static ScheduleState finished() {
            return new ScheduleState(Type.FINISHED, "", "", "");
        }

        static ScheduleState empty() {
            return new ScheduleState(Type.EMPTY, "", "", "");
        }
    }
}
