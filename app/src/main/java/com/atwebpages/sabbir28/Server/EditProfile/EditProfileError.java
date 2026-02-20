package com.atwebpages.sabbir28.Server.EditProfile;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handles edit profile error responses from server
 */
public class EditProfileError {

    private final String message;
    private final String[] fields;

    /**
     * Parse server JSON error response
     *
     * @param json JSON returned by server
     */
    public EditProfileError(String json) {
        String msg = "Unknown error";
        String[] flds = new String[0];

        if (json != null && !json.isEmpty()) {
            try {
                JSONObject obj = new JSONObject(json);
                if (obj.has("message")) msg = obj.getString("message");
                if (obj.has("fields")) {
                    JSONArray arr = obj.getJSONArray("fields");
                    flds = new String[arr.length()];
                    for (int i = 0; i < arr.length(); i++) flds[i] = arr.getString(i);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.message = msg;
        this.fields = flds;
    }

    /**
     * Main error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Fields that caused the error
     */
    public String[] getFields() {
        return fields;
    }

    /**
     * Check if a specific field caused the error
     */
    public boolean isFieldError(String field) {
        if (fields == null) return false;
        for (String f : fields) if (f.equalsIgnoreCase(field)) return true;
        return false;
    }

    /**
     * Check if token is missing or invalid
     */
    public boolean isTokenInvalid() {
        if (message == null) return false;
        String msg = message.toLowerCase();
        return msg.contains("invalid token") || msg.contains("missing token");
    }

    /**
     * Check if nothing to update error
     */
    public boolean isNothingToUpdate() {
        if (message == null) return false;
        return message.toLowerCase().contains("nothing to update");
    }
}