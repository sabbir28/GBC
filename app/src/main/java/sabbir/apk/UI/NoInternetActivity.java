package sabbir.apk.UI;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.analytics.FirebaseAnalytics;

import sabbir.apk.MainActivity;
import sabbir.apk.R;

public final class NoInternetActivity extends AppCompatActivity {

    private static final String TAG = "NoInternetActivity";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        logScreenView();

        Button retryButton = findViewById(R.id.btn_retry);

        retryButton.setOnClickListener(v -> {
            logRetryClick();
            if (isInternetAvailable(this)) {
                navigateToSplash();
            } else {
                Toast.makeText(
                        this,
                        "Still offline. Please check your connection.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        registerNetworkObserver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterNetworkObserver();
    }

    private void navigateToSplash() {
        Log.d(TAG, "Internet restored → redirecting");
        logInternetRestored();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void registerNetworkObserver() {
        connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(NoInternetActivity.this::navigateToSplash);
            }
        };

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                NetworkRequest request = new NetworkRequest.Builder().build();
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
        } catch (Exception ignored) {
            // Defensive: avoid crashes if registration fails
        }
    }

    private void unregisterNetworkObserver() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
        }
    }

    private void logScreenView() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "NoInternetActivity");
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, getClass().getSimpleName());
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    private void logRetryClick() {
        Bundle bundle = new Bundle();
        bundle.putString("action", "retry_button_click");
        mFirebaseAnalytics.logEvent("no_internet_action", bundle);
    }

    private void logInternetRestored() {
        Bundle bundle = new Bundle();
        bundle.putString("action", "internet_restored");
        mFirebaseAnalytics.logEvent("no_internet_action", bundle);
    }

    private static boolean isInternetAvailable(@NonNull Context context) {
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
