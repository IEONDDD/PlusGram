package com.plusgram.app.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.plusgram.app.appearance.MaterialMotionActivity;
import com.plusgram.app.appearance.MaterialSettingsFragment;

import org.telegram.messenger.BuildVars;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;

public final class PlusgramWhatsNewActivity extends MaterialSettingsFragment {
    public static void showOnce(BaseFragment host) {
        if (host.getFragmentView() == null || host.getParentActivity() == null) return;
        SharedPreferences preferences = host.getParentActivity().getSharedPreferences("plusgram_whats_new", Context.MODE_PRIVATE);
        String version = BuildVars.BUILD_VERSION_STRING;
        if (version.equals(preferences.getString("seen_version", null))) return;
        host.getFragmentView().post(() -> {
            INavigationLayout layout = host.getParentLayout();
            if (layout != null && layout.getLastFragment() == host
                    && !version.equals(preferences.getString("seen_version", null))
                    && host.presentFragment(new PlusgramWhatsNewActivity())) {
                preferences.edit().putString("seen_version", version).apply();
            }
        });
    }

    @Override protected int titleRes() {
        return R.string.PlusgramWhatsNew;
    }

    @Override protected void buildContent() {
        LinearLayout changes = card(0);
        TextView heading = text(LocaleController.formatString(R.string.PlusgramWhatsNewVersion,
                BuildVars.BUILD_VERSION_STRING), 23, Theme.key_windowBackgroundWhiteBlackText);
        heading.setTypeface(AndroidUtilities.bold());
        heading.setPadding(dp(20), dp(20), dp(20), dp(6));
        changes.addView(heading);
        row(changes, R.string.PlusgramMaterialExpressive, getString(R.string.PlusgramWhatsNewStyle),
                () -> presentFragment(new MaterialMotionActivity()));
        row(changes, R.string.PlusgramHeaderEffects, getString(R.string.PlusgramWhatsNewEffects),
                () -> presentFragment(new OpenExteraAppearanceActivity()));
        row(changes, R.string.PlusgramColorsBackground, getString(R.string.PlusgramWhatsNewBackground), null);
        row(changes, R.string.PlusgramDeveloperCouncil, getString(R.string.PlusgramWhatsNewCouncil),
                () -> presentFragment(new PlusgramDevelopersActivity()));
    }
}
