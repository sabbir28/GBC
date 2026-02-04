package sabbir.apk.UI;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import sabbir.apk.R;
import sabbir.apk.Reminder.ReminderPreferences;

public class Setting extends AppCompatActivity {

    private static final String PREFS_NAME = ReminderPreferences.PREFS_NAME;
    private static final String KEY_NOTIFICATIONS = ReminderPreferences.KEY_NOTIFICATIONS;
    private static final String KEY_SMS = "pref_sms";
    private static final String KEY_CLASS_REMINDERS = ReminderPreferences.KEY_CLASS_REMINDERS;
    private static final String KEY_MORNING_ALARM = ReminderPreferences.KEY_MORNING_ALARM;

    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchSms;
    private SwitchMaterial switchClassReminders;
    private SwitchMaterial switchMorningAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        switchNotifications = findViewById(R.id.switch_notification);
        switchSms = findViewById(R.id.switch_sms);
        switchClassReminders = findViewById(R.id.switch_class_reminders);
        switchMorningAlarm = findViewById(R.id.switch_morning_alarm);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true);
        boolean smsEnabled = prefs.getBoolean(KEY_SMS, false);
        boolean classRemindersEnabled = prefs.getBoolean(KEY_CLASS_REMINDERS, true);
        boolean morningAlarmEnabled = prefs.getBoolean(KEY_MORNING_ALARM, true);

        switchNotifications.setChecked(notificationsEnabled);
        switchSms.setChecked(smsEnabled);
        switchClassReminders.setChecked(classRemindersEnabled);
        switchMorningAlarm.setChecked(morningAlarmEnabled);
        updateReminderAvailability(notificationsEnabled, prefs);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
            updateReminderAvailability(isChecked, prefs);
        });

        switchSms.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_SMS, isChecked).apply());

        switchClassReminders.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_CLASS_REMINDERS, isChecked).apply());

        switchMorningAlarm.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_MORNING_ALARM, isChecked).apply());
    }

    private void updateReminderAvailability(boolean notificationsEnabled, SharedPreferences prefs) {
        if (!notificationsEnabled) {
            switchSms.setChecked(false);
            prefs.edit().putBoolean(KEY_SMS, false).apply();
            switchClassReminders.setChecked(false);
            prefs.edit().putBoolean(KEY_CLASS_REMINDERS, false).apply();
            switchMorningAlarm.setChecked(false);
            prefs.edit().putBoolean(KEY_MORNING_ALARM, false).apply();
        }
        switchSms.setEnabled(notificationsEnabled);
        switchClassReminders.setEnabled(notificationsEnabled);
        switchMorningAlarm.setEnabled(notificationsEnabled);
    }
}
