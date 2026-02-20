package com.atwebpages.sabbir28.Core;

import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {

    private static final String PREF_NAME = "user_pref";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_YEAR = "year";
    private static final String KEY_SECTION = "section";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_CLASS_ROLL = "class_roll";
    private static final String KEY_REG_NO = "reg_no";
    private static final String KEY_IMAGE_KEY = "image_key";
    private static final String KEY_IMAGE_BASE64 = "image_base64";

    private final SharedPreferences prefs;

    /** Initialize UserManager with application context */
    public UserManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** Save or update user information */
    public void saveUser(String name,
                         String email,
                         String year,
                         String section,
                         String phone,
                         String classRoll,
                         String regNo,
                         String imageKey,
                         String imageBase64) {
        prefs.edit()
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putString(KEY_YEAR, year)
                .putString(KEY_SECTION, section)
                .putString(KEY_PHONE, phone)
                .putString(KEY_CLASS_ROLL, classRoll)
                .putString(KEY_REG_NO, regNo)
                .putString(KEY_IMAGE_KEY, imageKey)
                .putString(KEY_IMAGE_BASE64, imageBase64)
                .apply();
    }

    /** Check if user data exists */
    public boolean hasUser() {
        return prefs.contains(KEY_NAME) && prefs.contains(KEY_EMAIL);
    }

    /** Clear all user data */
    public void clearUser() {
        prefs.edit().clear().apply();
    }

    /** Getters for user information */
    public String getUserName() {
        return prefs.getString(KEY_NAME, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getUserYear() {
        return prefs.getString(KEY_YEAR, null);
    }

    public String getUserSection() {
        return prefs.getString(KEY_SECTION, null);
    }

    public String getUserPhone() {
        return prefs.getString(KEY_PHONE, null);
    }

    public String getUserClassRoll() {
        return prefs.getString(KEY_CLASS_ROLL, null);
    }

    public String getUserRegNo() {
        return prefs.getString(KEY_REG_NO, null);
    }

    public String getUserImageKey() {
        return prefs.getString(KEY_IMAGE_KEY, null);
    }

    public String getUserImageBase64() {
        return prefs.getString(KEY_IMAGE_BASE64, null);
    }
}