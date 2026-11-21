package com.plusgram.app.updater;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import tw.nekomimi.nekogram.helpers.remote.UpdateHelper;
import xyz.nextalone.nagram.NaConfig;

public final class GitHubUpdater {

    private static final String REPO = "IEONDDD/PlusGram";
    public static final String RELEASES_URL = "https://github.com/" + REPO + "/releases";
    private static final String API = "https://api.github.com/repos/" + REPO;
    private static final long AUTO_INTERVAL = TimeUnit.HOURS.toMillis(6);
    private static final String PREFS = "exteraless_updater";
    private static final String KEY_LAST_CHECK = "last_check";
    private static final String KEY_SKIPPED = "skipped_tag";
    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private static volatile OkHttpClient client;
    private static volatile boolean checking;

    private GitHubUpdater() {
    }

    public static final class Release {
        public String tag;
        public String name;
        public String body;
        public String apkName;
        public long apkSize;
        public long published;

        public String displayVersion() {
            String value = TextUtils.isEmpty(tag) ? name : tag;
            return value != null && value.startsWith("v") ? value.substring(1) : value;
        }
    }

    public interface CheckCallback {
        void onChecked(Release release, Boolean newer, boolean failed);
    }

    private static OkHttpClient client() {
        if (client == null) {
            synchronized (GitHubUpdater.class) {
                if (client == null) {
                    client = new OkHttpClient.Builder()
                            .connectTimeout(20, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return client;
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void check(boolean force) {
        int channel = NaConfig.INSTANCE.getAutoUpdateChannel().Int();
        if (!force) {
            if (channel == UpdateHelper.UPDATE_OFF) {
                return;
            }
            if (Math.abs(System.currentTimeMillis() - lastCheck()) < AUTO_INTERVAL) {
                return;
            }
        }
        if (checking) {
            return;
        }
        checking = true;
        fetch((found, isNewer, failed) -> {
            checking = false;
            boolean offer = found != null && (Boolean.TRUE.equals(isNewer) || force && isNewer == null);
            if (offer && (force || !TextUtils.equals(found.tag, prefs().getString(KEY_SKIPPED, null)))) {
                show(found);
            } else if (force) {
                bulletin(failed
                        ? LocaleController.getString(R.string.OEUpdateCheckFailed)
                        : LocaleController.getString(R.string.YourVersionIsLatestNax), failed);
            }
        });
    }

    public static void fetch(CheckCallback callback) {
        final boolean prerelease = NaConfig.INSTANCE.getAutoUpdateChannel().Int() == UpdateHelper.UPDATE_CHANNEL_BETA;
        new Thread(() -> {
            Release release = null;
            Boolean newer = null;
            boolean failed = false;
            try {
                release = latest(prerelease);
                if (release != null) {
                    newer = isNewer(release);
                }
                prefs().edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply();
            } catch (Exception e) {
                failed = true;
                FileLog.e("GitHubUpdater: check failed", e);
            }
            final Release found = release;
            final Boolean isNewer = newer;
            final boolean error = failed;
            AndroidUtilities.runOnUIThread(() -> callback.onChecked(found, isNewer, error));
        }, "gh-updater").start();
    }

    public static long lastCheck() {
        return prefs().getLong(KEY_LAST_CHECK, 0);
    }

    private static void bulletin(String text, boolean error) {
        BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (fragment == null) {
            return;
        }
        if (error) {
            BulletinFactory.of(fragment).createErrorBulletin(text).show();
        } else {
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.done, text).show();
        }
    }

    private static JSONObject getJson(String url) throws Exception {
        Request request = new Request.Builder().url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Plusgram")
                .build();
        try (Response response = client().newCall(request).execute()) {
            ResponseBody body = response.body();
            if (response.code() == 404) {
                return null;
            }
            if (!response.isSuccessful() || body == null) {
                throw new IllegalStateException("HTTP " + response.code());
            }
            String text = body.string();
            return text.trim().startsWith("[") ? new JSONObject().put("items", new JSONArray(text)) : new JSONObject(text);
        }
    }

    private static Release latest(boolean prerelease) throws Exception {
        JSONObject wrapper = getJson(API + "/releases?per_page=10");
        JSONArray releases = wrapper == null ? null : wrapper.optJSONArray("items");
        if (releases == null) {
            return null;
        }
        for (int i = 0; i < releases.length(); i++) {
            JSONObject item = releases.optJSONObject(i);
            if (item == null || item.optBoolean("draft") || item.optBoolean("prerelease") && !prerelease) {
                continue;
            }
            JSONObject asset = pickAsset(item.optJSONArray("assets"));
            if (asset == null) {
                continue;
            }
            Release release = new Release();
            release.tag = item.optString("tag_name");
            release.name = item.optString("name", release.tag);
            release.body = item.optString("body", "");
            release.apkName = asset.optString("name");
            release.apkSize = asset.optLong("size");
            release.published = parseDate(item.optString("published_at"));
            return release;
        }
        return null;
    }

    private static JSONObject pickAsset(JSONArray assets) {
        if (assets == null) {
            return null;
        }
        JSONObject universal = null;
        JSONObject any = null;
        String abi = Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "";
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            String name = asset == null ? "" : asset.optString("name").toLowerCase(Locale.ROOT);
            if (!name.endsWith(".apk")) {
                continue;
            }
            if (!abi.isEmpty() && name.contains(abi.toLowerCase(Locale.ROOT))) {
                return asset;
            }
            if (name.contains("universal") && universal == null) {
                universal = asset;
            }
            if (any == null && !name.contains("armeabi") && !name.contains("x86")) {
                any = asset;
            }
        }
        return universal != null ? universal : any;
    }

    private static int[] version(String... sources) {
        for (String source : sources) {
            if (source == null) {
                continue;
            }
            Matcher matcher = VERSION.matcher(source);
            if (matcher.find()) {
                return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))};
            }
        }
        return null;
    }

    private static Integer compareVersion(Release release) {
        int[] remote = version(release.tag, release.name, release.apkName);
        int[] local = version(BuildVars.BUILD_VERSION_STRING);
        if (remote == null || local == null) {
            return null;
        }
        for (int i = 0; i < 3; i++) {
            if (remote[i] != local[i]) {
                return Integer.compare(remote[i], local[i]);
            }
        }
        return 0;
    }

    private static Boolean isNewer(Release release) throws Exception {
        Integer versionOrder = compareVersion(release);
        if (versionOrder != null && versionOrder != 0) {
            return versionOrder > 0;
        }
        String commit = BuildConfig.BUILD_COMMIT_ID;
        if (!TextUtils.isEmpty(commit)) {
            JSONObject compare = getJson(API + "/compare/" + commit + "..." + Uri.encode(release.tag));
            if (compare != null) {
                return compare.optInt("ahead_by", 0) > 0;
            }
        }
        if (BuildConfig.BUILD_TIMESTAMP > 0) {
            JSONObject item = getJson(API + "/releases/tags/" + Uri.encode(release.tag));
            long published = item == null ? 0 : parseDate(item.optString("published_at"));
            return published > BuildConfig.BUILD_TIMESTAMP * 1000L;
        }
        return versionOrder != null ? Boolean.FALSE : null;
    }

    private static long parseDate(String value) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(value).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    private static void show(Release release) {
        BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        Activity activity = fragment == null ? null : fragment.getParentActivity();
        if (activity == null) {
            return;
        }
        String size = release.apkSize > 0 ? AndroidUtilities.formatFileSize(release.apkSize) : "";
        int[] remote = version(release.tag, release.name, release.apkName);
        StringBuilder subtitle = new StringBuilder(TextUtils.isEmpty(release.name) ? release.tag : release.name);
        if (remote != null) {
            subtitle.append(" · ").append(remote[0]).append('.').append(remote[1]).append('.').append(remote[2]);
        }
        if (!size.isEmpty()) {
            subtitle.append(" · ").append(size);
        }
        String notes = TextUtils.isEmpty(release.body) ? release.tag : release.body;
        new UpdateSheet(activity, fragment.getResourceProvider(), LocaleController.getString(R.string.OEUpdateTitle),
                subtitle.toString(), notes, new UpdateSheet.Delegate() {
            @Override
            public void onUpdate() {
                openReleasePage(activity, release);
            }

            @Override
            public void onSkip() {
                prefs().edit().putString(KEY_SKIPPED, release.tag).apply();
            }
        }).show();
    }

    static void openReleasePage(Context context, Release release) {
        String url = TextUtils.isEmpty(release.tag)
                ? RELEASES_URL
                : RELEASES_URL + "/tag/" + Uri.encode(release.tag);
        Browser.openUrlInSystemBrowser(context, url);
    }
}
