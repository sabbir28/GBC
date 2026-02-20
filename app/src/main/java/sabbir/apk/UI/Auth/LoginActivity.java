package sabbir.apk.UI.Auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.atwebpages.sabbir28.Auth;
import com.atwebpages.sabbir28.Core.TokenManager;
import com.atwebpages.sabbir28.Server.Login.LoginError;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import sabbir.apk.MainActivity;
import sabbir.apk.R;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputEditText emailEditText;

    private TextInputLayout passwordInputLayout;
    private TextInputEditText passwordEditText;

    private CheckBox rememberMeCheckBox;
    private MaterialButton loginButton;
    private MaterialButton forgotPasswordButton;
    private MaterialButton registerButton;

    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tokenManager = new TokenManager(this);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        emailInputLayout = findViewById(R.id.emailInputLayout);
        emailEditText = findViewById(R.id.emailEditText);

        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        passwordEditText = findViewById(R.id.passwordEditText);

        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        registerButton = findViewById(R.id.registerButton);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        forgotPasswordButton.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot password clicked", Toast.LENGTH_SHORT).show();
        });

        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String email = getTrimmedText(emailEditText);
        String password = getTrimmedText(passwordEditText);

        if (!validateInputs(email, password)) return;

        Auth.loginAsync(email, password, (statusCode, responseBody) -> {
            runOnUiThread(() -> {
                if (statusCode == 200) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();

                    // Save token only if user opted in
                    if (rememberMeCheckBox.isChecked() && responseBody != null) {
                        JSONObject json = null;
                        try {
                            json = new JSONObject(responseBody);
                        } catch (JSONException e) {
                            Log.d("LOGIN", "line 93 LoginActivity attemptLogin class");
                        }
                        if ("ok".equals(json.optString("status"))) {
                            String token = json.optString("token", null);
                            if (!token.isEmpty()) {
                                tokenManager.saveToken(token);
                                Log.d("LOGIN", "Token saved successfully");
                            }
                        }
                    }

                    // Navigate to MainActivity only after successful login
                    navigateToHome();
                } else {
                    LoginError error = new LoginError(responseBody);
                    Log.e("LOGIN", error.getMessage());
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private boolean validateInputs(String email, String password) {
        boolean hasError = false;

        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email is required");
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Enter a valid email");
            hasError = true;
        } else {
            emailInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Password is required");
            hasError = true;
        } else if (password.length() < 6) {
            passwordInputLayout.setError("Password must be at least 6 characters");
            hasError = true;
        } else {
            passwordInputLayout.setError(null);
        }

        return !hasError;
    }

    private void navigateToHome() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private String getTrimmedText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}