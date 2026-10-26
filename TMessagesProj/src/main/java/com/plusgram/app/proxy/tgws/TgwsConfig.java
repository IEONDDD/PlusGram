package com.plusgram.app.proxy.tgws;

import android.content.SharedPreferences;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.utils.proxy.ProxySettings;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public final class TgwsConfig {
    private TgwsConfig() {}
    public static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("plusgram_tgws", 0);
    }
    public static String secret() {
        String value = prefs().getString("secret", "");
        if (value.isEmpty()) {
            byte[] bytes = new byte[16];
            new SecureRandom().nextBytes(bytes);
            value = hex(bytes);
            prefs().edit().putString("secret", value).commit();
        }
        return value;
    }
    public static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder();
        for (byte b : bytes) value.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return value.toString();
    }
    public static JSONObject json() {
        SharedPreferences p = prefs();
        JSONObject out = new JSONObject();
        try {
            out.put("port", p.getInt("port", 1443));
            out.put("pool", p.getInt("pool", 4));
            out.put("buffer", p.getInt("buffer", 256));
            out.put("secret", secret());
            out.put("cloudflare", p.getBoolean("cloudflare", false));
            out.put("domain", p.getString("domain", ""));
            out.put("fake_tls", p.getBoolean("fake_tls", false));
            out.put("sni", p.getString("sni", "google.com"));
            out.put("dc", p.getString("dc", ""));
        } catch (org.json.JSONException e) { throw new IllegalStateException(e); }
        return out;
    }
    public static ProxySettings proxy() {
        String key = secret();
        key = prefs().getBoolean("fake_tls", false)
                ? "ee" + key + hex(prefs().getString("sni", "google.com").getBytes(StandardCharsets.US_ASCII))
                : "dd" + key;
        return ProxySettings.builder().setType(ProxySettings.Type.MTPROTO)
                .setAddress("127.0.0.1").setPort(prefs().getInt("port", 1443)).setSecret(key).build();
    }
}
