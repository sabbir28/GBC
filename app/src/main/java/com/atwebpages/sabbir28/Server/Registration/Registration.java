package com.atwebpages.sabbir28.Server.Registration;

import android.os.Handler;
import android.os.Looper;
import com.atwebpages.sabbir28.Core.HTTP;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Registration {

    public interface Callback {
        void onResponse(int statusCode, String responseBody);
    }

    private static final String REGISTER_URL = "http://sabbir28.atwebpages.com/bmc/index.php?action=register";

    /**
     * Send registration data to server
     *
     * @param name       Student name
     * @param email      Student email
     * @param password   Password
     * @param phone      Phone number
     * @param classRoll  Class roll
     * @param regNo      Registration number
     * @param year       Year (e.g., "1st")
     * @param section    Section (A/B/C)
     * @param imageFile  Image file (profile photo)
     * @param callback   Callback to handle server response
     */
    public static void registerAsync(String name, String email, String password,
                                     String phone, String classRoll, String regNo,
                                     String year, String section, File imageFile,
                                     Callback callback) {

        new android.os.AsyncTask<Void, Void, HTTP.Response>() {
            @Override
            protected HTTP.Response doInBackground(Void... voids) {
                return sendRegistration(name, email, password, phone, classRoll, regNo, year, section, imageFile);
            }

            @Override
            protected void onPostExecute(HTTP.Response response) {
                if (callback != null) callback.onResponse(response.code, response.body);
            }
        }.execute();
    }

    /**
     * Send registration data to server
     *
     * @param name       Student name
     * @param email      Student email
     * @param password   Password
     * @param phone      Phone number
     * @param classRoll  Class roll
     * @param regNo      Registration number
     * @param year       Year (e.g., "1st")
     * @param section    Section (A/B/C)
     * @param imageFile  Image file (profile photo)
     * @param callback   Callback to handle server response
     */
    public static void registerThread(String name, String email, String password,
                                      String phone, String classRoll, String regNo,
                                      String year, String section, File imageFile,
                                      Callback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            HTTP.Response response = sendRegistration(name, email, password, phone, classRoll, regNo, year, section, imageFile);
            mainHandler.post(() -> {
                if (callback != null) callback.onResponse(response.code, response.body);
            });
        }).start();
    }

    /**
     * Send registration data to server synchronously (blocking)
     *
     * @param name       Student name
     * @param email      Student email
     * @param password   Password
     * @param phone      Phone number
     * @param classRoll  Class roll
     * @param regNo      Registration number
     * @param year       Year (e.g., "1st")
     * @param section    Section (A/B/C)
     * @param imageFile  Image file (profile photo)
     * @return HTTP.Response containing status code and response body
     */
    public static HTTP.Response registerSync(String name, String email, String password,
                                             String phone, String classRoll, String regNo,
                                             String year, String section, File imageFile) {
        return sendRegistration(name, email, password, phone, classRoll, regNo, year, section, imageFile);
    }

    // ===== Private method used by all approaches =====
    private static HTTP.Response sendRegistration(String name, String email, String password,
                                                  String phone, String classRoll, String regNo,
                                                  String year, String section, File imageFile) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("name", name);
            params.put("email", email);
            params.put("password", password);
            params.put("phone", phone);
            params.put("class_roll", classRoll);
            params.put("reg_no", regNo);
            params.put("year", year);
            if (section != null) params.put("section", section);

            return HTTP.postMultipart(REGISTER_URL, params, "image", imageFile);
        } catch (Exception e) {
            e.printStackTrace();
            return new HTTP.Response(-1, e.getMessage());
        }
    }
}