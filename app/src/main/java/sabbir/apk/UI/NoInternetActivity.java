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

import sabbir.apk.MainActivity;
import sabbir.apk.R;

public final class NoInternetActivity extends AppCompatActivity {

    private static final String TAG = "NoInternetActivity";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        Button retryButton = findViewById(R.id.btn_retry);

        retryButton.setOnClickListener(v -> {
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

    /**
     * Redirects user back to Splash / Main entry.
     */
    private void navigateToSplash() {
        Log.d(TAG, "Internet restored → redirecting");

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Registers connectivity observer (API 21+ safe).
     */
    private void registerNetworkObserver() {
        connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    runOnUiThread(() -> navigateToSplash());
                }
            };

            connectivityManager.registerDefaultNetworkCallback(networkCallback);

        } else {
            NetworkRequest request = new NetworkRequest.Builder().build();

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    runOnUiThread(() -> navigateToSplash());
                }
            };

            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    private void unregisterNetworkObserver() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
                // Defensive: avoids IllegalArgumentException on double unregister
            }
        }
    }

    /**
     * Connectivity check (shared logic).
     */
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
