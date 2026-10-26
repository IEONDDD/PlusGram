package com.plusgram.app.badges;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.exteragram.messenger.api.dto.BadgeDTO;
import com.exteragram.messenger.badges.BadgesController;
import com.plusgram.app.appearance.AppearanceConfig;
import com.plusgram.app.appearance.MaterialSettingsFragment;
import com.plusgram.app.appearance.MotionConfig;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

import java.util.List;

import static org.telegram.messenger.LocaleController.getString;

public class BadgesSettingsActivity extends MaterialSettingsFragment {

    @Override
    protected int titleRes() {
        return R.string.PlusgramBadgesTitle;
    }

    @Override
    protected void buildContent() {
        // 1. Live Preview Card
        buildPreviewCard();

        // 2. Global Toggle Card
        LinearLayout generalCard = card(0);
        toggle(generalCard, R.string.PlusgramBadgesEnable, BadgesController.INSTANCE.isBadgesEnabled(), enabled -> {
            BadgesController.INSTANCE.setBadgesEnabled(enabled);
            rebuild();
        });
        footer(R.string.PlusgramBadgesEnableInfo);

        // 3. My Badge Selection Card (if user has available badges)
        List<String> available = BadgesController.INSTANCE.getMyAvailableBadges();
        if (!available.isEmpty() && BadgesController.INSTANCE.isBadgesEnabled()) {
            LinearLayout selectionCard = card(R.string.PlusgramBadgesMyBadge);
            String current = BadgesController.INSTANCE.getMySelectedBadge();

            for (String badgeId : available) {
                String title;
                if (BadgesController.BADGE_CREATOR.equals(badgeId)) {
                    title = "🔨 " + getString(R.string.PlusgramBadgeCreator);
                } else if (BadgesController.BADGE_SUPPORTER.equals(badgeId)) {
                    title = "⭐️ " + getString(R.string.PlusgramBadgeSupporter);
                } else {
                    title = badgeId;
                }
                boolean isSelected = badgeId.equals(current);
                customRow(selectionCard, title, isSelected ? "✓ Активен" : null, () -> {
                    BadgesController.INSTANCE.setMySelectedBadge(badgeId);
                    rebuild();
                });
            }

            boolean isNoneSelected = BadgesController.BADGE_NONE.equals(current);
            customRow(selectionCard, "🚫 " + getString(R.string.PlusgramBadgeNone), isNoneSelected ? "✓ Скрыт" : null, () -> {
                BadgesController.INSTANCE.setMySelectedBadge(BadgesController.BADGE_NONE);
                rebuild();
            });
        }

        // 4. Support Card (Stars)
        LinearLayout supportCard = card(R.string.PlusgramBadgesSupport);
        row(supportCard, R.string.PlusgramBadgesSupport, getString(R.string.PlusgramBadgesSupportInfo), () -> {
            Browser.openUrl(getParentActivity(), "https://t.me/PlusgramStars_bot");
        });

        // 5. Info Footer
        footer(R.string.PlusgramBadgesColorInfo);
    }

    private void buildPreviewCard() {
        LinearLayout previewCard = new LinearLayout(context);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setGravity(Gravity.CENTER_HORIZONTAL);
        int radius = AppearanceConfig.sectionRadius();
        previewCard.setBackground(Theme.createRoundRectDrawable(dp(Math.max(8, radius)), Theme.getColor(Theme.key_windowBackgroundWhite)));
        previewCard.setPadding(dp(20), dp(24), dp(20), dp(24));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(16);
        content.addView(previewCard, params);

        TLRPC.User self = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();

        // Avatar
        BackupImageView avatarView = new BackupImageView(context);
        avatarView.setRoundRadius(dp(40));
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        if (self != null) {
            avatarDrawable.setInfo(UserConfig.selectedAccount, self);
            avatarView.setForUserOrChat(self, avatarDrawable);
        }
        previewCard.addView(avatarView, LayoutHelper.createLinear(80, 80, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));

        // Name + Badge container
        LinearLayout nameRow = new LinearLayout(context);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER);

        TextView nameText = new TextView(context);
        nameText.setTextSize(18);
        nameText.setTypeface(AndroidUtilities.bold());
        nameText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameText.setText(self != null ? ContactsController.formatName(self.first_name, self.last_name) : "Plusgram");
        nameRow.addView(nameText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        BadgeDTO badge = BadgesController.INSTANCE.getBadge(self);
        if (badge != null) {
            ImageView badgeView = new ImageView(context);
            Drawable d = BadgesController.INSTANCE.getBadgeDrawable(badge, null);
            badgeView.setImageDrawable(d);
            nameRow.addView(badgeView, LayoutHelper.createLinear(22, 22, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

            badgeView.setOnClickListener(v -> {
                MotionConfig.feedback(v);
                BadgesController.INSTANCE.showBadgeBulletin(this, badge, self, null, UserConfig.selectedAccount);
            });
        }
        previewCard.addView(nameRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 4));

        TextView hint = new TextView(context);
        hint.setTextSize(13);
        hint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        hint.setText("Предпросмотр значка в профиле (нажмите для проверки)");
        previewCard.addView(hint, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
    }

    private TextView customRow(LinearLayout card, String title, String value, Runnable click) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(MotionConfig.rowHeight()));
        row.setPadding(dp(20), dp(14), dp(20), dp(14));
        TextView label = text(title, 17, Theme.key_windowBackgroundWhiteBlackText);
        row.addView(label, new LinearLayout.LayoutParams(-1, -2));
        TextView detail = text(value, 14, Theme.key_windowBackgroundWhiteValueText);
        if (value != null) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = dp(5);
            row.addView(detail, p);
        }
        if (click != null) {
            row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
            row.setOnClickListener(v -> {
                MotionConfig.feedback(v);
                click.run();
            });
            row.setFocusable(true);
        }
        card.addView(row);
        return detail;
    }
}
