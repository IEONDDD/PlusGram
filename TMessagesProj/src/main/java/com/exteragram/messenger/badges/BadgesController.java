package com.exteragram.messenger.badges;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.FrameLayout;

import com.exteragram.messenger.api.dto.BadgeDTO;
import com.exteragram.messenger.badges.source.ApiBadgeSource;
import com.plusgram.app.badges.PlusgramBadgeDrawable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.BulletinFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Контроллер бейджей (значков профиля) Plusgram.
 * Поддерживает:
 * - Загрузку и кэширование списка спонсоров из GitHub (badges.json)
 * - Адаптацию цвета значка под тему оформления (Material You)
 * - Полное отключение/включение отображения значков в настройках
 * - Выбор активного значка для пользователей с несколькими бейджами (например, Создатель / Спонсор)
 */
public final class BadgesController {

    public static final BadgesController INSTANCE = new BadgesController();

    public static final String BADGE_CREATOR = "creator";
    public static final String BADGE_SUPPORTER = "supporter";
    public static final String BADGE_NONE = "none";

    public static final long CREATOR_TELEGRAM_ID = 1084312196L;

    private static final String BADGES_URL = "https://raw.githubusercontent.com/IEONDDD/PlusGram/main/badges.json";
    private static final String PREFS_NAME = "plusgram_badges_prefs";
    private static final String KEY_BADGES_ENABLED = "badges_enabled";
    private static final String KEY_MY_SELECTED_BADGE = "my_selected_badge";
    private static final String KEY_CACHED_JSON = "cached_badges_json";

    private final ApiBadgeSource apiBadgeSource = new ApiBadgeSource();
    private final Map<Long, UserBadgeInfo> remoteUsers = new ConcurrentHashMap<>();

    private Bitmap creatorBitmap;
    private Bitmap supporterBitmap;
    private Bitmap scallopBitmap;
    private boolean initialized = false;

    public static class UserBadgeInfo {
        public final long userId;
        public final List<String> badges = new ArrayList<>();
        public String selected;

        public UserBadgeInfo(long userId, String selected) {
            this.userId = userId;
            this.selected = selected;
        }
    }

    private BadgesController() {
        init();
    }

    public synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Default built-in fallback for creator
        UserBadgeInfo creatorInfo = new UserBadgeInfo(CREATOR_TELEGRAM_ID, BADGE_CREATOR);
        creatorInfo.badges.add(BADGE_CREATOR);
        creatorInfo.badges.add(BADGE_SUPPORTER);
        remoteUsers.put(CREATOR_TELEGRAM_ID, creatorInfo);

        // Load cached JSON
        loadCachedJson();

        // Fetch latest badges from GitHub in background
        Utilities.globalQueue.postRunnable(this::fetchRemoteBadges);
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isBadgesEnabled() {
        return prefs().getBoolean(KEY_BADGES_ENABLED, true);
    }

    public void setBadgesEnabled(boolean enabled) {
        prefs().edit().putBoolean(KEY_BADGES_ENABLED, enabled).apply();
    }

    public String getMySelectedBadge() {
        String saved = prefs().getString(KEY_MY_SELECTED_BADGE, null);
        if (saved != null) {
            return saved;
        }
        long self = selfId();
        UserBadgeInfo info = remoteUsers.get(self);
        if (info != null && !TextUtils.isEmpty(info.selected)) {
            return info.selected;
        }
        return BADGE_NONE;
    }

    public void setMySelectedBadge(String badgeId) {
        prefs().edit().putString(KEY_MY_SELECTED_BADGE, badgeId).apply();
    }

    public List<String> getMyAvailableBadges() {
        List<String> list = new ArrayList<>();
        long self = selfId();
        UserBadgeInfo info = remoteUsers.get(self);
        if (info != null && !info.badges.isEmpty()) {
            list.addAll(info.badges);
        } else if (self == CREATOR_TELEGRAM_ID) {
            list.add(BADGE_CREATOR);
            list.add(BADGE_SUPPORTER);
        }
        return list;
    }

    private static long selfId() {
        try {
            TLRPC.User self = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
            return self == null ? 0 : self.id;
        } catch (Exception e) {
            return 0;
        }
    }

    public BadgeDTO getBadge(TLObject object) {
        if (!isBadgesEnabled()) {
            return null;
        }
        if (object instanceof TLRPC.User) {
            return getBadgeForUser(((TLRPC.User) object).id);
        }
        return null;
    }

    public BadgeDTO getBadgeForUser(long userId) {
        if (!isBadgesEnabled() || userId == 0) {
            return null;
        }

        long self = selfId();
        if (userId == self) {
            String selected = getMySelectedBadge();
            if (BADGE_NONE.equals(selected) || TextUtils.isEmpty(selected)) {
                return null;
            }
            return createBadgeDTO(selected);
        }

        UserBadgeInfo info = remoteUsers.get(userId);
        if (info != null && !BADGE_NONE.equals(info.selected) && !TextUtils.isEmpty(info.selected)) {
            return createBadgeDTO(info.selected);
        }

        return null;
    }

    public BadgeDTO createBadgeDTO(String badgeId) {
        if (BADGE_CREATOR.equals(badgeId)) {
            return new BadgeDTO(
                    BADGE_CREATOR,
                    "Создатель Plusgram",
                    "Создатель Plusgram — официальный разработчик проекта."
            );
        } else if (BADGE_SUPPORTER.equals(badgeId)) {
            return new BadgeDTO(
                    BADGE_SUPPORTER,
                    "Спонсор Plusgram",
                    "Этот значок получен за поддержку проекта Plusgram (10 ⭐️). Спасибо за ваш вклад в развитие!"
            );
        }
        return null;
    }

    public synchronized Bitmap getScallopBitmap() {
        if (scallopBitmap == null || scallopBitmap.isRecycled()) {
            try {
                scallopBitmap = BitmapFactory.decodeResource(
                        ApplicationLoader.applicationContext.getResources(),
                        R.drawable.badge_scallop_background
                );
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return scallopBitmap;
    }

    public synchronized Bitmap getCreatorBitmap() {
        if (creatorBitmap == null || creatorBitmap.isRecycled()) {
            try {
                creatorBitmap = BitmapFactory.decodeResource(
                        ApplicationLoader.applicationContext.getResources(),
                        R.drawable.badge_creator_emblem
                );
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return creatorBitmap;
    }

    public synchronized Bitmap getSupporterBitmap() {
        if (supporterBitmap == null || supporterBitmap.isRecycled()) {
            try {
                supporterBitmap = BitmapFactory.decodeResource(
                        ApplicationLoader.applicationContext.getResources(),
                        R.drawable.badge_supporter_emblem
                );
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return supporterBitmap;
    }

    public Drawable getBadgeDrawable(BadgeDTO badge, Theme.ResourcesProvider resourcesProvider) {
        if (badge == null || !isBadgesEnabled()) {
            return null;
        }
        Bitmap bg = getScallopBitmap();
        if (BADGE_CREATOR.equals(badge.getBadgeId())) {
            Bitmap b = getCreatorBitmap();
            return b != null ? new PlusgramBadgeDrawable(bg, b, resourcesProvider) : null;
        } else if (BADGE_SUPPORTER.equals(badge.getBadgeId())) {
            Bitmap b = getSupporterBitmap();
            return b != null ? new PlusgramBadgeDrawable(bg, b, resourcesProvider) : null;
        }
        return null;
    }

    public Drawable getBadgeDrawable(String badgeId, Theme.ResourcesProvider resourcesProvider, int sizeDp) {
        Bitmap bg = getScallopBitmap();
        if (BADGE_CREATOR.equals(badgeId)) {
            Bitmap b = getCreatorBitmap();
            return b != null ? new PlusgramBadgeDrawable(bg, b, resourcesProvider, sizeDp) : null;
        } else if (BADGE_SUPPORTER.equals(badgeId)) {
            Bitmap b = getSupporterBitmap();
            return b != null ? new PlusgramBadgeDrawable(bg, b, resourcesProvider, sizeDp) : null;
        }
        return null;
    }

    public void showBadgeBulletin(BaseFragment fragment, BadgeDTO badge, org.telegram.tgnet.TLObject object,
                                  Theme.ResourcesProvider resourcesProvider, int account) {
        showBadgeBulletin(fragment, badge, object, resourcesProvider, account, null, Boolean.FALSE);
    }

    public void showBadgeBulletin(BaseFragment fragment, BadgeDTO badge, org.telegram.tgnet.TLObject object,
                                  Theme.ResourcesProvider resourcesProvider, int account,
                                  FrameLayout containerLayout, Boolean showButton) {
        if (object == null || badge == null || fragment == null) {
            return;
        }

        Drawable d = getBadgeDrawable(badge, resourcesProvider);
        if (d != null) {
            BulletinFactory factory = containerLayout != null
                    ? BulletinFactory.of(containerLayout, resourcesProvider)
                    : BulletinFactory.of(fragment);
            if (factory != null) {
                factory.createSimpleBulletin(d, badge.getTitle(), badge.getText())
                        .show();
                return;
            }
        }

        // Fallback for custom document ID
        if (badge.getDocumentId() != 0) {
            BulletinFactory factory = containerLayout != null
                    ? BulletinFactory.of(containerLayout, resourcesProvider)
                    : BulletinFactory.of(fragment);
            if (factory != null) {
                factory.createEmojiBulletin(AnimatedEmojiDrawable.findDocument(account, badge.getDocumentId()), badge.getText())
                        .wrapContent()
                        .show();
            }
        }
    }

    private void loadCachedJson() {
        String cached = prefs().getString(KEY_CACHED_JSON, null);
        if (!TextUtils.isEmpty(cached)) {
            parseBadgesJson(cached);
        }
    }

    private void fetchRemoteBadges() {
        try {
            URL url = new URL(BADGES_URL + "?t=" + System.currentTimeMillis());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setUseCaches(false);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                reader.close();
                String json = sb.toString();
                if (!TextUtils.isEmpty(json)) {
                    prefs().edit().putString(KEY_CACHED_JSON, json).apply();
                    parseBadgesJson(json);
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void parseBadgesJson(String jsonString) {
        try {
            JSONObject root = new JSONObject(jsonString);
            JSONObject users = root.optJSONObject("users");
            if (users != null) {
                java.util.Iterator<String> keys = users.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    long uid = Long.parseLong(key);
                    JSONObject userObj = users.getJSONObject(key);
                    String selected = userObj.optString("selected", BADGE_NONE);
                    UserBadgeInfo info = new UserBadgeInfo(uid, selected);
                    JSONArray arr = userObj.optJSONArray("badges");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            info.badges.add(arr.getString(i));
                        }
                    }
                    remoteUsers.put(uid, info);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public ApiBadgeSource getApiBadgeSource() {
        return apiBadgeSource;
    }
}
