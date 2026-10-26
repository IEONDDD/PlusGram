package com.plusgram.app.proxy.tgws;

import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.telegram.messenger.*;
import com.plusgram.app.appearance.MaterialSettingsFragment;
import static org.telegram.messenger.LocaleController.getString;

public final class TgwsSettingsActivity extends MaterialSettingsFragment {
    private TextView status, ping;
    private boolean visible;
    private boolean displayedEnabled;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (!visible) return;
            if (displayedEnabled != TgwsConfig.prefs().getBoolean("enabled", false)) rebuild();
            if (status != null) status.setText(TgwsService.statusText());
            if (ping != null) ping.setText(TgwsService.ping == -2 ? getString(R.string.PlusgramTgwsChecking)
                    : TgwsService.ping < 0 ? getString(R.string.PlusgramTgwsPingHint)
                    : TgwsService.ping + " ms");
            AndroidUtilities.runOnUIThread(this, 1000);
        }
    };
    @Override protected int titleRes() { return R.string.PlusgramTgwsTitle; }
    @Override protected void buildContent() {
        displayedEnabled = TgwsConfig.prefs().getBoolean("enabled", false);
        LinearLayout main = card(0);
        android.widget.ImageView icon = new android.widget.ImageView(context);
        icon.setImageResource(R.drawable.plusgram_icon);
        LinearLayout.LayoutParams logo = new LinearLayout.LayoutParams(dp(80), dp(80));
        logo.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        logo.topMargin = dp(24);
        main.addView(icon, logo);
        row(main, R.string.PlusgramTgwsTitle, getString(R.string.PlusgramTgwsSubtitle), null);
        status = row(main, R.string.PlusgramTgwsStatus, TgwsService.statusText(), null);
        toggle(main, R.string.PlusgramTgwsEnable, TgwsConfig.prefs().getBoolean("enabled", false), value -> {
            TgwsService.setEnabled(value);
            AndroidUtilities.runOnUIThread(this::rebuild, 300);
        });
        ping = row(main, R.string.PlusgramTgwsPing, getString(R.string.PlusgramTgwsPingHint), TgwsService::checkPing);
        toggle(main, R.string.PlusgramTgwsAutoBoot, TgwsConfig.prefs().getBoolean("auto_boot", false),
                value -> TgwsConfig.prefs().edit().putBoolean("auto_boot", value).apply());
        toggle(main, R.string.PlusgramTgwsPauseVpn, TgwsConfig.prefs().getBoolean("pause_vpn", true),
                value -> TgwsConfig.prefs().edit().putBoolean("pause_vpn", value).apply());
        LinearLayout connection = card(R.string.PlusgramTgwsConnection);
        number(connection, R.string.PlusgramTgwsPort, "port", 1443, 1024, 65535);
        number(connection, R.string.PlusgramTgwsPool, "pool", 4, 1, 16);
        number(connection, R.string.PlusgramTgwsBuffer, "buffer", 256, 64, 1024);
        LinearLayout security = card(R.string.PlusgramTgwsSecurity);
        String secret = TgwsConfig.secret();
        row(security, R.string.PlusgramTgwsSecret, "••••" + secret.substring(secret.length() - 6), () ->
                edit(R.string.PlusgramTgwsSecret, secret, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, value -> {
                    if (!value.matches("[0-9a-fA-F]{32}")) return false;
                    TgwsConfig.prefs().edit().putString("secret", value.toLowerCase(java.util.Locale.ROOT)).apply();
                    changed(); return true;
                }));
        LinearLayout cf = card(R.string.PlusgramTgwsCloudflare);
        toggle(cf, R.string.PlusgramTgwsCloudflareEnable, TgwsConfig.prefs().getBoolean("cloudflare", false),
                value -> { TgwsConfig.prefs().edit().putBoolean("cloudflare", value).apply(); changed(); });
        domain(cf, R.string.PlusgramTgwsDomain, "domain", "", true);
        LinearLayout tls = card(R.string.PlusgramTgwsFakeTls);
        toggle(tls, R.string.PlusgramTgwsFakeTlsEnable, TgwsConfig.prefs().getBoolean("fake_tls", false),
                value -> { TgwsConfig.prefs().edit().putBoolean("fake_tls", value).apply(); changed(); });
        domain(tls, R.string.PlusgramTgwsSni, "sni", "google.com", false);
        LinearLayout advanced = card(R.string.PlusgramTgwsAdvanced);
        String dc = TgwsConfig.prefs().getString("dc", "");
        row(advanced, R.string.PlusgramTgwsDc, dc.isEmpty() ? getString(R.string.PlusgramTgwsDefaultDc) : dc, () ->
                edit(R.string.PlusgramTgwsDc, dc, InputType.TYPE_CLASS_TEXT, value -> {
                    if (!value.isEmpty()) {
                        for (String entry : value.split("[,;\\s]+")) {
                            String[] pair = entry.split(":");
                            if (pair.length != 2 || !pair[0].matches("[1-5]|203")) return false;
                            String[] ip = pair[1].split("\\.", -1);
                            if (ip.length != 4) return false;
                            for (String part : ip) {
                                if (!part.matches("[0-9]{1,3}") || Integer.parseInt(part) > 255) return false;
                            }
                        }
                    }
                    TgwsConfig.prefs().edit().putString("dc", value).apply(); changed(); return true;
                }));
        footer(R.string.PlusgramTgwsInfo);
    }
    private void changed() {
        if (TgwsConfig.prefs().getBoolean("enabled", false)) TgwsService.setEnabled(true);
    }
    private void number(LinearLayout card, int title, String key, int fallback, int low, int high) {
        int current = TgwsConfig.prefs().getInt(key, fallback);
        row(card, title, Integer.toString(current), () -> edit(title, Integer.toString(current), InputType.TYPE_CLASS_NUMBER, value -> {
            try {
                int number = Integer.parseInt(value);
                if (number < low || number > high) return false;
                TgwsConfig.prefs().edit().putInt(key, number).apply(); changed(); return true;
            } catch (NumberFormatException e) { return false; }
        }));
    }
    private void domain(LinearLayout card, int title, String key, String fallback, boolean allowEmpty) {
        String current = TgwsConfig.prefs().getString(key, fallback);
        row(card, title, current.isEmpty() ? getString(R.string.PlusgramTgwsAuto) : current,
                () -> edit(title, current, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, value -> {
                    String domain = value.toLowerCase(java.util.Locale.ROOT);
                    if (!(allowEmpty && domain.isEmpty())) {
                        if (domain.length() > 253 || !domain.contains(".")) return false;
                        for (String label : domain.split("\\.", -1)) {
                            if (!label.matches("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")) return false;
                        }
                    }
                    TgwsConfig.prefs().edit().putString(key, domain).apply(); changed(); return true;
                }));
    }
    @Override public void onResume() {
        super.onResume(); visible = true;
        AndroidUtilities.cancelRunOnUIThread(refresh);
        AndroidUtilities.runOnUIThread(refresh);
    }
    @Override public void onPause() {
        visible = false; AndroidUtilities.cancelRunOnUIThread(refresh); super.onPause();
    }
    @Override public void onFragmentDestroy() {
        visible = false; AndroidUtilities.cancelRunOnUIThread(refresh); super.onFragmentDestroy();
    }
}
