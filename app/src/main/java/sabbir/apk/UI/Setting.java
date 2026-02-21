package sabbir.apk.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.atwebpages.sabbir28.Core.TokenManager;
import com.atwebpages.sabbir28.Core.UserManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.analytics.FirebaseAnalytics;

import sabbir.apk.R;
import sabbir.apk.Reminder.ReminderPreferences;
import sabbir.apk.UI.Settings.ProfileActivity;

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

    private LinearLayout ProfileSetting;

    private TokenManager tokenManager;
    private UserManager userManager;

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        logScreenView();

        tokenManager = new TokenManager(this);
        userManager = new UserManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> {
            finish();
            overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_slide_out);
        });

        switchNotifications = findViewById(R.id.switch_notification);
        switchSms = findViewById(R.id.switch_sms);
        switchClassReminders = findViewById(R.id.switch_class_reminders);
        switchMorningAlarm = findViewById(R.id.switch_morning_alarm);



        ProfileSetting = findViewById(R.id.ProfileSetting);

        setupProfileUpdateAction();

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
            logPreferenceChange("notifications", isChecked);
        });

        switchSms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SMS, isChecked).apply();
            logPreferenceChange("sms", isChecked);
        });

        switchClassReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_CLASS_REMINDERS, isChecked).apply();
            logPreferenceChange("class_reminders", isChecked);
        });

        switchMorningAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_MORNING_ALARM, isChecked).apply();
            logPreferenceChange("morning_alarm", isChecked);
        });
    }


    private void setupProfileUpdateAction() {

        ProfileSetting.setOnClickListener(view -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(R.anim.activity_fade_slide_in, R.anim.activity_fade_slide_out);
        });
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
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

    private void logScreenView() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "SettingActivity");
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, getClass().getSimpleName());
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    private void logPreferenceChange(String key, boolean value) {
        Bundle bundle = new Bundle();
        bundle.putString("preference_key", key);
        bundle.putBoolean("preference_value", value);
        mFirebaseAnalytics.logEvent("preference_changed", bundle);
    }

    private void logProfileUpdated(boolean success) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("success", success);
        mFirebaseAnalytics.logEvent("profile_updated", bundle);
    }
}
