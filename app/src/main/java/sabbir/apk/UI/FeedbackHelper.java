package sabbir.apk.UI;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

/**
 * System-native sound and haptic feedback. Respects Do Not Disturb and system settings.
 */
public final class FeedbackHelper {

    private static final int SCALE_ANIM_DURATION_MS = 150;
    private static final float PRESS_SCALE = 0.97f;

    private FeedbackHelper() {}

    /**
     * Play system default notification sound. No-op if ringer is silent or DND.
     */
    public static void playSuccessSound(@NonNull Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        if (am.getRingerMode() == AudioManager.RINGER_MODE_SILENT) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && am.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0) return;

        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (uri == null) return;
            Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), uri);
            if (r != null) {
                r.play();
            }
        } catch (Exception ignored) {
            // Ignore if ringtone fails (e.g. no permission or invalid uri)
        }
    }

    /**
     * Light haptic for primary actions (e.g. submit, long press). Respects system vibration setting.
     */
    public static void performPrimaryHaptic(@NonNull View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    /**
     * Wrap a view with scale-down on press (0.97) and release (1.0). Animation is interruptible.
     */
    public static void applyButtonPressFeedback(@NonNull View view, @NonNull Runnable onPress) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(PRESS_SCALE)
                            .scaleY(PRESS_SCALE)
                            .setDuration(SCALE_ANIM_DURATION_MS)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(SCALE_ANIM_DURATION_MS)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
            }
            return false; // allow click to fire
        });
        view.setOnClickListener(v -> onPress.run());
    }

    /**
     * Apply press feedback and optional haptic on click. Use for primary actions (login, register, submit).
     */
    public static void setPrimaryButtonWithFeedback(@NonNull View button, @NonNull Runnable onClick) {
        applyButtonPressFeedback(button, () -> {
            performPrimaryHaptic(button);
            onClick.run();
        });
    }
}
