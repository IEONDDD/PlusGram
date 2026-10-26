package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

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

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        PLUSGRAM("PlusgramIcon", R.drawable.plusgram_icon_background,
                R.drawable.plusgram_icon_foreground, R.string.AppIconExteraless),
        BLUEPRINT("BlueprintIcon", R.drawable.blueprint_icon_background,
                R.drawable.app_icon_empty_foreground, R.string.AppIconBlueprint),
        RED("RedIcon", R.drawable.red_icon_background,
                R.drawable.app_icon_empty_foreground, R.string.AppIconRed),
        NYA("NyaIcon", R.drawable.nya_icon_background,
                R.drawable.app_icon_empty_foreground, R.string.AppIconNya),
        AYU("AyuIcon", R.drawable.ayu_icon_background,
                R.drawable.app_icon_empty_foreground, R.string.AppIconAyu),
        QUACK("QuackIcon", R.drawable.quack_icon_background,
                R.drawable.app_icon_empty_foreground, R.string.AppIconQuack),
        GO("GoIcon", R.drawable.go_icon_background,
                R.drawable.app_icon_empty_foreground, R.string.AppIconGo),
        HAND("HandIcon", R.drawable.hand_icon_background,
                R.drawable.app_icon_empty_foreground, R.string.AppIconHand),
        MONO("MonoIcon", R.drawable.mono_icon_background,
                R.drawable.app_icon_empty_foreground, R.string.AppIconMono),
        NOTHING("NothingIcon", R.drawable.nothing_icon_background,
                R.drawable.app_icon_empty_foreground, R.string.AppIconNothing),
        PLUS("PlusIcon", R.drawable.plus_icon_background,
                R.drawable.app_icon_empty_foreground, R.string.AppIconPlus),
        TELEGRAM("TelegramIcon", R.drawable.original_icon_background,
                R.drawable.app_icon_empty_foreground, R.string.AppIconTelegramOriginal);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

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
