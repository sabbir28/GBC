package sabbir.apk.UI;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import sabbir.apk.R;

public class Setting extends AppCompatActivity {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_NOTIFICATIONS = "pref_notifications";
    private static final String KEY_SMS = "pref_sms";

    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchSms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        switchNotifications = findViewById(R.id.switch_notification);
        switchSms = findViewById(R.id.switch_sms);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true);
        boolean smsEnabled = prefs.getBoolean(KEY_SMS, false);

        switchNotifications.setChecked(notificationsEnabled);
        switchSms.setChecked(smsEnabled);
        updateSmsAvailability(notificationsEnabled, prefs);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
            updateSmsAvailability(isChecked, prefs);
        });

        switchSms.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_SMS, isChecked).apply());
    }

    private void updateSmsAvailability(boolean notificationsEnabled, SharedPreferences prefs) {
        if (!notificationsEnabled) {
            switchSms.setChecked(false);
            prefs.edit().putBoolean(KEY_SMS, false).apply();
        }
        switchSms.setEnabled(notificationsEnabled);
    }
}
