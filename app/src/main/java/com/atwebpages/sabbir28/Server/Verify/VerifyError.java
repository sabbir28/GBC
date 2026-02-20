package com.atwebpages.sabbir28.Server.Verify;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handles verify token error responses from server.
 * Parses JSON to determine the main issue and problematic fields.
 */
public class VerifyError {

    private final String message;
    private final String[] fields;

    /**
     * Constructor to parse server JSON response
     *
     * @param json JSON string returned by the server
     */
    public VerifyError(String json) {
        String msg = "Unknown error";
        String[] flds = new String[0];

        if (json != null && !json.isEmpty()) {
            try {
                JSONObject obj = new JSONObject(json);

                // Extract "message" if present
                if (obj.has("message")) msg = obj.getString("message");

                // Extract "fields" array if present
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
     * Get main error message
     *
     * @return Human-readable error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get fields that caused the error
     *
     * @return Array of field names
     */
    public String[] getFields() {
        return fields;
    }

    /**
     * Check if a specific field was reported missing or invalid
     *
     * @param field Field name to check
     * @return true if missing/invalid
     */
    public boolean isFieldMissing(String field) {
        if (fields == null) return false;
        for (String f : fields) {
            if (f.equalsIgnoreCase(field)) return true;
        }
        return false;
    }

    /**
     * Check if the token is invalid or expired
     *
     * @return true if token is invalid/expired
     */
    public boolean isInvalidToken() {
        if (message == null) return false;
        String msg = message.toLowerCase();
        return msg.contains("session expired") || msg.contains("invalid token");
    }

    /**
     * Check if the token was missing in the request
     *
     * @return true if missing
     */
    public boolean isTokenMissing() {
        if (fields == null) return false;
        for (String f : fields) {
            if (f.equalsIgnoreCase("token")) return true;
        }
        return false;
    }
}