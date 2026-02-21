package sabbir.apk.UI.Auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.atwebpages.sabbir28.Auth;
import com.atwebpages.sabbir28.Server.Registration.RegistrationError;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import sabbir.apk.R;
import sabbir.apk.UI.FeedbackHelper;

public class RegisterActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private ImageView cameraIcon;

    private TextInputEditText nameEditText, emailEditText, passwordEditText,
            phoneEditText, rollEditText, regNoEditText;

    private AutoCompleteTextView yearDropdown, sectionDropdown;

    private MaterialButton registerButton, loginLinkButton;

    private File selectedProfileImageFile = null;

    // Image picker launcher
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) processSelectedImage(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupDropdowns();
        setupClickListeners();
    }

    private void initViews() {
        profileImageView = findViewById(R.id.profileImageView);
        cameraIcon = findViewById(R.id.cameraIcon);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        rollEditText = findViewById(R.id.rollEditText);
        regNoEditText = findViewById(R.id.regNoEditText);

        yearDropdown = findViewById(R.id.yearDropdown);
        sectionDropdown = findViewById(R.id.sectionDropdown);

        registerButton = findViewById(R.id.registerButton);
        loginLinkButton = findViewById(R.id.loginLinkButton);
    }

    private void setupDropdowns() {
        // Year dropdown
        String[] years = getResources().getStringArray(R.array.academic_years);
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, years);
        yearDropdown.setAdapter(yearAdapter);
        yearDropdown.setKeyListener(null); // prevent typing
        yearDropdown.setText("", false);

        // Section dropdown
        String[] sections = getResources().getStringArray(R.array.sections);
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, sections);
        sectionDropdown.setAdapter(sectionAdapter);
        sectionDropdown.setKeyListener(null); // prevent typing
        sectionDropdown.setText("", false);
    }

    private void setupClickListeners() {
        cameraIcon.setOnClickListener(v -> openGallery());
        profileImageView.setOnClickListener(v -> openGallery());

        FeedbackHelper.setPrimaryButtonWithFeedback(registerButton, this::register);
        loginLinkButton.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_slide_out);
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap != null) {
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, 25, 25, true);
                profileImageView.setImageBitmap(resized);

                File tempFile = new File(getCacheDir(), "profile_image.png");
                FileOutputStream fos = new FileOutputStream(tempFile);
                resized.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();

                selectedProfileImageFile = tempFile;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private String getTrimmedText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void register() {
        String name = getTrimmedText(nameEditText);
        String email = getTrimmedText(emailEditText);
        String password = getTrimmedText(passwordEditText);
        String phone = getTrimmedText(phoneEditText);
        String roll = getTrimmedText(rollEditText);
        String regNo = getTrimmedText(regNoEditText);
        String year = yearDropdown.getText() != null ? yearDropdown.getText().toString().trim() : "";
        String section = sectionDropdown.getText() != null ? sectionDropdown.getText().toString().trim() : "";

        // Basic validations
        if (name.isEmpty()) { nameEditText.setError("Full name required"); nameEditText.requestFocus(); return; }
        if (email.isEmpty()) { emailEditText.setError("Email required"); emailEditText.requestFocus(); return; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailEditText.setError("Enter valid email"); emailEditText.requestFocus(); return; }
        if (password.isEmpty()) { passwordEditText.setError("Password required"); passwordEditText.requestFocus(); return; }
        if (password.length() < 6) { passwordEditText.setError("Min 6 characters"); passwordEditText.requestFocus(); return; }
        if (phone.isEmpty()) { phoneEditText.setError("Phone required"); phoneEditText.requestFocus(); return; }
        if (!android.util.Patterns.PHONE.matcher(phone).matches()) { phoneEditText.setError("Enter valid phone"); phoneEditText.requestFocus(); return; }
        if (roll.isEmpty()) { rollEditText.setError("Class roll required"); rollEditText.requestFocus(); return; }
        if (regNo.isEmpty()) { regNoEditText.setError("Registration number required"); regNoEditText.requestFocus(); return; }

        // Ensure valid dropdown selections
        if (!Arrays.asList(getResources().getStringArray(R.array.academic_years)).contains(year)) {
            yearDropdown.setError("Select valid year"); yearDropdown.requestFocus(); return;
        }
        if (!Arrays.asList(getResources().getStringArray(R.array.sections)).contains(section)) {
            sectionDropdown.setError("Select valid section"); sectionDropdown.requestFocus(); return;
        }

        registerButton.setEnabled(false);

        Auth.registerInBackground(
                name, email, password, phone, roll, regNo, year, section, selectedProfileImageFile,
                (status, body) -> {
                    registerButton.setEnabled(true);
                    if (status == 200) {
                        FeedbackHelper.playSuccessSound(RegisterActivity.this);
                        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                        overridePendingTransition(R.anim.activity_fade_slide_in, R.anim.activity_fade_slide_out);
                    } else {
                        RegistrationError error = new RegistrationError(body);
                        Toast.makeText(RegisterActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }
}