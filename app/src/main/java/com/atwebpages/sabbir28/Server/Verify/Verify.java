package com.atwebpages.sabbir28.Server.Verify;

import android.os.Handler;
import android.os.Looper;
import com.atwebpages.sabbir28.Core.HTTP;

/**
 * Handles verify requests to the server using token
 */
public class Verify {

    public interface Callback {
        void onResponse(int statusCode, String responseBody);
    }

    private static final String VERIFY_URL = "http://sabbir28.atwebpages.com/bmc/index.php?action=verify&token=";

    /**
     * Verify token asynchronously using AsyncTask
     *
     * @param token    Token received after login
     * @param callback Callback to handle server response
     */
    public static void verifyAsync(String token, Callback callback) {
        new android.os.AsyncTask<Void, Void, HTTP.Response>() {
            @Override
            protected HTTP.Response doInBackground(Void... voids) {
                return sendVerify(token);
            }

            @Override
            protected void onPostExecute(HTTP.Response response) {
                if (callback != null) callback.onResponse(response.code, response.body);
            }
        }.execute();
    }

    /**
     * Verify token asynchronously using Thread + Handler
     *
     * @param token    Token received after login
     * @param callback Callback to handle server response
     */
    public static void verifyThread(String token, Callback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            HTTP.Response response = sendVerify(token);
            mainHandler.post(() -> {
                if (callback != null) callback.onResponse(response.code, response.body);
            });
        }).start();
    }

    /**
     * Verify token synchronously (blocking)
     *
     * @param token Token received after login
     * @return HTTP.Response containing status code and body
     */
    public static HTTP.Response verifySync(String token) {
        return sendVerify(token);
    }

    // ===== Private helper method to send GET request =====
    private static HTTP.Response sendVerify(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return new HTTP.Response(400, "{\"message\":\"Missing token\",\"fields\":[\"token\"]}");
            }
            String url = VERIFY_URL + token;
            return HTTP.get(url);
        } catch (Exception e) {
            e.printStackTrace();
            return new HTTP.Response(-1, e.getMessage());
        }
    }
}