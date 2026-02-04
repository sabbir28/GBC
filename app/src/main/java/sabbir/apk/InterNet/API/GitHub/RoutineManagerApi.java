package sabbir.apk.InterNet.API.GitHub;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RoutineManagerApi
 *
 * Accountability:
 * - Validate remote SHA
 * - Cache routine.json locally
 * - Download only on change
 * - Guarantee file integrity
 *
 * Storage:
 * /data/data/<package>/files/github_cache/
 *  ├── routine.json
 *  └── routine.json.sha
 */
public final class RoutineManagerApi {

    private static final String TAG = "RoutineManagerApi";
    private static final String CACHE_DIR = "github_cache";
    private static final String ROUTINE_FILE = "routine.json";
    private static final String SHA_SUFFIX = ".sha";
    private static final String TMP_SUFFIX = ".tmp";

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private RoutineManagerApi() {
        // utility class
    }

    /* =========================
       Public API
       ========================= */

    public static void sync(Context context, JSONObject githubFileObject) {
        EXECUTOR.execute(() -> {
            try {
                String remoteSha = githubFileObject.getString("sha");
                String downloadUrl = githubFileObject.getString("download_url");

                String localSha = readLocalSha(context);

                if (remoteSha.equals(localSha)
                        && routineFileExists(context)) {
                    Log.d(TAG, "Routine cache is current");
                    return;
                }

                File downloaded = downloadRoutine(context, downloadUrl);

                promoteTempFile(context, downloaded);
                writeLocalSha(context, remoteSha);

                Log.d(TAG, "Routine sync completed successfully");

            } catch (Exception e) {
                Log.e(TAG, "Routine sync failed", e);
            }
        });
    }

    /**
     * Safe accessor for routine.json
     */
    @Nullable
    public static JSONObject readRoutine(Context context) {
        File file = getRoutineFile(context);

        if (!file.exists() || !file.canRead()) {
            Log.w(TAG, "Routine file unavailable");
            return null;
        }

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(file))) {

            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            return new JSONObject(json.toString());

        } catch (Exception e) {
            Log.e(TAG, "Routine read failed", e);
            return null;
        }
    }

    public static File getRoutineFile(Context context) {
        return new File(
                new File(context.getFilesDir(), CACHE_DIR),
                ROUTINE_FILE
        );
    }

    /* =========================
       Internal mechanics
       ========================= */

    private static File downloadRoutine(Context context, String urlString)
            throws Exception {

        HttpURLConnection conn = null;

        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);
            conn.connect();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException(
                        "HTTP " + conn.getResponseCode()
                );
            }

            File cacheDir = ensureCacheDir(context);
            File tmpFile = new File(cacheDir, ROUTINE_FILE + TMP_SUFFIX);

            try (
                    InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(tmpFile)
            ) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }

            return tmpFile;

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void promoteTempFile(Context context, File tmpFile) {
        File finalFile = getRoutineFile(context);

        if (finalFile.exists() && !finalFile.delete()) {
            throw new IllegalStateException("Old routine cleanup failed");
        }

        if (!tmpFile.renameTo(finalFile)) {
            throw new IllegalStateException("Atomic replace failed");
        }
    }

    public static boolean routineFileExists(Context context) {
        File file = getRoutineFile(context);
        return file.exists() && file.length() > 0;
    }

    private static File ensureCacheDir(Context context) {
        File dir = new File(context.getFilesDir(), CACHE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Cache dir creation failed");
        }
        return dir;
    }

    @Nullable
    private static String readLocalSha(Context context) {
        File shaFile = new File(
                ensureCacheDir(context),
                ROUTINE_FILE + SHA_SUFFIX
        );

        if (!shaFile.exists()) {
            return null;
        }

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(shaFile))) {
            return reader.readLine();
        } catch (Exception e) {
            Log.e(TAG, "SHA read failed", e);
            return null;
        }
    }

    private static void writeLocalSha(Context context, String sha) {
        File shaFile = new File(
                ensureCacheDir(context),
                ROUTINE_FILE + SHA_SUFFIX
        );

        try (FileWriter writer = new FileWriter(shaFile, false)) {
            writer.write(sha);
        } catch (Exception e) {
            Log.e(TAG, "SHA write failed", e);
        }
    }
}
