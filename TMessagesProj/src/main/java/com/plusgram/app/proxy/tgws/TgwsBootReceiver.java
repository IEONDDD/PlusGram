package com.plusgram.app.proxy.tgws;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.telegram.messenger.ApplicationLoader;

public final class TgwsBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (TgwsConfig.prefs().getBoolean("enabled", false) && TgwsConfig.prefs().getBoolean("auto_boot", false)) {
            ApplicationLoader.postInitApplication();
            TgwsService.setEnabled(true);
        }
    }
}
