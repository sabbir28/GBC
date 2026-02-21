package sabbir.apk;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.atwebpages.sabbir28.Auth;
import com.atwebpages.sabbir28.Core.TokenManager;
import com.atwebpages.sabbir28.Core.UserManager;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sabbir.apk.InterNet.API.GitHub.RoutineManagerApi;
import sabbir.apk.InterNet.API.Thread.GitHubExecutor;
import sabbir.apk.InterNet.Deta.GitHubApi;
import sabbir.apk.R;
import sabbir.apk.UI.Auth.LoginActivity;
import sabbir.apk.UI.HomeActivity;
import sabbir.apk.UI.NoInternetActivity;

public final class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final long MIN_SPLASH_MS = 1500L;

    private static final String GITHUB_OWNER = "sabbir28";
    private static final String GITHUB_REPO = "sabbir28.github.io";
    private static final String ROUTINE_PATH = "/BMC";
    private static final String ROUTINE_FILE = "rootine.json";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private FirebaseAnalytics analytics;
    private static TokenManager tokenManager;
    private static UserManager userManager;

    private boolean navigationHandled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        analytics = FirebaseAnalytics.getInstance(this);
        tokenManager = new TokenManager(this);
        userManager = new UserManager(this);
        logAppLaunch();

        long start = System.currentTimeMillis();

        if (isInternetAvailable(this)) {
            handleOnlineFlow();
        } else {
            handleOfflineFlow();
        }

        ensureMinimumSplash(start);
    }

    private void handleOnlineFlow() {
        String token = tokenManager.getToken();
        if (token == null) {
            goLogin();
            return;
        }

        Auth.verifyTokenAsync(token, (status, body) -> {
            if (status == 200) {
                Log.i(TAG, "Token valid");
                try {
                    assert body != null;
                    JSONObject json = new JSONObject(body);
                    userManager.saveUser(
                            json.optString("name"),
                            json.optString("email"),
                            json.optString("year"),
                            json.optString("section"),
                            json.optString("phone"),
                            json.optString("class_roll"),
                            json.optString("reg_no"),
                            json.optString("image_key"),
                            json.optString("image_base64")
                    );
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to save user details ! ");
                }
                triggerGitHubSync();
                navigateHomeDelayed();
            } else {
                Log.e(TAG, "Token invalid");
                goLogin();
            }
        });

        logNetworkStatus(true);
    }

    private void handleOfflineFlow() {
        logNetworkStatus(false);
        boolean exists = RoutineManagerApi.routineFileExists(getApplicationContext());
        if (!exists) {
            goNoInternet();
            return;
        }
        Toast.makeText(this, "No internet. Offline mode.", Toast.LENGTH_SHORT).show();
        navigateHomeDelayed();
    }

    private void goLogin() {
        if (navigationHandled) return;
        navigationHandled = true;
        startActivity(new Intent(this, LoginActivity.class));
        finish();
        overridePendingTransition(R.anim.activity_fade_slide_in, R.anim.activity_fade_slide_out);
    }

    private void goNoInternet() {
        if (navigationHandled) return;
        navigationHandled = true;
        startActivity(new Intent(this, NoInternetActivity.class));
        finish();
        overridePendingTransition(R.anim.activity_fade_slide_in, R.anim.activity_fade_slide_out);
    }

    private void navigateHomeDelayed() {
        if (navigationHandled) return;
        navigationHandled = true;
        handler.postDelayed(() -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            overridePendingTransition(R.anim.activity_fade_slide_in, R.anim.activity_fade_slide_out);
        }, MIN_SPLASH_MS);
    }

    private void ensureMinimumSplash(long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = Math.max(0, MIN_SPLASH_MS - elapsed);
        handler.postDelayed(() -> {
            if (!navigationHandled) navigateHomeDelayed();
        }, remaining);
    }

    private void triggerGitHubSync() {
        GitHubApi.fetchRepoInfo(GITHUB_OWNER, GITHUB_REPO, null, new GitHubExecutor.Callback() {
            @Override
            public void onSuccess(String result) {
                fetchRoutineFile();
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "GitHub metadata failed", e);
                logSync(false, "metadata_fail");
            }
        });
    }

    private void fetchRoutineFile() {
        GitHubApi.fetchRepoContents(GITHUB_OWNER, GITHUB_REPO, ROUTINE_PATH, null, new GitHubExecutor.Callback() {
            @Override
            public void onSuccess(String result) {
                processFiles(result);
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "GitHub contents failed", e);
                logSync(false, "contents_fail");
            }
        });
    }

    private void processFiles(@NonNull String json) {
        try {
            JSONArray files = new JSONArray(json);
            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.getJSONObject(i);
                if (!"file".equals(file.optString("type"))) continue;
                if (ROUTINE_FILE.equals(file.optString("name"))) {
                    RoutineManagerApi.sync(getApplicationContext(), file);
                    logSync(true, "routine_sync");
                    return;
                }
            }
            logSync(false, "routine_missing");
        } catch (Exception e) {
            Log.e(TAG, "Parsing failed", e);
            logSync(false, "parse_fail");
        }
    }

    private void logAppLaunch() {
        Bundle b = new Bundle();
        b.putString("source", "splash");
        analytics.logEvent("app_launch", b);
    }

    private void logNetworkStatus(boolean connected) {
        Bundle b = new Bundle();
        b.putString("status", connected ? "connected" : "disconnected");
        analytics.logEvent("network_status", b);
    }

    private void logSync(boolean success, String detail) {
        Bundle b = new Bundle();
        b.putBoolean("success", success);
        b.putString("detail", detail);
        analytics.logEvent("routine_sync", b);
    }

    public static boolean isInternetAvailable(@NonNull Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network net = cm.getActiveNetwork();
            if (net == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(net);
            return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}