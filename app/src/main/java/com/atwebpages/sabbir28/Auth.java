package com.atwebpages.sabbir28;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.atwebpages.sabbir28.Core.HTTP;
import com.atwebpages.sabbir28.Server.EditProfile.EditProfile;
import com.atwebpages.sabbir28.Server.EditProfile.EditProfileError;
import com.atwebpages.sabbir28.Server.Login.Login;
import com.atwebpages.sabbir28.Server.Login.LoginError;
import com.atwebpages.sabbir28.Server.Registration.Registration;
import com.atwebpages.sabbir28.Server.Registration.RegistrationError;
import com.atwebpages.sabbir28.Server.Verify.Verify;
import com.atwebpages.sabbir28.Server.Verify.VerifyError;

import java.io.File;

/**
 * Auth is a unified wrapper/facade for all user-related server operations.
 * It provides a clean single-point interface for:
 * <ul>
 *     <li>User registration</li>
 *     <li>User login</li>
 *     <li>Token verification</li>
 *     <li>Profile editing (with optional fields)</li>
 * </ul>
 * <p>
 * Each operation supports three calling styles:
 * <ul>
 *     <li><b>Async</b> — uses legacy AsyncTask (runs on background → callback on main thread)</li>
 *     <li><b>InBackground</b> — uses plain Thread + Handler (recommended over AsyncTask)</li>
 *     <li><b>Sync</b> — blocking call (only use from background threads / coroutines / workers)</li>
 * </ul>
 * <p>
 * Recommended error handling pattern:
 * <pre>
 * if (status == 200) {
 *     // success
 * } else {
 *     RegistrationError error = new RegistrationError(body);
 *     Log.e("AUTH", "Registration failed: " + error.getMessage());
 *     // show toast / dialog with error.getMessage()
 * }
 * </pre>
 * Supported error classes:
 * <ul>
 *     <li>{@link RegistrationError}</li>
 *     <li>{@link LoginError}</li>
 *     <li>{@link VerifyError}</li>
 *     <li>{@link EditProfileError}</li>
 * </ul>
 */
public final class Auth {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * Generic callback interface used by all Auth operations.
     * Returns HTTP status code and raw JSON response body from the server.
     */
    public interface Callback {
        /**
         * Called when the server responds (or network error occurs).
         *
         * @param statusCode   HTTP status code (200 = OK, 400, 401, 409, 500, etc.)
         * @param responseBody Raw JSON string returned by the server (may be empty or error JSON)
         */
        void onResponse(int statusCode, @Nullable String responseBody);
    }

    // ─────────────────────────────────────────────
    //  REGISTRATION
    // ─────────────────────────────────────────────

    /**
     * Register a new user asynchronously using AsyncTask.
     * <p>
     * <b>Example with error handling:</b>
     * <pre>{@code
     * Auth.registerAsync("John Doe", "john@example.com", "pass123", "01712345678",
     *     "181-15-1234", "181152016", "4th", "A", profileImageFile,
     *     (status, body) -> {
     *         if (status == 200) {
     *             // success - maybe go to login or home
     *         } else {
     *             RegistrationError error = new RegistrationError(body);
     *             Log.e("REG", error.getMessage());
     *             // show Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
     *         }
     *     });
     * }</pre>
     *
     * @param name        Full name of user
     * @param email       Unique email address
     * @param password    User password
     * @param phone       Phone number
     * @param classRoll   Class roll number
     * @param regNo       Registration number
     * @param year        Academic year (e.g. "1st", "2nd", "4th")
     * @param section     Section (e.g. "A", "B", "C")
     * @param profileImage Profile picture file (can be null)
     * @param callback    Callback to receive server response
     */
    public static void registerAsync(
            String name, String email, String password,
            String phone, String classRoll, String regNo,
            String year, String section, @Nullable File profileImage,
            @Nullable Callback callback) {

        Registration.registerAsync(name, email, password, phone, classRoll, regNo, year, section, profileImage,
                (status, body) -> {
                    if (callback != null) callback.onResponse(status, body);
                });
    }

    /**
     * Register a new user on a background thread (recommended over AsyncTask).
     * Callback is posted back to the main thread via Handler.
     */
    public static void registerInBackground(
            String name, String email, String password,
            String phone, String classRoll, String regNo,
            String year, String section, @Nullable File profileImage,
            @Nullable Callback callback) {

        Registration.registerThread(name, email, password, phone, classRoll, regNo, year, section, profileImage,
                (status, body) -> {
                    if (callback != null) {
                        MAIN_HANDLER.post(() -> callback.onResponse(status, body));
                    }
                });
    }

    /**
     * Blocking registration call — only call from background thread!
     */
    public static HTTP.Response registerSync(
            String name, String email, String password,
            String phone, String classRoll, String regNo,
            String year, String section, @Nullable File profileImage) {

        return Registration.registerSync(name, email, password, phone, classRoll, regNo, year, section, profileImage);
    }

    // ─────────────────────────────────────────────
    //  LOGIN
    // ─────────────────────────────────────────────

    /**
     * Login user asynchronously using AsyncTask.
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * Auth.loginAsync("john@example.com", "pass123", (status, body) -> {
     *     if (status == 200) {
     *         // success - save token, go to main screen
     *     } else {
     *         LoginError error = new LoginError(body);
     *         Log.e("LOGIN", error.getMessage());
     *         // show error to user
     *     }
     * });
     * }</pre>
     */
    public static void loginAsync(String email, String password, @Nullable Callback callback) {
        Login.loginAsync(email, password,
                (status, body) -> {
                    if (callback != null) callback.onResponse(status, body);
                });
    }

    public static void loginInBackground(String email, String password, @Nullable Callback callback) {
        Login.loginThread(email, password,
                (status, body) -> {
                    if (callback != null) {
                        MAIN_HANDLER.post(() -> callback.onResponse(status, body));
                    }
                });
    }

    public static HTTP.Response loginSync(String email, String password) {
        return Login.loginSync(email, password);
    }

    // ─────────────────────────────────────────────
    //  VERIFY TOKEN
    // ─────────────────────────────────────────────

    /**
     * Verify authentication token asynchronously (e.g. on app start).
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * Auth.verifyTokenAsync(savedToken, (status, body) -> {
     *     if (status == 200) {
     *         // token is valid → proceed to main screen
     *     } else {
     *         VerifyError error = new VerifyError(body);
     *         // token invalid/expired → go to login
     *     }
     * });
     * }</pre>
     */
    public static void verifyTokenAsync(String token, @Nullable Callback callback) {
        Verify.verifyAsync(token,
                (status, body) -> {
                    if (callback != null) callback.onResponse(status, body);
                });
    }

    public static void verifyTokenInBackground(String token, @Nullable Callback callback) {
        Verify.verifyThread(token,
                (status, body) -> {
                    if (callback != null) {
                        MAIN_HANDLER.post(() -> callback.onResponse(status, body));
                    }
                });
    }

    public static HTTP.Response verifyTokenSync(String token) {
        return Verify.verifySync(token);
    }

    // ─────────────────────────────────────────────
    //  EDIT PROFILE
    // ─────────────────────────────────────────────

    /**
     * Edit user profile asynchronously. All fields except token are optional.
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * Auth.editProfileAsync(token, "New Name", null, "01799999999", null, null, null, null, newImageFile,
     *     (status, body) -> {
     *         if (status == 200) {
     *             // profile updated
     *         } else {
     *             EditProfileError error = new EditProfileError(body);
     *             Log.e("EDIT", error.getMessage());
     *         }
     *     });
     * }</pre>
     *
     * @param token        Authentication token (required)
     * @param name         New name (pass null to keep current)
     * @param email        New email (pass null to keep current)
     * @param phone        New phone (pass null to keep current)
     * @param classRoll    New class roll (pass null to keep current)
     * @param regNo        New registration number (pass null to keep current)
     * @param year         New academic year (pass null to keep current)
     * @param section      New section (pass null to keep current)
     * @param profileImage New profile picture (pass null to keep current)
     * @param callback     Response callback
     */
    public static void editProfileAsync(
            String token,
            @Nullable String name, @Nullable String email, @Nullable String phone,
            @Nullable String classRoll, @Nullable String regNo,
            @Nullable String year, @Nullable String section,
            @Nullable File profileImage,
            @Nullable Callback callback) {

        EditProfile.editAsync(token, name, email, phone, classRoll, regNo, year, section, profileImage,
                (status, body) -> {
                    if (callback != null) callback.onResponse(status, body);
                });
    }

    public static void editProfileInBackground(
            String token,
            @Nullable String name, @Nullable String email, @Nullable String phone,
            @Nullable String classRoll, @Nullable String regNo,
            @Nullable String year, @Nullable String section,
            @Nullable File profileImage,
            @Nullable Callback callback) {

        EditProfile.editThread(token, name, email, phone, classRoll, regNo, year, section, profileImage,
                (status, body) -> {
                    if (callback != null) {
                        MAIN_HANDLER.post(() -> callback.onResponse(status, body));
                    }
                });
    }

    public static HTTP.Response editProfileSync(
            String token,
            @Nullable String name, @Nullable String email, @Nullable String phone,
            @Nullable String classRoll, @Nullable String regNo,
            @Nullable String year, @Nullable String section,
            @Nullable File profileImage) {

        return EditProfile.editSync(token, name, email, phone, classRoll, regNo, year, section, profileImage);
    }
}