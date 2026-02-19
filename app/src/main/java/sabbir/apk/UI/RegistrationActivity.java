package sabbir.apk.UI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import sabbir.apk.R;

public final class RegistrationActivity extends AppCompatActivity {

    private SwitchMaterial switchOptionalLogin;
    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputEditText etDepartment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        setupToolbar();
        bindViews();

        SharedPreferences prefs = UserProfilePreferences.getPrefs(this);
        populateFromPrefs(prefs);

        switchOptionalLogin.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateFieldAvailability(isChecked));

        Button btnSave = findViewById(R.id.btn_save_profile);
        btnSave.setOnClickListener(v -> onSaveClicked(prefs));
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_registration);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
    }

    private void bindViews() {
        switchOptionalLogin = findViewById(R.id.switch_optional_login);
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etDepartment = findViewById(R.id.et_department);
    }

    private void populateFromPrefs(@NonNull SharedPreferences prefs) {
        switchOptionalLogin.setChecked(UserProfilePreferences.isLoginEnabled(prefs));
        etFullName.setText(UserProfilePreferences.getFullName(prefs));
        etEmail.setText(prefs.getString(UserProfilePreferences.KEY_EMAIL, ""));
        etPhone.setText(prefs.getString(UserProfilePreferences.KEY_PHONE, ""));
        etDepartment.setText(UserProfilePreferences.getDepartment(prefs));
        updateFieldAvailability(switchOptionalLogin.isChecked());
    }

    private void onSaveClicked(@NonNull SharedPreferences prefs) {
        String fullName = getTrimmedText(etFullName);
        String email = getTrimmedText(etEmail);
        String phone = getTrimmedText(etPhone);
        String department = getTrimmedText(etDepartment);
        boolean optionalLoginEnabled = switchOptionalLogin.isChecked();

        if (!validateInput(optionalLoginEnabled, fullName, email, phone)) {
            return;
        }

        UserProfilePreferences.saveProfile(
                prefs,
                optionalLoginEnabled,
                fullName,
                email,
                phone,
                department
        );

        Toast.makeText(this, R.string.registration_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean validateInput(boolean loginEnabled, String fullName, String email, String phone) {
        if (!loginEnabled) {
            return true;
        }

        if (fullName.isEmpty()) {
            etFullName.setError(getString(R.string.registration_name_required));
            etFullName.requestFocus();
            return false;
        }

        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.registration_invalid_email));
            etEmail.requestFocus();
            return false;
        }

        if (!phone.isEmpty() && phone.length() < 8) {
            etPhone.setError(getString(R.string.registration_invalid_phone));
            etPhone.requestFocus();
            return false;
        }

        return true;
    }

    private void updateFieldAvailability(boolean enabled) {
        etFullName.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etDepartment.setEnabled(enabled);
    }

    @NonNull
    private String getTrimmedText(@NonNull TextInputEditText input) {
        CharSequence value = input.getText();
        return value == null ? "" : value.toString().trim();
    }
}
