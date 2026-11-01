package com.plusgram.app.appearance;

import android.content.SharedPreferences;
import androidx.core.graphics.ColorUtils;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Switch;
import xyz.nextalone.nagram.NaConfig;

/** One-time Material You defaults; subsequent user choices stay authoritative. */
public final class MaterialExperience {
    private MaterialExperience() {}
    public static void initialize() {
        SharedPreferences state = ApplicationLoader.applicationContext.getSharedPreferences("plusgram_material_experience", 0);
        if (state.getInt("version", 0) >= 4) return;
        AppearanceConfig.newLoadingStyle.setConfigBool(true);
        AppearanceConfig.newChatHeaderStyle.setConfigBool(true);
        AppearanceConfig.newNavigationBarStyle.setConfigBool(true);
        AppearanceConfig.m3ListItems.setConfigBool(true);
        AppearanceConfig.md3Player.setConfigBool(true);
        AppearanceConfig.md3MiniPlayer.setConfigBool(true);
        AppearanceConfig.squareFab.setConfigBool(true);
        AppearanceConfig.iosNavigationBarStyle.setConfigBool(false);
        AppearanceConfig.iosChatHeader.setConfigBool(false);
        AppearanceConfig.sectionRadius.setConfigInt(28);
        NaConfig.INSTANCE.getSliderStyle().setConfigInt(2);
        NaConfig.INSTANCE.getSwitchStyle().setConfigInt(Switch.SWITCH_STYLE_M3);
        NaConfig.INSTANCE.getBackAnimationStyle().setConfigInt(2);
        if (MaterialYouController.isAvailable()) MaterialYouController.apply(MaterialYouController.SYSTEM);
        ApplicationLoader.applicationContext.deleteSharedPreferences("plusgram_badges_prefs");
        state.edit().putInt("version", 4).apply();
    }
    /** Material surface container derived from the active palette in every theme. */
    public static int surface(Theme.ResourcesProvider provider) {
        return ColorUtils.blendARGB(Theme.getColor(Theme.key_windowBackgroundWhite, provider),
                Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, provider), Theme.isCurrentThemeDark() ? .08f : .035f);
    }
}
