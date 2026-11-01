package com.plusgram.app.proxy.tgws;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import com.plusgram.app.proxy.bypass.ProxyAutoDetector;
import com.plusgram.app.proxy.bypass.TgWsProxyService;

/** Compatibility entry point for existing Plusgram screens; no Python networking. */
public final class TgwsService {
    private static boolean initialized;
    private TgwsService() {}
    public static synchronized void onAppStart() {
        if (initialized) return;
        initialized = true;
        Context context = ApplicationLoader.applicationContext;
        SharedPreferences prefs = context.getSharedPreferences("tgwsproxy", 0);
        if (!prefs.getBoolean("plusgram_migrated", false)) {
            SharedPreferences old = context.getSharedPreferences("plusgram_tgws", 0);
            // Adopt the working upstream defaults. Do not migrate the old disabled
            // Cloudflare setting, which forced blocked direct Telegram routes.
            prefs.edit().putBoolean("plusgram_migrated", true)
                    .putBoolean("enabled", old.getBoolean("enabled", false))
                    .putBoolean("cf_enabled", true).putBoolean("auto_start", true).apply();
        }
        ProxyAutoDetector.init(context);
        if (prefs.getBoolean("enabled", false) && !ProxyAutoDetector.isVpnActive(context)) {
            new Thread(() -> ProxyAutoDetector.startProxyService(context, prefs), "bypass-restore").start();
        }
    }
    public static String statusText() {
        if (ProxyAutoDetector.isVpnActive(ApplicationLoader.applicationContext))
            return LocaleController.getString(R.string.PlusgramTgwsPaused);
        if (TgWsProxyService.isRunning) return LocaleController.getString(R.string.PlusgramTgwsRunning) + " 127.0.0.1:" + TgWsProxyService.currentPort;
        boolean requested = ApplicationLoader.applicationContext.getSharedPreferences("tgwsproxy", 0).getBoolean("enabled", false);
        return LocaleController.getString(requested ? R.string.PlusgramTgwsStarting : R.string.PlusgramTgwsOff);
    }
}
