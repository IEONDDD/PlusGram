package com.plusgram.app.proxy.tgws;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.chaquo.python.Python;
import com.plusgram.app.plugins.PythonPluginsEngine;
import com.plusgram.app.proxy.ProxyController;
import org.json.JSONObject;
import org.telegram.messenger.*;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.LaunchActivity;
import org.telegram.utils.proxy.ProxySettings;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.telegram.messenger.LocaleController.getString;

/** A single, serialized Python runtime owned by a visible Android foreground service. */
public final class TgwsService extends Service {
    private static final int NOTIFICATION = 14430;
    private static final String CHANNEL = "plusgram_tgws";
    private static final String STOP = "com.plusgram.app.TGWS_STOP";
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    public static volatile String state = "off";
    public static volatile String error = "";
    public static volatile long ping = -1;
    private static boolean appStarted;
    private static boolean live;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean destroyed, ready, applied, paused, busy, internalChange;
    private int generation;
    private String pausedRoute;
    private boolean pausedRouteEnabled;
    private final NotificationCenter.NotificationCenterDelegate observer = (id, account, args) -> {
        if (!internalChange && applied && TgwsConfig.prefs().getBoolean("enabled", false) && !ownsRoute()) setEnabled(false);
    };

    public static void setEnabled(boolean enabled) {
        TgwsConfig.prefs().edit().putBoolean("enabled", enabled).apply();
        Context context = ApplicationLoader.applicationContext;
        if (!enabled) {
            context.stopService(new Intent(context, TgwsService.class));
            restoreRoute();
            state = "off";
            return;
        }
        try {
            Intent intent = new Intent(context, TgwsService.class);
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
            else context.startService(intent);
        } catch (RuntimeException e) {
            state = "error";
            error = e.getClass().getSimpleName();
            TgwsConfig.prefs().edit().putBoolean("enabled", false).apply();
            restoreRoute();
        }
    }

    public static void onAppStart() {
        if (appStarted || live) return;
        appStarted = true;
        // A killed process must never leave a dead localhost route installed.
        restoreRoute();
        if (TgwsConfig.prefs().getBoolean("enabled", false)) setEnabled(true);
    }

    public static String statusText() {
        switch (state) {
            case "running": return LocaleController.getString(R.string.PlusgramTgwsRunning) + " 127.0.0.1:" + TgwsConfig.prefs().getInt("port", 1443);
            case "starting": return LocaleController.getString(R.string.PlusgramTgwsStarting);
            case "paused": return LocaleController.getString(R.string.PlusgramTgwsPaused);
            case "error": return LocaleController.getString(R.string.PlusgramTgwsError) + " (" + error + ")";
            default: return LocaleController.getString(R.string.PlusgramTgwsOff);
        }
    }

    private static boolean ownsRoute() {
        String owned = TgwsConfig.prefs().getString("owned_route", "");
        return !owned.isEmpty() && MessagesController.getGlobalMainSettings().getBoolean("proxy_enabled", false)
                && owned.equals(ProxySettings.fromSharedPreferences(MessagesController.getGlobalMainSettings()).getLink());
    }

    private void applyRoute() {
        android.content.SharedPreferences main = MessagesController.getGlobalMainSettings();
        if (!TgwsConfig.prefs().contains("owned_route")) {
            TgwsConfig.prefs().edit()
                    .putString("previous_route", ProxySettings.fromSharedPreferences(main).getLink())
                    .putBoolean("previous_enabled", main.getBoolean("proxy_enabled", false))
                    .putString("owned_route", TgwsConfig.proxy().getLink()).commit();
        }
        internalChange = true;
        try {
            useRoute(TgwsConfig.proxy(), true);
            ProxyController.getInstance().setName(SharedConfig.currentProxy, "Plusgram Bypass");
            applied = true;
        }
        finally { internalChange = false; }
    }

    public static void restoreRoute() {
        android.content.SharedPreferences p = TgwsConfig.prefs();
        if (!p.contains("owned_route")) return;
        // Compare address, type and secret even when another screen just disabled the route.
        boolean stillSelected = p.getString("owned_route", "").equals(
                ProxySettings.fromSharedPreferences(MessagesController.getGlobalMainSettings()).getLink());
        String owned = p.getString("owned_route", "");
        if (stillSelected) {
            ProxySettings previous = ProxySettings.fromUri(Uri.parse(p.getString("previous_route", "")));
            if (previous == null) previous = ProxySettings.builder().setAddress("").setPort(1080).build();
            useRoute(previous, p.getBoolean("previous_enabled", false) && previous.isValid());
        }
        p.edit().remove("owned_route").remove("previous_route").remove("previous_enabled").commit();
        for (SharedConfig.ProxyInfo info : new java.util.ArrayList<>(SharedConfig.proxyList)) {
            if (owned.equals(info.settings.getLink()) && info != SharedConfig.currentProxy) SharedConfig.deleteProxy(info);
        }
    }

    private static void useRoute(ProxySettings settings, boolean enabled) {
        android.content.SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        settings.toSharedPreferences(editor);
        editor.putBoolean("proxy_enabled", enabled).commit();
        SharedConfig.currentProxy = settings.isValid() ? SharedConfig.addProxy(new SharedConfig.ProxyInfo(settings)) : null;
        ConnectionsManager.setProxySettings(enabled, settings);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
    }

    public static void checkPing() {
        if (!"running".equals(state)) return;
        ping = -2;
        final String route = TgwsConfig.proxy().getLink();
        ConnectionsManager.getInstance(UserConfig.selectedAccount).checkProxy(TgwsConfig.proxy(), time ->
                AndroidUtilities.runOnUIThread(() -> {
                    if ("running".equals(state) && route.equals(TgwsConfig.proxy().getLink())) ping = time;
                }));
    }

    @Override public void onCreate() {
        super.onCreate();
        live = true;
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager.class).createNotificationChannel(
                    new NotificationChannel(CHANNEL, "Plusgram TGWS Proxy", NotificationManager.IMPORTANCE_LOW));
        }
        state = "starting";
        startForeground(NOTIFICATION, notification());
        NotificationCenter.getGlobalInstance().addObserver(observer, NotificationCenter.proxySettingsChanged);
        PythonPluginsEngine.getInstance().ensureStarted(this, ok -> handler.post(() -> {
            if (destroyed) return;
            if (!ok) { fail("PythonStartup"); return; }
            ready = true;
            handler.post(tick);
        }));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) {
            setEnabled(false);
            return START_NOT_STICKY;
        }
        if (!TgwsConfig.prefs().getBoolean("enabled", false)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ready) restart();
        return START_STICKY;
    }

    private boolean vpnActive() {
        if (!TgwsConfig.prefs().getBoolean("pause_vpn", true)) return false;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        for (Network network : cm.getAllNetworks()) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true;
        }
        return false;
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (destroyed || !ready) return;
            boolean vpn = vpnActive();
            if (paused && pausedRoute != null &&
                    (!pausedRoute.equals(ProxySettings.fromSharedPreferences(MessagesController.getGlobalMainSettings()).getLink())
                    || pausedRouteEnabled != MessagesController.getGlobalMainSettings().getBoolean("proxy_enabled", false))) {
                setEnabled(false);
                return;
            }
            if (!busy && (vpn != paused || "starting".equals(state) && !applied)) {
                if (vpn && !paused) { paused = true; stopRuntime(true); }
                else if (!vpn && (paused || !applied)) { paused = false; startRuntime(); }
            }
            if (applied && !ownsRoute()) { setEnabled(false); return; }
            if (applied && !busy) {
                int token = generation;
                WORKER.execute(() -> {
                    try {
                        JSONObject status = new JSONObject(Python.getInstance().getModule("plusgram_tgws.runtime").callAttr("status").toString());
                        if (!"running".equals(status.optString("state"))) handler.post(() -> {
                            if (!destroyed && token == generation) fail(status.optString("error", "RuntimeStopped"));
                        });
                    } catch (Exception e) { handler.post(() -> { if (!destroyed && token == generation) fail("RuntimeStopped"); }); }
                });
            }
            handler.postDelayed(this, 2000);
        }
    };

    private void restart() {
        paused = vpnActive();
        stopRuntime(paused);
    }

    private void stopRuntime(boolean forVpn) {
        int token = ++generation;
        busy = true;
        applied = false;
        internalChange = true;
        try { restoreRoute(); } finally { internalChange = false; }
        pausedRoute = ProxySettings.fromSharedPreferences(MessagesController.getGlobalMainSettings()).getLink();
        pausedRouteEnabled = MessagesController.getGlobalMainSettings().getBoolean("proxy_enabled", false);
        state = forVpn ? "paused" : "starting";
        ping = -1;
        WORKER.execute(() -> {
            try { Python.getInstance().getModule("plusgram_tgws.runtime").callAttr("stop"); }
            catch (Exception e) { handler.post(() -> { if (!destroyed && token == generation) fail("Shutdown"); }); return; }
            handler.post(() -> {
                if (destroyed || token != generation) return;
                busy = false;
                updateNotification();
                if (!forVpn) startRuntime();
            });
        });
    }

    private void startRuntime() {
        int token = ++generation;
        busy = true;
        state = "starting";
        String config = TgwsConfig.json().toString();
        String routeBeforeStart = ProxySettings.fromSharedPreferences(MessagesController.getGlobalMainSettings()).getLink();
        boolean routeEnabledBeforeStart = MessagesController.getGlobalMainSettings().getBoolean("proxy_enabled", false);
        WORKER.execute(() -> {
            try {
                com.chaquo.python.PyObject runtime = Python.getInstance().getModule("plusgram_tgws.runtime");
                runtime.callAttr("stop");
                runtime.callAttr("start", config);
                JSONObject result = null;
                for (int i = 0; i < 100; i++) {
                    result = new JSONObject(runtime.callAttr("status").toString());
                    if (!"starting".equals(result.optString("state"))) break;
                    Thread.sleep(100);
                }
                if (result == null || !"running".equals(result.optString("state")))
                    throw new IllegalStateException(result == null ? "StartupTimeout" : result.optString("error", "StartupTimeout"));
                handler.post(() -> {
                    if (destroyed || token != generation) return;
                    busy = false;
                    if (!routeBeforeStart.equals(ProxySettings.fromSharedPreferences(MessagesController.getGlobalMainSettings()).getLink())
                            || routeEnabledBeforeStart != MessagesController.getGlobalMainSettings().getBoolean("proxy_enabled", false)) {
                        setEnabled(false);
                        return;
                    }
                    state = "running";
                    applyRoute();
                    updateNotification();
                    checkPing();
                });
            } catch (Exception e) {
                String reason = e instanceof IllegalStateException ? e.getMessage() : e.getClass().getSimpleName();
                handler.post(() -> { if (!destroyed && token == generation) fail(reason); });
            }
        });
    }

    private void fail(String reason) {
        error = reason;
        state = "error";
        TgwsConfig.prefs().edit().putBoolean("enabled", false).apply();
        stopSelf();
    }

    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, LaunchActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, TgwsService.class).setAction(STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.plusgram_notification)
                .setContentTitle("Plusgram TGWS Proxy").setContentText(statusText()).setContentIntent(open)
                .setOngoing(true).setSilent(true).addAction(0, LocaleController.getString(R.string.Stop), stop).build();
    }

    private void updateNotification() {
        getSystemService(NotificationManager.class).notify(NOTIFICATION, notification());
    }

    @Override public void onDestroy() {
        live = false;
        destroyed = true;
        ++generation;
        handler.removeCallbacksAndMessages(null);
        NotificationCenter.getGlobalInstance().removeObserver(observer, NotificationCenter.proxySettingsChanged);
        restoreRoute();
        WORKER.execute(() -> {
            if (Python.isStarted()) {
                try { Python.getInstance().getModule("plusgram_tgws.runtime").callAttr("stop"); }
                catch (Exception ignored) {}
            }
        });
        if (!"error".equals(state)) state = "off";
        ping = -1;
        stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
