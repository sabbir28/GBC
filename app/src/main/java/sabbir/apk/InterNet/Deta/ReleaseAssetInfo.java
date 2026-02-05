package sabbir.apk.InterNet.Deta;


public final class ReleaseAssetInfo {

    public final String sha256;
    public final String downloadUrl;
    public final int downloadCount;
    public final String updatedAt;

    public ReleaseAssetInfo(
            String sha256,
            String downloadUrl,
            int downloadCount,
            String updatedAt
    ) {
        this.sha256 = sha256;
        this.downloadUrl = downloadUrl;
        this.downloadCount = downloadCount;
        this.updatedAt = updatedAt;
    }
}
