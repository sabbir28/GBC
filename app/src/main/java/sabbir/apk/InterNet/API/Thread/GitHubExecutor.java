package sabbir.apk.InterNet.API.Thread;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GitHubExecutor {

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private static final Handler MAIN =
            new Handler(Looper.getMainLooper());

    private GitHubExecutor() {}

    public static void execute(Task task, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                String result = task.run();
                MAIN.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                MAIN.post(() -> callback.onError(e));
            }
        });
    }

    public interface Task {
        String run() throws Exception;
    }

    public interface Callback {
        void onSuccess(String result);
        void onError(Exception e);
    }
}
