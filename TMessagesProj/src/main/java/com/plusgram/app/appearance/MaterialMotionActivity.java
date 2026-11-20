package com.plusgram.app.appearance;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.function.IntConsumer;
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
        LinearLayout presets = card(R.string.PlusgramMaterialStyle);
        boolean expressive = AppearanceConfig.isMaterialExpressive();
        LinearLayout buttons = new LinearLayout(context);
        buttons.setPadding(dp(12), dp(6), dp(12), dp(4));
        presets.addView(buttons);
        styleButton(buttons, R.string.PlusgramMaterialClassic, !expressive, false);
        styleButton(buttons, R.string.PlusgramMaterialExpressive, expressive, true);
        TextView styleDetail = text(getString(expressive ? R.string.PlusgramMaterialExpressiveInfo
                : R.string.PlusgramMaterialClassicInfo), 14, Theme.key_windowBackgroundWhiteGrayText);
        styleDetail.setPadding(dp(20), dp(4), dp(20), dp(16));
        presets.addView(styleDetail);

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
            NaConfig.INSTANCE.getBackAnimationStyle().setConfigInt(2);
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
        toggle(motion, R.string.PlusgramMotionEntrance, MotionConfig.prefs().getBoolean("entrance", true),
                value -> MotionConfig.prefs().edit().putBoolean("entrance", value).apply());
        toggle(motion, R.string.PlusgramMotionHaptics, MotionConfig.prefs().getBoolean("haptics", true),
                value -> MotionConfig.prefs().edit().putBoolean("haptics", value).apply());
        row(motion, R.string.PlusgramMotionReset, null, () -> {
            MotionConfig.prefs().edit().clear().apply();
            MaterialYouConfig.reset();
            applyColors();
        });
        footer(R.string.PlusgramMotionInfo);
    }

    private void styleButton(LinearLayout group, int label, boolean selected, boolean expressive) {
        boolean dark = Theme.isCurrentThemeDark();
        int background = selected ? (dark ? 0xFFE6BDEE : 0xFF62436E)
                : Theme.getColor(Theme.key_windowBackgroundWhite);
        int foreground = selected ? (dark ? 0xFF35203E : 0xFFFFFFFF)
                : Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
        TextView button = text(getString(label), 15, Theme.key_windowBackgroundWhiteBlackText);
        button.setTextColor(foreground);
        button.setTypeface(AndroidUtilities.bold());
        button.setGravity(Gravity.CENTER);
        button.setMinimumHeight(dp(48));
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        button.setSelected(selected);
        button.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(Theme.multAlpha(foreground, .14f)),
                Theme.createRoundRectDrawable(dp(24), background), null));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMarginStart(dp(3));
        params.setMarginEnd(dp(3));
        group.addView(button, params);
        button.setFocusable(true);
        button.setOnClickListener(v -> {
            if (AppearanceConfig.isMaterialExpressive() == expressive) return;
            MotionConfig.feedback(v);
            AppearanceConfig.materialExpressive.setConfigBool(expressive);
            refreshTheme();
            if (getParentLayout() != null) getParentLayout().rebuildAllFragmentViews(false, false);
        });
        MotionConfig.interactive(button);
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
        BackgroundPalette palette = BackgroundPalette.selected();
        if (palette == BackgroundPalette.CUSTOM) {
            return String.format(java.util.Locale.US, "#%06X", 0xFFFFFF & MaterialYouConfig.backgroundColor());
        }
        return getString(palette.label);
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
        if (getParentActivity() == null) return;
        BottomSheet sheet = new BottomSheet(context, false, getResourceProvider());
        sheet.setTitle(getString(R.string.PlusgramColorsBackground));
        ScrollView sheetScroll = new ScrollView(context);
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(12), dp(8), dp(12), dp(20));
        sheetScroll.addView(grid);
        boolean dark = Theme.isCurrentThemeDark();
        BackgroundPalette[] palettes = BackgroundPalette.values();
        for (int i = 0; i < palettes.length; i += 2) {
            LinearLayout pair = new LinearLayout(context);
            grid.addView(pair);
            for (int j = i; j < Math.min(i + 2, palettes.length); j++) {
                BackgroundPalette palette = palettes[j];
                TextView tile = text(getString(palette.label), 15, Theme.key_windowBackgroundWhiteBlackText);
                boolean selected = palette == BackgroundPalette.selected();
                Integer swatch = palette.colorAtTone(dark ? 20 : 92);
                int color = swatch == null ? Theme.getColor(Theme.key_windowBackgroundWhite) : swatch;
                boolean light = androidx.core.graphics.ColorUtils.calculateLuminance(color) > .50;
                int textColor = light ? 0xFF201A24 : 0xFFF7EDF8;
                android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
                shape.setCornerRadius(dp(22));
                shape.setColor(color);
                if (selected) shape.setStroke(dp(2), light ? 0xFF62436E : 0xFFE6BDEE);
                tile.setBackground(new android.graphics.drawable.RippleDrawable(
                        android.content.res.ColorStateList.valueOf(Theme.multAlpha(textColor, .15f)), shape, null));
                tile.setTextColor(textColor);
                tile.setGravity(Gravity.CENTER);
                tile.setMinimumHeight(dp(64));
                tile.setPadding(dp(8), dp(8), dp(8), dp(8));
                tile.setSelected(selected);
                tile.setFocusable(true);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
                params.setMargins(dp(4), dp(4), dp(4), dp(4));
                pair.addView(tile, params);
                tile.setOnClickListener(v -> {
                    sheet.dismiss();
                    if (palette == BackgroundPalette.CUSTOM) {
                        showColorSheet(R.string.PlusgramBackgroundCustom, MaterialYouConfig.backgroundColor(), colorChoice -> {
                            MaterialYouConfig.backgroundColor.setConfigInt(colorChoice);
                            selectBackground(palette);
                        });
                    } else {
                        selectBackground(palette);
                    }
                });
            }
        }
        sheet.setCustomView(sheetScroll);
        showDialog(sheet);
    }

    private void selectBackground(BackgroundPalette palette) {
        MaterialYouConfig.backgroundMode.setConfigInt(palette.ordinal());
        if (palette != BackgroundPalette.SYSTEM && MaterialYouController.isAvailable()
                && MaterialYouController.currentMode() == MaterialYouController.OFF) {
            MaterialYouController.apply(MaterialYouController.SYSTEM);
        }
        applyColors();
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
        showColorSheet(R.string.PlusgramColorsCustom, MaterialYouConfig.accentColor(), color -> {
            MaterialYouConfig.accentMode.setConfigInt(MaterialYouConfig.ACCENT_CUSTOM);
            MaterialYouConfig.accentColor.setConfigInt(color);
            applyColors();
        });
    }

    private void showColorSheet(int title, int initialColor, IntConsumer save) {
        Context context = getParentActivity();
        if (context == null) return;
        int[] pendingColor = {initialColor};
        ColorPicker colorPicker = new ColorPicker(context, false, (color, num, applyNow) -> {
            pendingColor[0] = color;
        }) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, android.view.View.MeasureSpec.makeMeasureSpec(
                        AndroidUtilities.dp(300), android.view.View.MeasureSpec.EXACTLY));
            }
        };
        colorPicker.setColor(initialColor, 0);
        colorPicker.setType(-1, true, 1, 1, false, 0, false);
        BottomSheet bottomSheet = new BottomSheet(context, false, getResourceProvider());
        bottomSheet.setTitle(getString(title));
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
            save.accept(pendingColor[0]);
        });
        sheetLayout.addView(done, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 16, 8, 16, 8));
        bottomSheet.setCustomView(sheetLayout);
        showDialog(bottomSheet);
    }

    private void refreshTheme() {
        AppearanceConfig.invalidateDividerStyle();
        Theme.applyCommonTheme();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false);
        if (scroll != null) scroll.setBackgroundColor(MaterialExperience.background(getResourceProvider()));
        rebuild();
    }
}
