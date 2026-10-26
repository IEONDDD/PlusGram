package com.plusgram.app.appearance;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.telegram.messenger.*;
import org.telegram.ui.MainTabsLayout;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ColorPicker;
import org.telegram.ui.Components.LayoutHelper;
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

        LinearLayout colors = card(R.string.PlusgramColorsTitle);
        row(colors, R.string.PlusgramColorsAccent, accentLabel(), () -> chooseAccent());
        row(colors, R.string.PlusgramColorsBackground, backgroundLabel(), () -> chooseBackground());
        footer(R.string.PlusgramColorsInfo);

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
        row(motion, R.string.PlusgramMotionReset, null, () -> {
            MotionConfig.prefs().edit().clear().apply();
            MaterialYouConfig.reset();
            applyColors();
        });
        footer(R.string.PlusgramMotionInfo);
    }
    private static final int[] ACCENT_PRESETS = {
            0xFF4E6EF2, 0xFF6750A4, 0xFFB3261E, 0xFFE07A1F, 0xFFF2B705,
            0xFF3B873E, 0xFF00897B, 0xFF00A8C8, 0xFF1976D2, 0xFFE91E63,
            0xFF9C27B0, 0xFF795548
    };

    private String accentLabel() {
        if (MaterialYouConfig.accentMode() == MaterialYouConfig.ACCENT_CUSTOM) {
            return String.format(java.util.Locale.US, "#%06X", 0xFFFFFF & MaterialYouConfig.accentColor());
        }
        return getString(R.string.PlusgramColorsAccentSystem);
    }

    private String backgroundLabel() {
        switch (MaterialYouConfig.backgroundMode()) {
            case MaterialYouConfig.BACKGROUND_GRAY:
                return getString(R.string.PlusgramColorsBackgroundGray);
            case MaterialYouConfig.BACKGROUND_BLACK:
                return getString(R.string.PlusgramColorsBackgroundBlack);
            default:
                return getString(R.string.PlusgramColorsBackgroundSystem);
        }
    }

    private void applyColors() {
        tw.nekomimi.nekogram.helpers.MonetHelper.invalidateCache();
        int mode = MaterialYouController.currentMode();
        if (mode != MaterialYouController.OFF && MaterialYouController.isAvailable()) {
            MaterialYouController.apply(mode);
        }
        refreshTheme();
    }

    private void chooseBackground() {
        choose(R.string.PlusgramColorsBackground, new String[]{
                getString(R.string.PlusgramColorsBackgroundSystem),
                getString(R.string.PlusgramColorsBackgroundGray),
                getString(R.string.PlusgramColorsBackgroundBlack)
        }, index -> {
            MaterialYouConfig.backgroundMode.setConfigInt(index);
            applyColors();
        });
    }

    private void chooseAccent() {
        Context context = getParentActivity();
        if (context == null) return;

        final AlertDialog[] holder = new AlertDialog[1];

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView system = new TextView(context);
        system.setText(getString(R.string.PlusgramColorsAccentSystem));
        system.setTextSize(16);
        system.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        system.setGravity(Gravity.CENTER_VERTICAL);
        system.setPadding(dp(24), 0, dp(24), 0);
        system.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ROUNDRECT_6DP));
        system.setOnClickListener(v -> {
            MaterialYouConfig.accentMode.setConfigInt(MaterialYouConfig.ACCENT_SYSTEM);
            if (holder[0] != null) holder[0].dismiss();
            applyColors();
        });
        content.addView(system, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(24), dp(8), dp(24), dp(8));
        content.addView(grid, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout row = null;
        for (int i = 0; i < ACCENT_PRESETS.length; i++) {
            if (i % 6 == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                grid.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, dp(8)));
            }
            final int color = ACCENT_PRESETS[i];
            FrameLayout cell = new FrameLayout(context);
            View swatch = new View(context);
            android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(color);
            if (MaterialYouConfig.accentMode() == MaterialYouConfig.ACCENT_CUSTOM && MaterialYouConfig.accentColor() == color) {
                circle.setStroke(dp(2), Theme.getColor(Theme.key_dialogTextBlack));
            }
            swatch.setBackground(circle);
            cell.addView(swatch, LayoutHelper.createFrame(30, 30, Gravity.CENTER));
            if (row != null) {
                row.addView(cell, LayoutHelper.createLinear(0, 36, 1.0f));
            }
            cell.setOnClickListener(v -> {
                MaterialYouConfig.accentMode.setConfigInt(MaterialYouConfig.ACCENT_CUSTOM);
                MaterialYouConfig.accentColor.setConfigInt(color);
                if (holder[0] != null) holder[0].dismiss();
                applyColors();
            });
        }

        TextView custom = new TextView(context);
        custom.setText(getString(R.string.PlusgramColorsCustom));
        custom.setTextSize(16);
        custom.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        custom.setGravity(Gravity.CENTER_VERTICAL);
        custom.setPadding(dp(24), 0, dp(24), 0);
        custom.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ROUNDRECT_6DP));
        custom.setOnClickListener(v -> {
            if (holder[0] != null) holder[0].dismiss();
            showAccentColorSheet();
        });
        content.addView(custom, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider())
                .setTitle(getString(R.string.PlusgramColorsAccent))
                .setView(content)
                .setNegativeButton(getString(R.string.Cancel), null);
        holder[0] = builder.create();
        showDialog(holder[0]);
    }

    private void showAccentColorSheet() {
        Context context = getParentActivity();
        if (context == null) return;
        ColorPicker colorPicker = new ColorPicker(context, false, (color, num, applyNow) -> {
            MaterialYouConfig.accentMode.setConfigInt(MaterialYouConfig.ACCENT_CUSTOM);
            MaterialYouConfig.accentColor.setConfigInt(color);
        }) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, android.view.View.MeasureSpec.makeMeasureSpec(
                        AndroidUtilities.dp(300), android.view.View.MeasureSpec.EXACTLY));
            }
        };
        colorPicker.setColor(MaterialYouConfig.accentColor(), 0);
        colorPicker.setType(-1, true, 1, 1, false, 0, false);
        BottomSheet bottomSheet = new BottomSheet(context, false, getResourceProvider());
        LinearLayout sheetLayout = new LinearLayout(context);
        sheetLayout.setOrientation(LinearLayout.VERTICAL);
        sheetLayout.addView(colorPicker, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        TextView done = new TextView(context);
        done.setText(getString(R.string.Save));
        done.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        done.setTypeface(AndroidUtilities.bold());
        done.setGravity(Gravity.CENTER);
        done.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        done.setBackground(Theme.createSelectorDrawable(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4), 0.12f), Theme.RIPPLE_MASK_ROUNDRECT_6DP));
        done.setOnClickListener(v -> {
            bottomSheet.dismiss();
            applyColors();
        });
        sheetLayout.addView(done, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 16, 8, 16, 8));
        bottomSheet.setCustomView(sheetLayout);
        showDialog(bottomSheet);
    }

    private void refreshTheme() {
        AppearanceConfig.invalidateDividerStyle();
        Theme.applyCommonTheme();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false);
        if (scroll != null) scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        rebuild();
    }
}
