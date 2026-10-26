package com.plusgram.app.appearance;

import android.widget.LinearLayout;
import org.telegram.messenger.*;
import org.telegram.ui.MainTabsLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Switch;
import xyz.nextalone.nagram.NaConfig;
import static org.telegram.messenger.LocaleController.getString;

public final class MaterialMotionActivity extends MaterialSettingsFragment {
    @Override protected int titleRes() { return R.string.PlusgramMotionTitle; }
    @Override protected void buildContent() {
        LinearLayout styles = card(R.string.PlusgramMotionMd3);
        row(styles, R.string.PlusgramMotionApply, getString(R.string.PlusgramMaterialYouPresetInfo), () -> {
            AppearanceConfig.newLoadingStyle.setConfigBool(true);
            AppearanceConfig.newChatHeaderStyle.setConfigBool(true);
            AppearanceConfig.newNavigationBarStyle.setConfigBool(true);
            AppearanceConfig.m3ListItems.setConfigBool(true);
            AppearanceConfig.md3Player.setConfigBool(true);
            AppearanceConfig.md3MiniPlayer.setConfigBool(true);
            AppearanceConfig.squareFab.setConfigBool(true);
            AppearanceConfig.iosNavigationBarStyle.setConfigBool(false);
            AppearanceConfig.iosChatHeader.setConfigBool(false);
            AppearanceConfig.sectionRadius.setConfigInt(24);
            NaConfig.INSTANCE.getSliderStyle().setConfigInt(2);
            NaConfig.INSTANCE.getSwitchStyle().setConfigInt(Switch.SWITCH_STYLE_M3);
            if (MainTabsLayout.isBottomNavigationFloating())
                MainTabsLayout.setBottomNavigationMode(MainTabsLayout.BOTTOM_NAVIGATION_MODE_SHOW);
            if (MaterialYouController.isAvailable()) MaterialYouController.apply(MaterialYouController.SYSTEM);
            refreshTheme();
        });
        String[] modes = {getString(R.string.PlusgramMaterialYouOff), getString(R.string.PlusgramMaterialYouSystem),
                getString(R.string.PlusgramMaterialYouLight), getString(R.string.PlusgramMaterialYouDark),
                getString(R.string.PlusgramMaterialYouAmoled)};
        row(styles, R.string.PlusgramMaterialYou, MaterialYouController.isAvailable()
                ? modes[MaterialYouController.currentMode()] : getString(R.string.PlusgramMaterialYouUnavailable), () -> {
            if (MaterialYouController.isAvailable()) choose(R.string.PlusgramMaterialYou, modes, index -> {
                MaterialYouController.apply(index); refreshTheme();
            });
        });
        row(styles, R.string.PlusgramMotionPalette,
                AppearanceConfig.monetStyle.Int() == 0 ? "Telemone · Material 3" : "Monet Classic", () ->
                choose(R.string.PlusgramMotionPalette, new String[]{"Telemone · Material 3", "Monet Classic"}, index -> {
                    int mode = MaterialYouController.currentMode();
                    AppearanceConfig.monetStyle.setConfigInt(index);
                    tw.nekomimi.nekogram.helpers.MonetHelper.invalidateCache();
                    if (mode != MaterialYouController.OFF) MaterialYouController.apply(mode);
                    refreshTheme();
                }));
        row(styles, R.string.PlusgramMotionShape, AppearanceConfig.sectionRadius() + " dp", () ->
                choose(R.string.PlusgramMotionShape, new String[]{"8 dp", "16 dp", "24 dp", "32 dp"}, index -> {
                    AppearanceConfig.sectionRadius.setConfigInt(new int[]{8,16,24,32}[index]); refreshTheme();
                }));
        toggle(styles, R.string.PlusgramMotionDensity, MotionConfig.prefs().getBoolean("comfortable", true), value -> {
            MotionConfig.prefs().edit().putBoolean("comfortable", value).apply(); refreshTheme();
        });
        LinearLayout motion = card(R.string.PlusgramMotionTitle);
        toggle(motion, R.string.PlusgramMotionAnimations, MotionConfig.prefs().getBoolean("enabled", true),
                value -> MotionConfig.prefs().edit().putBoolean("enabled", value).apply());
        row(motion, R.string.PlusgramMotionSpeed, MotionConfig.prefs().getInt("duration", 100) + "%", () ->
                choose(R.string.PlusgramMotionSpeed, new String[]{getString(R.string.PlusgramMotionFast),
                        getString(R.string.PlusgramMotionNormal),getString(R.string.PlusgramMotionSlow),
                        getString(R.string.PlusgramMotionSlower)}, index ->
                        MotionConfig.prefs().edit().putInt("duration", new int[]{50,100,150,200}[index]).apply()));
        toggle(motion, R.string.PlusgramMotionPress, MotionConfig.prefs().getBoolean("press", true),
                value -> MotionConfig.prefs().edit().putBoolean("press", value).apply());
        toggle(motion, R.string.PlusgramMotionHaptics, MotionConfig.prefs().getBoolean("haptics", true),
                value -> MotionConfig.prefs().edit().putBoolean("haptics", value).apply());
        row(motion, R.string.PlusgramMotionReset, null, () -> { MotionConfig.prefs().edit().clear().apply(); refreshTheme(); });
        footer(R.string.PlusgramMotionInfo);
    }
    private void refreshTheme() {
        AppearanceConfig.invalidateDividerStyle();
        Theme.applyCommonTheme();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false);
        if (scroll != null) scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        rebuild();
    }
}
