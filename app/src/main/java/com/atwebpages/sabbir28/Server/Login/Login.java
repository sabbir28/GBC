package com.atwebpages.sabbir28.Server.Login;

import android.os.Handler;
import android.os.Looper;
import com.atwebpages.sabbir28.Core.HTTP;


/**
 * Handles login requests to the server
 */
public class Login {

    public interface Callback {
        void onResponse(int statusCode, String responseBody);
    }

    private static final String LOGIN_URL = "http://sabbir28.atwebpages.com/bmc/index.php?action=login";

    /**
     * Send login request asynchronously using AsyncTask
     *
     * @param email    User email
     * @param password User password
     * @param callback Callback to handle server response
     */
    public static void loginAsync(String email, String password, Callback callback) {
        new android.os.AsyncTask<Void, Void, HTTP.Response>() {
            @Override
            protected HTTP.Response doInBackground(Void... voids) {
                return sendLogin(email, password);
            }

            @Override
            protected void onPostExecute(HTTP.Response response) {
                if (callback != null) callback.onResponse(response.code, response.body);
            }
        }.execute();
    }

    /**
     * Send login request asynchronously using Thread + Handler
     *
     * @param email    User email
     * @param password User password
     * @param callback Callback to handle server response
     */
    public static void loginThread(String email, String password, Callback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            HTTP.Response response = sendLogin(email, password);
            mainHandler.post(() -> {
                if (callback != null) callback.onResponse(response.code, response.body);
            });
        }).start();
    }

    /**
     * Send login request synchronously (blocking)
     *
     * @param email    User email
     * @param password User password
     * @return HTTP.Response containing status code and body
     */
    public static HTTP.Response loginSync(String email, String password) {
        return sendLogin(email, password);
    }

    // ===== Private helper to send JSON POST =====
    private static HTTP.Response sendLogin(String email, String password) {
        try {
            // Prepare JSON body manually
            String jsonBody = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
            return HTTP.postJson(LOGIN_URL, jsonBody);
        } catch (Exception e) {
            e.printStackTrace();
            return new HTTP.Response(-1, e.getMessage());
        }
    }
}