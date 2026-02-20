package com.atwebpages.sabbir28.Server.Registration;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handles registration error responses from server.
 * Parses JSON to determine the main issue and missing fields.
 */
public class RegistrationError {

    private final String message;
    private final String[] fields;

    /**
     * Constructor to parse server JSON response
     *
     * @param json JSON string returned by the server
     */
    public RegistrationError(String json) {
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
     * Check if server reported a missing field
     *
     * @param field Field name to check
     * @return true if missing
     */
    public boolean isFieldMissing(String field) {
        if (fields == null) return false;
        for (String f : fields) {
            if (f.equalsIgnoreCase(field)) return true;
        }
        return false;
    }
}