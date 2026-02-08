package sabbir.apk.InterNet.Deta;

import java.io.File;

public interface DownloadListener {

    void onProgress(
            long downloadedBytes,
            long totalBytes,
            int percent,
            long remainingBytes
    );

    void onCompleted(File file);

    void onError(Exception e);
}
