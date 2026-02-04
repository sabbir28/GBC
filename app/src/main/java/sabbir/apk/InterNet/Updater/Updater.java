package sabbir.apk.InterNet.Updater;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import sabbir.apk.InterNet.API.Thread.GitHubExecutor;
import sabbir.apk.InterNet.Deta.ReleaseInfo;

public final class Updater {

    private static final String API_URL =
            "https://api.github.com/repos/sabbir28/GBC/releases/latest";

    private static final String PREFS = "updater_prefs";
    private static final String KEY_HASH = "apk_sha256";

    private Updater() {}

    /* ---------- Callback ---------- */

    public interface UpdateCallback {
        void onSuccess(ReleaseInfo release);
        void onFailure(Exception e);
    }

    /* ---------- Local state ---------- */

    public static String getCurrentVersionHash(Context context) {
        return context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_HASH, "");
    }

    public static void saveCurrentVersionHash(Context context, String hash) {
        context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HASH, hash)
                .apply();
    }

    /* ---------- Network ---------- */

    public static void fetchLatestReleaseAsync(UpdateCallback callback) {
        GitHubExecutor.execute(
                () -> {
                    HttpURLConnection conn =
                            (HttpURLConnection) new URL(API_URL).openConnection();

                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10_000);
                    conn.setReadTimeout(10_000);

                    try (BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(conn.getInputStream()))) {

                        StringBuilder json = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            json.append(line);
                        }

                        JSONObject root = new JSONObject(json.toString());
                        JSONArray assets = root.getJSONArray("assets");

                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject asset = assets.getJSONObject(i);

                            if ("app-debug.apk".equals(asset.getString("name"))) {

                                String digest = asset.getString("digest");
                                String sha256 = normalizeSha256(digest);

                                JSONObject out = new JSONObject();
                                out.put("url",
                                        asset.getString("browser_download_url"));
                                out.put("sha256", sha256);

                                return out.toString();
                            }
                        }

                        throw new IllegalStateException("APK asset not found");

                    } finally {
                        conn.disconnect();
                    }
                },
                new GitHubExecutor.Callback() {

                    @Override
                    public void onSuccess(String result) {
                        try {
                            JSONObject json = new JSONObject(result);
                            callback.onSuccess(
                                    new ReleaseInfo(
                                            json.getString("url"),
                                            json.getString("sha256")
                                    )
                            );
                        } catch (Exception e) {
                            callback.onFailure(e);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onFailure(e);
                    }
                }
        );
    }

    private static String normalizeSha256(String digest) {
        // server gives: "sha256:abcdef..."
        return digest.startsWith("sha256:")
                ? digest.substring("sha256:".length())
                : digest;
    }

    /* ---------- Decision ---------- */

    public static boolean isUpdateRequired(String local, String remote) {
        return remote != null && !remote.equalsIgnoreCase(local);
    }

    /* ---------- Download ---------- */

    public static void downloadApk(Context context, String url) {
        DownloadManager.Request req =
                new DownloadManager.Request(Uri.parse(url));

        req.setTitle("App Update");
        req.setDescription("Downloading latest version");
        req.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        );
        req.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "app-debug.apk"
        );

        DownloadManager dm =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        if (dm != null) {
            dm.enqueue(req);
        }
    }

    /* ---------- Install ---------- */

    public static void requestInstall(Context context) {
        Uri apkUri = Uri.fromFile(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS + "/app-debug.apk"
                )
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(
                apkUri,
                "application/vnd.android.package-archive"
        );
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }
}
