package org.telegram.ui.Components.blur3;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Стиль контура «стеклянных» поверхностей.
 * GLARE(0), SOLID(1), HIDDEN(2);
 * порядок значений совпадает с {@code AppearanceConfig.glassOutlineStyle}.
 */
public enum GlassOutlineStyle {
    /** Блик: тонкая светлая линия сверху и тёмная снизу — как было до появления настройки. */
    GLARE,
    /** Сплошной контур цветом разделителя, тень выключена. */
    SOLID,
    /** Контура и тени нет. */
    HIDDEN;

    private static final Set<Listener> listeners = Collections.newSetFromMap(new WeakHashMap<>());

    // made with <3 from uzbekgram
    public interface Listener {
        void onGlassOutlineStyleChanged();
    }

    public static void addListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public static void dispatchChange() {
        final Listener[] snapshot;
        synchronized (listeners) {
            snapshot = listeners.toArray(new Listener[0]);
        }
        for (Listener listener : snapshot) {
            listener.onGlassOutlineStyleChanged();
        }
    }

    public static GlassOutlineStyle current() {
        return HIDDEN;
    }
}
