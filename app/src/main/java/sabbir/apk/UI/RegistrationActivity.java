package sabbir.apk.UI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import sabbir.apk.R;

public final class RegistrationActivity extends AppCompatActivity {

    public static final String USER_PREFS = "user_profile";
    public static final String KEY_LOGIN_ENABLED = "login_enabled";
    public static final String KEY_FULL_NAME = "full_name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_DEPARTMENT = "department";

    private SwitchMaterial switchOptionalLogin;
    private EditText etFullName;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etDepartment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_registration);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        switchOptionalLogin = findViewById(R.id.switch_optional_login);
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etDepartment = findViewById(R.id.et_department);
        Button btnSave = findViewById(R.id.btn_save_profile);

        SharedPreferences prefs = getSharedPreferences(USER_PREFS, MODE_PRIVATE);
        switchOptionalLogin.setChecked(prefs.getBoolean(KEY_LOGIN_ENABLED, false));
        etFullName.setText(prefs.getString(KEY_FULL_NAME, ""));
        etEmail.setText(prefs.getString(KEY_EMAIL, ""));
        etPhone.setText(prefs.getString(KEY_PHONE, ""));
        etDepartment.setText(prefs.getString(KEY_DEPARTMENT, ""));

        updateFieldAvailability(switchOptionalLogin.isChecked());

        switchOptionalLogin.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateFieldAvailability(isChecked));

        btnSave.setOnClickListener(v -> {
            if (switchOptionalLogin.isChecked() && TextUtils.isEmpty(etFullName.getText().toString().trim())) {
                etFullName.setError("Name is required when optional login is enabled");
                etFullName.requestFocus();
                return;
            }

            prefs.edit()
                    .putBoolean(KEY_LOGIN_ENABLED, switchOptionalLogin.isChecked())
                    .putString(KEY_FULL_NAME, etFullName.getText().toString().trim())
                    .putString(KEY_EMAIL, etEmail.getText().toString().trim())
                    .putString(KEY_PHONE, etPhone.getText().toString().trim())
                    .putString(KEY_DEPARTMENT, etDepartment.getText().toString().trim())
                    .apply();

            Toast.makeText(this, "Profile details saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updateFieldAvailability(boolean enabled) {
        etFullName.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etDepartment.setEnabled(enabled);
    }
}
