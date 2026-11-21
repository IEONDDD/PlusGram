package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

import java.util.ArrayList;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.PLUSGRAM);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        // Пока пользователь ничего не выбирал, включённой считается наша иконка:
        // именно она стоит у <application> в манифесте, и переключатель должен
        // показывать выбранным то, что человек видит на рабочем столе.
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.PLUSGRAM;
    }

    public static synchronized void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ArrayList<PackageManager.ComponentEnabledSetting> settings = new ArrayList<>();
            for (LauncherIcon candidate : LauncherIcon.values()) {
                settings.add(new PackageManager.ComponentEnabledSetting(candidate.getComponentName(ctx),
                        candidate == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP));
            }
            pm.setComponentEnabledSettings(settings);
            return;
        }

        pm.setComponentEnabledSetting(icon.getComponentName(ctx), PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        for (LauncherIcon candidate : LauncherIcon.values()) {
            if (candidate != icon && isEnabled(candidate)) {
                pm.setComponentEnabledSetting(candidate.getComponentName(ctx), PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }
    }

    public enum LauncherIcon {
        PLUSGRAM("PlusgramIcon", R.drawable.plusgram_icon_background,
                R.drawable.plusgram_icon_foreground, R.string.PlusgramIconClassic),
        PLUSGRAM_LIGHT("PlusgramLightIcon", R.drawable.plusgram_icon_light_background,
                R.drawable.plusgram_icon_light_foreground, R.string.PlusgramIconLight),
        PLUSGRAM_BURGUNDY("PlusgramBurgundyIcon", R.drawable.plusgram_icon_burgundy_background,
                R.drawable.plusgram_icon_burgundy_foreground, R.string.PlusgramIconBurgundy),
        PLUSGRAM_POMEGRANATE("PlusgramPomegranateIcon", R.drawable.plusgram_icon_pomegranate_background,
                R.drawable.plusgram_icon_pomegranate_foreground, R.string.PlusgramIconPomegranate),
        TELEGRAM("TelegramIcon", R.drawable.icon_background_sa,
                R.mipmap.icon_foreground_sa, R.string.AppIconTelegramOriginal),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa,
                R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa,
                R.mipmap.icon_foreground_sa, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa,
                R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa,
                R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa,
                R.mipmap.icon_foreground_sa, R.string.AppIconNox);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        public boolean isPlusgram() {
            switch (this) {
                case PLUSGRAM:
                case PLUSGRAM_LIGHT:
                case PLUSGRAM_BURGUNDY:
                case PLUSGRAM_POMEGRANATE:
                    return true;
                default:
                    return false;
            }
        }

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }

    }
}
