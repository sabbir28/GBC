package sabbir.apk.Analytics;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Map;

/**
 * Small helper to keep Firebase Analytics event logging consistent.
 */
public final class FirebaseEventLogger {

    private final FirebaseAnalytics analytics;

    public FirebaseEventLogger(@NonNull FirebaseAnalytics analytics) {
        this.analytics = analytics;
    }

    public void logSimpleEvent(@NonNull String eventName) {
        analytics.logEvent(eventName, new Bundle());
    }

    public void logStringEvent(@NonNull String eventName, @NonNull String key, @Nullable String value) {
        Bundle bundle = new Bundle();
        bundle.putString(key, value == null ? "" : value);
        analytics.logEvent(eventName, bundle);
    }

    public void logGitHubSync(@NonNull String step, boolean success, @Nullable String detail) {
        Bundle bundle = new Bundle();
        bundle.putString("step", step);
        bundle.putBoolean("success", success);
        bundle.putString("detail", detail == null ? "" : detail);
        analytics.logEvent("github_sync", bundle);
    }

    public void logMapEvent(@NonNull String eventName, @NonNull Map<String, String> map) {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        analytics.logEvent(eventName, bundle);
    }
}
