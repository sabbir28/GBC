package sabbir.apk.InterNet.Updater;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

import sabbir.apk.InterNet.API.Thread.GitHubExecutor;
import sabbir.apk.InterNet.Deta.ReleaseAssetInfo;

public final class Updater {

    private static final String API_URL = "https://api.github.com/repos/sabbir28/GBC/releases/latest";

    private static final String PREFS = "updater_prefs";
    private static final String KEY_HASH = "apk_sha256";

    private static final int MAX_IGNORE_COUNT = 3;
    private static final long IGNORE_COOLDOWN_MS = 24L * 60 * 60 * 1000; // 24 hours
    /** Minimum age of update (ms) before showing the dialog. */
    public static final long MIN_AGE_TO_SHOW_MS = 2L * 60 * 60 * 1000; // 2 hours
    /** Minimum age of update (ms) before we may force the user to update. */
    private static final long MIN_AGE_TO_FORCE_MS = 2L * 24 * 60 * 60 * 1000; // 2 days
    private static final String KEY_IGNORE_COUNT = "ignore_count";
    private static final String KEY_LAST_IGNORE_TIME = "last_ignore_time";

    /**
     * Returns the age of the update in milliseconds, or -1 if unknown (null/empty or parse failure).
     */
    public static long getUpdateAgeMillis(String updatedAt) {
        if (updatedAt == null || updatedAt.trim().isEmpty()) {
            return -1L;
        }
        try {
            Instant updated = Instant.parse(updatedAt.trim());
            long ageMs = Duration.between(updated, Instant.now()).toMillis();
            return ageMs < 0 ? -1L : ageMs;
        } catch (Exception e) {
            return -1L;
        }
    }

    public interface Sha256Callback {
        void onSuccess(String sha256);
        void onFailure(Exception e);
    }

    public static void getInstalledApkSha256Async(
            Context context,
            Sha256Callback callback
    ) {
        GitHubExecutor.execute(
                () -> {
                    String apkPath = context.getApplicationInfo().sourceDir;

                    try (InputStream is = new FileInputStream(apkPath)) {
                        MessageDigest digest =
                                MessageDigest.getInstance("SHA-256");

                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) > 0) {
                            digest.update(buffer, 0, read);
                        }

                        byte[] hash = digest.digest();
                        StringBuilder hex =
                                new StringBuilder(hash.length * 2);

                        for (byte b : hash) {
                            hex.append(String.format("%02x", b));
                        }

                        return hex.toString();

                    } catch (Exception e) {
                        throw e;
                    }
                },
                new GitHubExecutor.Callback() {

                    @Override
                    public void onSuccess(String result) {
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onFailure(e);
                    }
                }
        );
    }


    public interface ReleaseAssetCallback {
        void onSuccess(ReleaseAssetInfo info);
        void onFailure(Exception e);
    }

    public static void fetchLatestApkAssetAsync(
            ReleaseAssetCallback callback
    ) {
        GitHubExecutor.execute(
                () -> {
                    HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();

                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10_000);
                    conn.setReadTimeout(10_000);

                    try (BufferedReader reader = new BufferedReader(
                                         new InputStreamReader(
                                                 conn.getInputStream()
                                         )
                                 )) {

                        StringBuilder json = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            json.append(line);
                        }

                        JSONObject root =
                                new JSONObject(json.toString());

                        JSONArray assets =
                                root.getJSONArray("assets");

                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject asset =
                                    assets.getJSONObject(i);

                            if (!asset.getString("name")
                                    .endsWith(".apk")) {
                                continue;
                            }

                            String digest =
                                    asset.optString("digest", "");

                            if (!digest.startsWith("sha256:")) {
                                continue;
                            }

                            String sha256 =
                                    digest.replace("sha256:", "")
                                            .trim();

                            return new JSONObject()
                                    .put("sha256", sha256)
                                    .put(
                                            "url",
                                            asset.optString(
                                                    "browser_download_url",
                                                    null
                                            )
                                    )
                                    .put(
                                            "download_count",
                                            asset.optInt(
                                                    "download_count",
                                                    0
                                            )
                                    )
                                    .put(
                                            "updated_at",
                                            asset.optString(
                                                    "updated_at",
                                                    null
                                            )
                                    )
                                    .toString();
                        }

                        throw new IllegalStateException(
                                "No APK asset with SHA256 found"
                        );

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
                                    new ReleaseAssetInfo(
                                            json.getString("sha256"),
                                            json.optString("url", null),
                                            json.optInt("download_count", 0),
                                            json.optString("updated_at", null)
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


    public static boolean canIgnoreUpdate(Context context) {
        return canIgnoreUpdate(context, null);
    }

    /**
     * Returns true if the user may dismiss the update (Remind Later). If updateUpdatedAt is
     * non-null and the update is less than 2 days old, the user can always dismiss. Otherwise
     * the existing ignore count and cooldown apply.
     */
    public static boolean canIgnoreUpdate(Context context, String updateUpdatedAt) {
        long ageMillis = getUpdateAgeMillis(updateUpdatedAt);
        if (ageMillis >= 0 && ageMillis < MIN_AGE_TO_FORCE_MS) {
            return true;
        }

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        int count = prefs.getInt(KEY_IGNORE_COUNT, 0);
        long lastTime = prefs.getLong(KEY_LAST_IGNORE_TIME, 0L);

        if (count >= MAX_IGNORE_COUNT) {
            return false;
        }

        if (lastTime == 0L) {
            return true;
        }

        long now = System.currentTimeMillis();
        return (now - lastTime) >= IGNORE_COOLDOWN_MS;
    }

    public static void recordIgnore(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        int count = prefs.getInt(KEY_IGNORE_COUNT, 0);

        prefs.edit()
                .putInt(KEY_IGNORE_COUNT, count + 1)
                .putLong(KEY_LAST_IGNORE_TIME, System.currentTimeMillis())
                .apply();
    }

    public static void resetIgnoreState(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_IGNORE_COUNT)
                .remove(KEY_LAST_IGNORE_TIME)
                .apply();
    }





}
