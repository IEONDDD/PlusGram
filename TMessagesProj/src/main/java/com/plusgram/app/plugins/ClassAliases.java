package com.plusgram.app.plugins;

public final class ClassAliases {

    public static final String ROOT = "com.exteragram.messenger";

    public static final String MEDIA_ROOT = "com.google.android.exoplayer2";

    private static final String[] ROOTS = {ROOT, MEDIA_ROOT};

    private static final String[][] EXACT = {
            {"com.exteragram.messenger.utils.chats.ChatUtils", "com.exteragram.messenger.utils.chats.ChatUtils"},
            {"com.exteragram.messenger.utils.ChatUtils", "com.exteragram.messenger.utils.chats.ChatUtils"},
            {"com.exteragram.messenger.utils.text.LocaleUtils", "com.exteragram.messenger.utils.text.LocaleUtils"},
            {"com.exteragram.messenger.utils.LocaleUtils", "com.exteragram.messenger.utils.text.LocaleUtils"},
            {"com.exteragram.messenger.utils.AppUtils", "com.exteragram.messenger.utils.AppUtils"},
            {"com.exteragram.messenger.R", "org.telegram.messenger.R"},
            {"com.exteragram.messenger.utils.system.VibratorUtils", "com.exteragram.messenger.utils.system.VibratorUtils"},
            {"com.exteragram.messenger.ai.AiConfig", "com.plusgram.app.ai.AiConfig"},
            {"com.exteragram.messenger.ai.AiController", "com.exteragram.messenger.ai.AiController"},
            {"com.exteragram.messenger.ai.ui.ResponseAlert", "com.exteragram.messenger.ai.ui.ResponseAlert"},
            {"com.exteragram.messenger.ai.ui.GenerateFromMessageBottomSheet", "com.exteragram.messenger.ai.ui.GenerateFromMessageBottomSheet"},
            {"com.exteragram.messenger.plugins.ui.components.InstallPluginBottomSheet", "com.exteragram.messenger.plugins.ui.components.InstallPluginBottomSheet"},
            {"com.exteragram.messenger.utils.system.SystemUtils", "com.exteragram.messenger.utils.system.SystemUtils"},
            {"com.exteragram.messenger.utils.SystemUtils", "com.exteragram.messenger.utils.system.SystemUtils"},
            {"com.exteragram.messenger.preferences.MainPreferencesActivity", "com.plusgram.app.settings.OpenExteraSettingsActivity"},
            {"com.exteragram.messenger.preferences.GeneralPreferencesActivity", "com.plusgram.app.settings.OpenExteraGeneralActivity"},
            {"com.exteragram.messenger.preferences.AppearancePreferencesActivity", "com.plusgram.app.settings.OpenExteraAppearanceActivity"},
            {"com.exteragram.messenger.preferences.ChatsPreferencesActivity", "com.plusgram.app.settings.OpenExteraChatsActivity"},
            {"com.exteragram.messenger.preferences.OtherPreferencesActivity", "com.plusgram.app.settings.OpenExteraOtherActivity"},
            {"com.exteragram.messenger.preferences.AppNavigationPreferencesActivity", "com.plusgram.app.settings.OpenExteraAppNavigationActivity"},
            {"com.exteragram.messenger.preferences.BasePreferencesActivity", "com.exteragram.messenger.preferences.BasePreferencesActivity"},
            {"com.exteragram.messenger.preferences.components.AltSeekbar", "com.plusgram.app.appearance.AltSeekbar"},
            {"com.exteragram.messenger.utils.chats.MainMenuHelper", "com.plusgram.app.drawer.MainMenuHelper"},
            {"com.exteragram.messenger.icons.ui.IconPacksActivity", "com.plusgram.app.icons.IconPacksActivity"},
            {"com.exteragram.messenger.pillstack.ui.pills.crypto.utils.ColoredBackground", "com.plusgram.app.pillstack.pills.ColoredBackground"},
            {"com.exteragram.messenger.pillstack.ui.pills.weather.WeatherPill", "com.plusgram.app.pillstack.pills.WeatherPill"},
            {"com.exteragram.messenger.pillstack.ui.PillStackPreferencesActivity", "com.plusgram.app.pillstack.PillStackSettingsActivity"},
            {"com.exteragram.messenger.pillstack.ui.PillStackLayout", "com.plusgram.app.pillstack.PillStackView"},
            {"com.exteragram.messenger.pillstack.ui.pills.weather.WeatherPreferencesActivity", "com.plusgram.app.pillstack.pills.weather.WeatherSettingsActivity"},
            {"com.exteragram.messenger.preferences.appearance.AppearancePreferencesActivity", "com.plusgram.app.settings.OpenExteraAppearanceActivity"},
            {"com.exteragram.messenger.nowplaying.ui.components.NowPlayingCard", "com.plusgram.app.components.ProfileMusicCard"},
            {"com.exteragram.messenger.nowplaying.NowPlayingController", "com.plusgram.app.nowplaying.NowPlayingController"},
            {"com.exteragram.messenger.proxy.ProxyController", "com.plusgram.app.proxy.ProxyController"},
            {"com.exteragram.messenger.utils.ui.UIUtil", "com.plusgram.app.utils.UIUtil"},
            {"com.exteragram.messenger.utils.ui.MainTabsUiHelper", "com.plusgram.app.appearance.MainTabsUiHelper"},
            {"com.google.android.exoplayer2.util.Consumer", "androidx.media3.common.util.Consumer"},
            {"com.google.android.exoplayer2.video.VideoSize", "androidx.media3.common.VideoSize"},
            {"com.google.android.exoplayer2.PlaybackException", "androidx.media3.common.PlaybackException"},
            {"com.google.android.exoplayer2.PlaybackParameters", "androidx.media3.common.PlaybackParameters"},
            {"com.google.android.exoplayer2.C", "androidx.media3.common.C"},
            {"org.telegram.ui.Gifts.ProfileGiftsContainer$SelectGiftsBottomSheet", "org.telegram.ui.Gifts.SelectGiftsBottomSheet"},
    };

    private static final String[][] PREFIXES = {
            {"com.exteragram.messenger.ai.data.", "com.plusgram.app.ai.data."},
            {"com.exteragram.messenger.ai.network.", "com.plusgram.app.ai.network."},
            {"com.exteragram.messenger.pillstack.core.", "com.plusgram.app.pillstack."},
            {"com.exteragram.messenger.pillstack.ui.pills.", "com.plusgram.app.pillstack.pills."},
            {"com.exteragram.messenger.pillstack.ui.", "com.plusgram.app.pillstack."},
            {"com.exteragram.messenger.preferences.", "com.plusgram.app.settings."},
            {"com.exteragram.messenger.plugins.", "com.plusgram.app.plugins."},
            {"com.exteragram.messenger.icons.", "com.plusgram.app.icons."},
            {"com.exteragram.messenger.camera.", "com.plusgram.app.camera."},
            {"com.exteragram.messenger.backup.", "com.plusgram.app.backup."},
            {"com.exteragram.messenger.feed.", "com.plusgram.app.feed."},
            {"com.exteragram.messenger.drawer.", "com.plusgram.app.drawer."},
            {"com.exteragram.messenger.components.", "com.plusgram.app.components."},
            {"com.exteragram.messenger.utils.", "com.plusgram.app.utils."},
    };

    private ClassAliases() {
    }

    public static String resolve(String name) {
        if (name == null) {
            return null;
        }
        String exact = lookup(name);
        if (exact != null) {
            return exact;
        }
        if (!underRoot(name)) {
            return name;
        }
        int dollar = name.indexOf('$');
        String outer = dollar < 0 ? name : name.substring(0, dollar);
        String nested = dollar < 0 ? "" : name.substring(dollar);
        exact = lookup(outer);
        if (exact != null) {
            return exact + nested;
        }
        for (String[] pair : PREFIXES) {
            if (outer.startsWith(pair[0])) {
                return pair[1] + outer.substring(pair[0].length()) + nested;
            }
        }
        return name;
    }

    private static boolean underRoot(String name) {
        for (String root : ROOTS) {
            if (name.startsWith(root + ".")) {
                return true;
            }
        }
        return false;
    }

    private static String lookup(String name) {
        for (String[] pair : EXACT) {
            if (pair[0].equals(name)) {
                return pair[1];
            }
        }
        return null;
    }
}
