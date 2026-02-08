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

import org.json.JSONArray;
import org.json.JSONObject;

import sabbir.apk.InterNet.API.GitHub.RoutineManagerApi;
import sabbir.apk.InterNet.API.Thread.GitHubExecutor;
import sabbir.apk.InterNet.Deta.GitHubApi;
import sabbir.apk.UI.HomeActivity;
import sabbir.apk.UI.NoInternetActivity;

public final class MainActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    // Splash timing policy
    private static final long MIN_SPLASH_DURATION_MS = 1500L;

    // GitHub config
    private static final String GITHUB_OWNER = "sabbir28";
    private static final String GITHUB_REPO = "sabbir28.github.io";
    private static final String ROUTINE_PATH = "/BMC";
    private static final String ROUTINE_FILE = "rootine.json";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // splash layout with logo

        long splashStartTime = System.currentTimeMillis();

        if (isInternetAvailable(this)) {
            triggerBackgroundSync();
        } else {
            // Cheacking if file was alrady existing or not
            boolean file_exist = RoutineManagerApi.routineFileExists(getApplicationContext());
            if(!file_exist)
            {
                startActivity(new Intent(this, NoInternetActivity.class));
            }
            Toast.makeText(
                    this,
                    "No internet connection. Running in offline mode.",
                    Toast.LENGTH_SHORT
            ).show();
        }

        enforceMinimumSplashTime(splashStartTime);
    }

    /**
     * Ensures branding is visible for a minimum duration.
     * Does NOT wait for network completion.
     */
    private void enforceMinimumSplashTime(long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = Math.max(0, MIN_SPLASH_DURATION_MS - elapsed);

        mainHandler.postDelayed(this::navigateToHome, remaining);
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    /**
     * Background sync entry point.
     * Fire-and-forget by design.
     */
    private void triggerBackgroundSync() {
        GitHubApi.fetchRepoInfo(
                GITHUB_OWNER,
                GITHUB_REPO,
                null,
                new GitHubExecutor.Callback() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "Repo metadata fetched");
                        fetchRoutineFile();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Repo metadata fetch failed", e);
                    }
                }
        );
    }






    private void fetchRoutineFile() {
        GitHubApi.fetchRepoContents(
                GITHUB_OWNER,
                GITHUB_REPO,
                ROUTINE_PATH,
                null,
                new GitHubExecutor.Callback() {
                    @Override
                    public void onSuccess(String result) {
                        processRepoFiles(result);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Repo contents fetch failed", e);
                    }
                }
        );
    }

    private void processRepoFiles(@NonNull String json) {
        try {
            JSONArray files = new JSONArray(json);

            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.getJSONObject(i);

                if (!"file".equals(file.optString("type"))) {
                    continue;
                }

                if (ROUTINE_FILE.equals(file.optString("name"))) {
                    RoutineManagerApi.sync(getApplicationContext(), file);
                    Log.d(TAG, "Routine sync completed");
                    return;
                }
            }

            Log.w(TAG, "Routine file not found");

        } catch (Exception e) {
            Log.e(TAG, "Repo file parsing failed", e);
        }
    }

    /**
     * Reliable connectivity check (API 21+).
     */
    public static boolean isInternetAvailable(@NonNull Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        }

        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
