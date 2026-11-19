package com.plusgram.app.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.ColorUtils;

import com.plusgram.app.appearance.MaterialSettingsFragment;
import com.plusgram.app.proxy.tgws.TgwsService;
import com.plusgram.app.proxy.tgws.TgwsSettingsActivity;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.PushListenerController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.ProxyListActivity;

import xyz.nextalone.nagram.NaConfig;

public final class ConnectionDiagnosticsActivity extends MaterialSettingsFragment {
    private TextView connection, proxy, bypass, provider, registration, permission, battery;

    private final Runnable update = new Runnable() {
        @Override public void run() {
            refreshStatus();
            AndroidUtilities.runOnUIThread(this, 2000);
        }
    };

    @Override protected int titleRes() {
        return R.string.PlusgramDiagnosticsTitle;
    }

    @Override protected void buildContent() {
        LinearLayout overview = card(0);
        int surface = Theme.getColor(Theme.key_windowBackgroundWhite);
        int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        boolean dark = Theme.isCurrentThemeDark();
        overview.setBackground(Theme.createRoundRectDrawable(dp(32),
                ColorUtils.blendARGB(surface, accent, dark ? 0.23f : 0.15f)));
        overview.setPadding(dp(20), dp(22), dp(20), dp(22));
        LinearLayout summary = new LinearLayout(context);
        summary.setGravity(Gravity.CENTER_VERTICAL);
        overview.addView(summary);
        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.baseline_wifi_24);
        icon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        icon.setScaleType(ImageView.ScaleType.CENTER);
        icon.setBackground(Theme.createRoundRectDrawable(dp(18),
                ColorUtils.blendARGB(surface, accent, dark ? 0.38f : 0.27f)));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        summary.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout heading = new LinearLayout(context);
        heading.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(0, -2, 1);
        headingParams.setMarginStart(dp(16));
        summary.addView(heading, headingParams);
        TextView label = text(getString(R.string.PlusgramDiagnosticsConnection), 14,
                Theme.key_windowBackgroundWhiteBlackText);
        heading.addView(label);
        connection = text("", 28, Theme.key_windowBackgroundWhiteBlackText);
        connection.setTypeface(null, Typeface.BOLD);
        connection.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        heading.addView(connection);

        LinearLayout notifications = card(R.string.PlusgramDiagnosticsDelivery);
        boolean signedIn = getUserConfig().isClientActivated();
        provider = row(notifications, R.string.PushServiceType, "", signedIn
                ? () -> presentFragment(new OpenExteraGeneralActivity()) : null);
        registration = row(notifications, R.string.PlusgramDiagnosticsRegistration, "", null);

        LinearLayout android = card(R.string.PlusgramDiagnosticsAndroid);
        permission = row(android, R.string.PlusgramDiagnosticsPermission, "",
                () -> openSystemSettings(Build.VERSION.SDK_INT >= 26
                        ? new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                        : appDetails()));
        battery = row(android, R.string.PlusgramDiagnosticsBackground, "", () -> {
            if (backgroundRestricted()) {
                openAppDetails();
            } else {
                openSystemSettings(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        });

        LinearLayout routing = card(R.string.PlusgramDiagnosticsRouting);
        proxy = row(routing, R.string.ProxySettings, "", () -> presentFragment(new ProxyListActivity()));
        bypass = row(routing, R.string.PlusgramTgwsTitle, "", () -> presentFragment(new TgwsSettingsActivity()));
        refreshStatus();
    }

    private void refreshStatus() {
        if (connection == null) return;
        set(connection, switch (getConnectionsManager().getConnectionState()) {
            case ConnectionsManager.ConnectionStateConnected -> R.string.PlusgramDiagnosticsConnected;
            case ConnectionsManager.ConnectionStateWaitingForNetwork -> R.string.WaitingForNetwork;
            case ConnectionsManager.ConnectionStateConnectingToProxy -> R.string.ConnectingToProxy;
            case ConnectionsManager.ConnectionStateUpdating -> R.string.Updating;
            case ConnectionsManager.ConnectionStateConnecting -> R.string.Connecting;
            default -> R.string.PlusgramDiagnosticsUnknown;
        });

        int type = NaConfig.INSTANCE.getPushServiceType().Int();
        set(provider, switch (type) {
            case 0 -> R.string.PushServiceTypeInApp;
            case 1 -> R.string.PushServiceTypeFCM;
            case 2 -> R.string.PushServiceTypeUnified;
            case 3 -> R.string.PushServiceTypeMicroG;
            default -> R.string.PlusgramDiagnosticsUnknown;
        });
        set(registration, registrationStatus(type));
        set(permission, NotificationManagerCompat.from(context).areNotificationsEnabled()
                ? R.string.PlusgramDiagnosticsAllowed : R.string.PlusgramDiagnosticsBlocked);
        PowerManager power = context.getSystemService(PowerManager.class);
        set(battery, backgroundRestricted() ? R.string.PlusgramDiagnosticsRestricted
                : power == null ? R.string.PlusgramDiagnosticsUnknown
                : power.isIgnoringBatteryOptimizations(context.getPackageName())
                        ? R.string.PlusgramDiagnosticsExempt : R.string.PlusgramDiagnosticsOptimized);
        set(proxy, MessagesController.getGlobalMainSettings().getBoolean("proxy_enabled", false)
                ? R.string.PlusgramDiagnosticsOn : R.string.PlusgramDiagnosticsOff);
        set(bypass, switch (TgwsService.state) {
            case "running" -> R.string.PlusgramTgwsRunning;
            case "starting" -> R.string.PlusgramTgwsStarting;
            case "paused" -> R.string.PlusgramTgwsPaused;
            case "error" -> R.string.PlusgramTgwsError;
            default -> R.string.PlusgramTgwsOff;
        });
    }

    private boolean backgroundRestricted() {
        ActivityManager activity = context.getSystemService(ActivityManager.class);
        return Build.VERSION.SDK_INT >= 28 && activity != null && activity.isBackgroundRestricted();
    }

    private int registrationStatus(int type) {
        if (!getUserConfig().isClientActivated()) return R.string.PlusgramDiagnosticsSignIn;
        if (type == 0) {
            return getConnectionsManager().isPushConnectionEnabled()
                    ? R.string.PlusgramDiagnosticsInAppOn : R.string.PlusgramDiagnosticsInAppOff;
        }
        if (type < 1 || type > 3) return R.string.PlusgramDiagnosticsUnknown;
        if (type != 2 && !BuildConfig.FIREBASE_ENABLED) return R.string.PlusgramDiagnosticsNoFirebase;
        int expected = type == 2 ? PushListenerController.PUSH_TYPE_WEB : PushListenerController.PUSH_TYPE_FIREBASE;
        PushListenerController.IPushListenerServiceProvider activeProvider = ApplicationLoader.getPushProvider();
        if (activeProvider.getPushType() != expected || !activeProvider.hasServices()) {
            return R.string.PlusgramDiagnosticsProviderUnavailable;
        }
        if (SharedConfig.pushType == expected && !TextUtils.isEmpty(SharedConfig.pushString)) {
            if (getUserConfig().registeredForPush) return R.string.PlusgramDiagnosticsRegistered;
            return getMessagesController().registeringForPush ? R.string.PlusgramDiagnosticsRegistering
                    : R.string.PlusgramDiagnosticsNotRegistered;
        }
        String status = SharedConfig.pushStringStatus;
        if ("__FIREBASE_FAILED__".equals(status) || "__UNIFIEDPUSH_FAILED__".equals(status)) {
            return R.string.PlusgramDiagnosticsRegistrationFailed;
        }
        return R.string.PlusgramDiagnosticsWaitingForPush;
    }

    private void set(TextView view, int resource) {
        set(view, getString(resource));
    }

    private void set(TextView view, String value) {
        if (!TextUtils.equals(view.getText(), value)) view.setText(value);
    }

    private Intent appDetails() {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.getPackageName(), null));
    }

    private void openAppDetails() {
        openSystemSettings(appDetails());
    }

    private void openSystemSettings(Intent intent) {
        if (getParentActivity() == null) return;
        try {
            getParentActivity().startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            if (!Settings.ACTION_APPLICATION_DETAILS_SETTINGS.equals(intent.getAction())) {
                openAppDetails();
            } else {
                BulletinFactory.of(this).createErrorBulletin(getString(R.string.PlusgramDiagnosticsSettingsUnavailable)).show();
            }
        }
    }

    @Override public void onResume() {
        super.onResume();
        AndroidUtilities.cancelRunOnUIThread(update);
        update.run();
    }

    @Override public void onPause() {
        AndroidUtilities.cancelRunOnUIThread(update);
        super.onPause();
    }

    @Override public void onFragmentDestroy() {
        AndroidUtilities.cancelRunOnUIThread(update);
        super.onFragmentDestroy();
    }
}
