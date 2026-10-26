package com.plusgram.app.appearance;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.view.HapticFeedbackConstants;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;

/** Motion preferences shared by fragment navigation and settings controls. */
public final class MotionConfig {
    private MotionConfig() {}
    public static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("plusgram_motion", 0);
    }
    public static boolean enabled() {
        return prefs().getBoolean("enabled", true) &&
                (Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled());
    }
    public static int duration(long milliseconds) {
        if (!enabled()) return 0;
        float factor = Math.max(50, Math.min(200, prefs().getInt("duration", 100))) / 100f;
        return Math.round(milliseconds * factor);
    }
    public static int rowHeight() { return prefs().getBoolean("comfortable", true) ? 72 : 56; }
    public static void press(View view, boolean pressed) {
        if (!prefs().getBoolean("press", true) || !enabled()) return;
        // Only simple settings cells: chat/media transforms are owned by their own gestures.
        if (view instanceof TextSettingsCell || view instanceof TextCheckCell || view instanceof TextDetailSettingsCell) {
            view.animate().scaleX(pressed ? .98f : 1f).scaleY(pressed ? .98f : 1f)
                    .setDuration(duration(pressed ? 100 : 180)).start();
        }
    }
    public static void feedback(View view) {
        if (prefs().getBoolean("haptics", true)) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }
}
