package com.atwebpages.sabbir28.Core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * TokenManager handles storing, retrieving, and clearing
 * the user's authentication token using SharedPreferences.
 */
public class TokenManager {

    private static final String PREF_NAME = "auth_token_pref";
    private static final String KEY_TOKEN = "auth_token";

    private final SharedPreferences prefs;

    /** Initialize TokenManager with app context */
    public TokenManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** Save or update the token */
    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    /** Get the saved token, or null if none */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /** Check if token exists */
    public boolean hasToken() {
        return getToken() != null;
    }

    /** Clear the token (logout) */
    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }
}