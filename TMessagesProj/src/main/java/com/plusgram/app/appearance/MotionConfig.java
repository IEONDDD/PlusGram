package com.plusgram.app.appearance;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import androidx.recyclerview.widget.RecyclerView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;

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
        if (!prefs().getBoolean("press", true) || !enabled()) {
            // A preference/system animation change must never leave a recycled row scaled.
            if (!pressed) { view.setScaleX(1f); view.setScaleY(1f); }
            return;
        }
        // Simple list cells only: chat/media transforms are owned by their own gestures.
        if (view instanceof TextSettingsCell || view instanceof TextCheckCell || view instanceof TextDetailSettingsCell
                || view instanceof TextCell || view instanceof NotificationsCheckCell
                || view instanceof org.telegram.ui.Cells.TextRadioCell
                || view instanceof org.telegram.ui.Cells.CheckBoxCell
                || view instanceof org.telegram.ui.Cells.TextCheckCell2) {
            animatePress(view, pressed);
        }
    }
    public static void animatePress(View view, boolean pressed) {
        if (!pressed || (prefs().getBoolean("press", true) && enabled())) {
            view.animate().scaleX(pressed ? .97f : 1f).scaleY(pressed ? .97f : 1f)
                    .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                    .setDuration(duration(pressed ? 100 : 260)).start();
        }
    }
    /** Adds feedback without consuming the control's click or scroll gestures. */
    public static void interactive(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: animatePress(v, true); break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: animatePress(v, false); break;
            }
            return false;
        });
    }
    /** One entrance per screen, limited to visible rows; scrolling never replays it. */
    public static void animateListEntrance(ViewGroup list) {
        list.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                if (list.getChildCount() == 0) return true;
                list.getViewTreeObserver().removeOnPreDrawListener(this);
                if (!enabled() || !prefs().getBoolean("entrance", true)) return true;
                for (int i = 0; i < Math.min(10, list.getChildCount()); i++) {
                    View child = list.getChildAt(i);
                    child.setAlpha(0f);
                    // Leave translations to RecyclerView's item animator.
                    child.animate().alpha(1f).setStartDelay(duration(i * 24L))
                            .setDuration(duration(280)).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                            .setUpdateListener(animation -> list.invalidate())
                            .withEndAction(() -> { child.animate().setStartDelay(0); child.animate().setUpdateListener(null); list.invalidate(); }).start();
                }
                return true;
            }
        });
        if (list instanceof RecyclerView) ((RecyclerView) list).addOnChildAttachStateChangeListener(
                new RecyclerView.OnChildAttachStateChangeListener() {
                    @Override public void onChildViewAttachedToWindow(View view) {}
                    @Override public void onChildViewDetachedFromWindow(View view) {
                        view.animate().cancel();
                        view.animate().setUpdateListener(null);
                        view.animate().setStartDelay(0);
                        view.setAlpha(1f); view.setScaleX(1f); view.setScaleY(1f);
                    }
                });
    }
    public static void feedback(View view) {
        if (prefs().getBoolean("haptics", true)) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }
}
