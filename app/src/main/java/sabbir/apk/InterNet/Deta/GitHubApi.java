package sabbir.apk.InterNet.Deta;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

import sabbir.apk.InterNet.API.GitHub.GitHubClient;
import sabbir.apk.InterNet.API.Thread.GitHubExecutor;

/**
 * GitHub API façade
 * Handles repository metadata, contents, and file decoding
 */
public final class GitHubApi {

    /**
     * Fetch GitHub repository metadata
     */
    public static void fetchRepoInfo(
            String owner,
            String repo,
            String token,
            GitHubExecutor.Callback callback
    ) {
        GitHubExecutor.execute(
                () -> GitHubClient.get(
                        "/repos/" + owner + "/" + repo,
                        token
                ),
                callback
        );
    }

    /**
     * Fetch repository contents (files or directories)
     *
     * @param path empty or null means repo root
     */
    public static void fetchRepoContents(
            String owner,
            String repo,
            String path,
            String token,
            GitHubExecutor.Callback callback
    ) {
        String normalizedPath = (path == null || path.isEmpty())
                ? ""
                : "/" + path;

        String endpoint =
                "/repos/" + owner + "/" + repo + "/contents" + normalizedPath;

        GitHubExecutor.execute(
                () -> GitHubClient.get(endpoint, token),
                callback
        );
    }

    /**
     * Decode Base64-encoded GitHub file content into UTF-8 text
     */
    public static String decodeBase64File(String base64Content) {
        if (base64Content == null || base64Content.isEmpty()) {
            return "";
        }

        byte[] decodedBytes =
                Base64.decode(base64Content, Base64.DEFAULT);

        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}
