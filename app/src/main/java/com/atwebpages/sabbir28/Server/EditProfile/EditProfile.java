package com.atwebpages.sabbir28.Server.EditProfile;

import android.os.Handler;
import android.os.Looper;
import com.atwebpages.sabbir28.Core.HTTP;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles edit profile requests to the server
 */
public class EditProfile {

    public interface Callback {
        void onResponse(int statusCode, String responseBody);
    }

    private static final String EDIT_URL = "http://sabbir28.atwebpages.com/bmc/index.php?action=edit";

    /**
     * Update profile asynchronously using AsyncTask
     *
     * @param token      User token
     * @param name       Optional name
     * @param email      Optional email
     * @param phone      Optional phone
     * @param classRoll  Optional class roll
     * @param regNo      Optional registration number
     * @param year       Optional year
     * @param section    Optional section
     * @param imageFile  Optional profile image
     * @param callback   Callback for server response
     */
    public static void editAsync(String token, String name, String email, String phone,
                                 String classRoll, String regNo, String year, String section,
                                 File imageFile, Callback callback) {

        new android.os.AsyncTask<Void, Void, HTTP.Response>() {
            @Override
            protected HTTP.Response doInBackground(Void... voids) {
                return sendEdit(token, name, email, phone, classRoll, regNo, year, section, imageFile);
            }

            @Override
            protected void onPostExecute(HTTP.Response response) {
                if (callback != null) callback.onResponse(response.code, response.body);
            }
        }.execute();
    }

    /**
     * Update profile asynchronously using Thread+Handler
     */
    public static void editThread(String token, String name, String email, String phone,
                                  String classRoll, String regNo, String year, String section,
                                  File imageFile, Callback callback) {

        Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            HTTP.Response response = sendEdit(token, name, email, phone, classRoll, regNo, year, section, imageFile);
            mainHandler.post(() -> {
                if (callback != null) callback.onResponse(response.code, response.body);
            });
        }).start();
    }

    /**
     * Update profile synchronously (blocking)
     */
    public static HTTP.Response editSync(String token, String name, String email, String phone,
                                         String classRoll, String regNo, String year, String section,
                                         File imageFile) {
        return sendEdit(token, name, email, phone, classRoll, regNo, year, section, imageFile);
    }

    // ===== Private helper method =====
    private static HTTP.Response sendEdit(String token, String name, String email, String phone,
                                          String classRoll, String regNo, String year, String section,
                                          File imageFile) {
        if (token == null || token.isEmpty()) {
            return new HTTP.Response(400, "{\"message\":\"Missing token\",\"fields\":[\"token\"]}");
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("token", token);
            if (name != null) params.put("name", name);
            if (email != null) params.put("email", email);
            if (phone != null) params.put("phone", phone);
            if (classRoll != null) params.put("class_roll", classRoll);
            if (regNo != null) params.put("reg_no", regNo);
            if (year != null) params.put("year", year);
            if (section != null) params.put("section", section);

            return HTTP.postMultipart(EDIT_URL, params, "image", imageFile);

        } catch (Exception e) {
            e.printStackTrace();
            return new HTTP.Response(-1, e.getMessage());
        }
    }
}