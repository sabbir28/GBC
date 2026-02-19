package sabbir.apk.UI;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class UserProfilePreferences {

    private static final String PREFS_NAME = "user_profile";

    public static final String KEY_LOGIN_ENABLED = "login_enabled";
    public static final String KEY_FULL_NAME = "full_name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_DEPARTMENT = "department";

    private UserProfilePreferences() {
        // Utility class
    }

    @NonNull
    public static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isLoginEnabled(@NonNull SharedPreferences prefs) {
        return prefs.getBoolean(KEY_LOGIN_ENABLED, false);
    }

    @NonNull
    public static String getFullName(@NonNull SharedPreferences prefs) {
        return prefs.getString(KEY_FULL_NAME, "");
    }

    @NonNull
    public static String getDepartment(@NonNull SharedPreferences prefs) {
        return prefs.getString(KEY_DEPARTMENT, "");
    }

    public static void saveProfile(
            @NonNull SharedPreferences prefs,
            boolean loginEnabled,
            @NonNull String fullName,
            @NonNull String email,
            @NonNull String phone,
            @NonNull String department
    ) {
        prefs.edit()
                .putBoolean(KEY_LOGIN_ENABLED, loginEnabled)
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PHONE, phone)
                .putString(KEY_DEPARTMENT, department)
                .apply();
    }
}
