package com.plusgram.app.appearance;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.Theme;
import tw.nekomimi.nekogram.helpers.MonetHelper;

/** Selects the existing Monet palettes through Telegram's persistent day/night engine. */
public final class MaterialYouController {
    public static final int OFF = 0;
    public static final int SYSTEM = 1;
    public static final int LIGHT = 2;
    public static final int DARK = 3;
    public static final int AMOLED = 4;

    private MaterialYouController() {}

    public static boolean isAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public static int currentMode() {
        Theme.ThemeInfo active = Theme.getActiveTheme();
        if (active == null || !active.isMonet()) return OFF;
        if (Theme.selectedAutoNightType == Theme.AUTO_NIGHT_TYPE_SYSTEM
                && Theme.getCurrentTheme().isMonet()
                && Theme.getCurrentNightTheme() != null
                && Theme.getCurrentNightTheme().isMonet()) return SYSTEM;
        return active.isMonetAmoled() ? AMOLED : active.isMonetDark() ? DARK : LIGHT;
    }

    private static SharedPreferences preferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences("plusgram_material_you", 0);
    }

    public static void apply(int mode) {
        if (mode < OFF || mode > AMOLED || (!isAvailable() && mode != OFF)) return;
        if (mode == OFF) {
            if (currentMode() == OFF) return;
            SharedPreferences saved = preferences();
            Theme.ThemeInfo day = themeOrDefault(saved.getString("day", "Blue"), "Blue");
            Theme.ThemeInfo night = themeOrDefault(saved.getString("night", "Dark Blue"), "Dark Blue");
            int auto = saved.getInt("auto", Theme.AUTO_NIGHT_TYPE_NONE);
            boolean wasNight = saved.getBoolean("wasNight", false);
            setThemes(day, night, auto, wasNight);
            if (auto != Theme.AUTO_NIGHT_TYPE_NONE) Theme.checkAutoNightThemeConditions(true);
            return;
        }

        if (currentMode() == OFF) {
            Theme.ThemeInfo night = Theme.getCurrentNightTheme();
            preferences().edit()
                    .putString("day", Theme.getCurrentTheme().getKey())
                    .putString("night", night == null ? "Dark Blue" : night.getKey())
                    .putInt("auto", Theme.selectedAutoNightType)
                    .putBoolean("wasNight", Theme.isCurrentThemeNight()).apply();
        }
        Theme.ThemeInfo light = Theme.getTheme("Monet Light");
        Theme.ThemeInfo dark = Theme.getTheme(mode == AMOLED ? "Monet AMOLED" : "Monet Dark");
        if (light == null || dark == null) return;
        MonetHelper.invalidateCache();
        if (mode == SYSTEM) {
            boolean night = (ApplicationLoader.applicationContext.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            setThemes(light, dark, Theme.AUTO_NIGHT_TYPE_SYSTEM, night);
        } else {
            // A fixed dark theme is also the day theme, so auto-night cannot undo the selection.
            setThemes(mode == LIGHT ? light : dark, dark, Theme.AUTO_NIGHT_TYPE_NONE, false);
        }
    }

    private static Theme.ThemeInfo themeOrDefault(String key, String fallback) {
        Theme.ThemeInfo theme = Theme.getTheme(key);
        return theme != null ? theme : Theme.getTheme(fallback);
    }

    private static void setThemes(Theme.ThemeInfo day, Theme.ThemeInfo night, int auto, boolean useNight) {
        Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_NONE;
        Theme.cancelAutoNightThemeCallbacks();
        Theme.applyTheme(day, true, false);
        Theme.setCurrentNightTheme(night);
        Theme.selectedAutoNightType = auto;
        Theme.saveAutoNightThemeConfig();
        Theme.ThemeInfo active = useNight ? night : day;
        if (useNight) Theme.applyTheme(active, true, true);
        NotificationCenter.getGlobalInstance().postNotificationName(
                NotificationCenter.needSetDayNightTheme, active, useNight, null, -1);
    }
}
