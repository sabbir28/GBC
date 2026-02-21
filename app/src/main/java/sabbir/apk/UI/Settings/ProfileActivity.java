package sabbir.apk.UI.Settings;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.atwebpages.sabbir28.Auth;
import com.atwebpages.sabbir28.Core.TokenManager;
import com.atwebpages.sabbir28.Core.UserManager;
import com.atwebpages.sabbir28.Server.EditProfile.EditProfileError;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;

import sabbir.apk.R;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText nameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText phoneEditText;
    private TextInputEditText rollEditText;
    private TextInputEditText regNoEditText;
    private AutoCompleteTextView yearDropdown;
    private AutoCompleteTextView sectionDropdown;
    private MaterialButton saveButton;

    private TokenManager tokenManager;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tokenManager = new TokenManager(this);
        userManager = new UserManager(this);

        setupToolbar();
        initViews();
        setupDropdowns();
        bindUserValues();

        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        rollEditText = findViewById(R.id.rollEditText);
        regNoEditText = findViewById(R.id.regNoEditText);
        yearDropdown = findViewById(R.id.yearDropdown);
        sectionDropdown = findViewById(R.id.sectionDropdown);
        saveButton = findViewById(R.id.btnSave);
    }

    private void setupDropdowns() {
        String[] years = getResources().getStringArray(R.array.academic_years);
        String[] sections = getResources().getStringArray(R.array.sections);

        yearDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, years));
        sectionDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sections));

        yearDropdown.setKeyListener(null);
        sectionDropdown.setKeyListener(null);
    }

    private void bindUserValues() {
        nameEditText.setText(orEmpty(userManager.getUserName()));
        emailEditText.setText(orEmpty(userManager.getUserEmail()));
        phoneEditText.setText(orEmpty(userManager.getUserPhone()));
        rollEditText.setText(orEmpty(userManager.getUserClassRoll()));
        regNoEditText.setText(orEmpty(userManager.getUserRegNo()));
        yearDropdown.setText(orEmpty(userManager.getUserYear()), false);
        sectionDropdown.setText(orEmpty(userManager.getUserSection()), false);
    }

    private void saveProfile() {
        String token = tokenManager.getToken();
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String name = valueOf(nameEditText);
        String email = valueOf(emailEditText);
        String phone = valueOf(phoneEditText);
        String classRoll = valueOf(rollEditText);
        String regNo = valueOf(regNoEditText);
        String year = yearDropdown.getText() == null ? "" : yearDropdown.getText().toString().trim();
        String section = sectionDropdown.getText() == null ? "" : sectionDropdown.getText().toString().trim();

        if (!validateInput(name, email, phone, classRoll, regNo, year, section)) {
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText(R.string.saving_profile);

        Auth.editProfileInBackground(token, name, email, phone, classRoll, regNo, year, section, null,
                (statusCode, responseBody) -> runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    saveButton.setText(R.string.save_changes);

                    if (statusCode == 200) {
                        userManager.saveUser(name, email, year, section, phone, classRoll, regNo,
                                userManager.getUserImageKey(), userManager.getUserImageBase64());
                        Toast.makeText(this, R.string.profile_saved_success, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        EditProfileError error = new EditProfileError(responseBody);
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }));
    }

    private boolean validateInput(String name, String email, String phone, String classRoll,
                                  String regNo, String year, String section) {
        if (name.isEmpty()) {
            nameEditText.setError(getString(R.string.validation_name_required));
            nameEditText.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(getString(R.string.validation_email_invalid));
            emailEditText.requestFocus();
            return false;
        }
        if (!phone.isEmpty() && !Patterns.PHONE.matcher(phone).matches()) {
            phoneEditText.setError(getString(R.string.validation_phone_invalid));
            phoneEditText.requestFocus();
            return false;
        }
        if (!Arrays.asList(getResources().getStringArray(R.array.academic_years)).contains(year)) {
            yearDropdown.setError(getString(R.string.validation_year_invalid));
            yearDropdown.requestFocus();
            return false;
        }
        if (!Arrays.asList(getResources().getStringArray(R.array.sections)).contains(section)) {
            sectionDropdown.setError(getString(R.string.validation_section_invalid));
            sectionDropdown.requestFocus();
            return false;
        }
        if (classRoll.isEmpty()) {
            rollEditText.setError(getString(R.string.validation_roll_required));
            rollEditText.requestFocus();
            return false;
        }
        if (regNo.isEmpty()) {
            regNoEditText.setError(getString(R.string.validation_reg_required));
            regNoEditText.requestFocus();
            return false;
        }
        return true;
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
