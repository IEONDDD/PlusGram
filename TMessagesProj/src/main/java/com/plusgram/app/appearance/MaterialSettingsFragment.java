package com.plusgram.app.appearance;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Components.Switch;
import java.util.function.Consumer;
import java.util.function.Predicate;
import static org.telegram.messenger.LocaleController.getString;

/** Accessible, wrapping cards using the active Telegram/Monet color roles. */
public abstract class MaterialSettingsFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    protected Context context;
    protected LinearLayout content;
    protected ScrollView scroll;
    protected abstract int titleRes();
    protected abstract void buildContent();
    @Override public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetNewTheme);
        return super.onFragmentCreate();
    }
    @Override public void onFragmentDestroy() {
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetNewTheme);
        super.onFragmentDestroy();
    }
    @Override public void didReceivedNotification(int id, int account, Object... args) {
        if (scroll != null) {
            scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
            rebuild();
        }
    }

    @Override public View createView(Context context) {
        this.context = context;
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(getString(titleRes()));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        content = new LinearLayout(context) {
            @Override protected void onMeasure(int width, int height) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(width), dp(720)),
                        MeasureSpec.EXACTLY), height);
            }
        };
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));
        content.setLayoutDirection(LocaleController.isRTL ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        ScrollView.LayoutParams params = new ScrollView.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        scroll.addView(content, params);
        fragmentView = scroll;
        buildContent();
        return fragmentView;
    }
    protected int dp(int n) { return AndroidUtilities.dp(n); }
    protected TextView text(String value, int size, int color) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(Theme.getColor(color));
        text.setGravity(Gravity.START);
        return text;
    }
    protected LinearLayout card(int title) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int radius = AppearanceConfig.sectionRadius();
        card.setBackground(Theme.createRoundRectDrawable(dp(Math.max(8, radius)), Theme.getColor(Theme.key_windowBackgroundWhite)));
        card.setClipToOutline(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(16);
        content.addView(card, params);
        if (title != 0) {
            TextView heading = text(getString(title), 15, Theme.key_windowBackgroundWhiteBlueHeader);
            heading.setTypeface(null, Typeface.BOLD);
            heading.setPadding(dp(20), dp(20), dp(20), dp(8));
            card.addView(heading);
        }
        return card;
    }
    protected TextView row(LinearLayout card, int title, String value, Runnable click) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(MotionConfig.rowHeight()));
        row.setPadding(dp(20), dp(14), dp(20), dp(14));
        TextView label = text(getString(title), 17, Theme.key_windowBackgroundWhiteBlackText);
        row.addView(label, new LinearLayout.LayoutParams(-1, -2));
        TextView detail = text(value, 14, Theme.key_windowBackgroundWhiteGrayText);
        if (value != null) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = dp(5);
            row.addView(detail, p);
        }
        if (click != null) {
            row.setBackground(new RippleDrawable(ColorStateList.valueOf(Theme.getColor(Theme.key_listSelector)), null,
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)));
            row.setOnClickListener(v -> { MotionConfig.feedback(v); click.run(); });
            row.setFocusable(true);
        }
        card.addView(row);
        return detail;
    }
    protected void toggle(LinearLayout card, int title, boolean checked, Consumer<Boolean> changed) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(MotionConfig.rowHeight()));
        row.setPadding(dp(20), dp(12), dp(20), dp(12));
        TextView label = text(getString(title), 17, Theme.key_windowBackgroundWhiteBlackText);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        Switch control = new Switch(context);
        control.setChecked(checked, false);
        control.setClickable(false);
        control.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(52), dp(40));
        p.setMarginStart(dp(16));
        row.addView(control, p);
        row.setContentDescription(getString(title));
        row.setFocusable(true);
        row.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.Switch");
                info.setCheckable(true);
                info.setChecked(control.isChecked());
            }
        });
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(Theme.getColor(Theme.key_listSelector)), null,
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)));
        row.setOnClickListener(v -> {
            boolean next = !control.isChecked();
            control.setChecked(next, MotionConfig.enabled());
            MotionConfig.feedback(v);
            changed.accept(next);
        });
        card.addView(row);
    }
    protected void footer(int resource) {
        TextView view = text(getString(resource), 14, Theme.key_windowBackgroundWhiteGrayText);
        view.setPadding(dp(8), dp(4), dp(8), dp(20));
        content.addView(view);
    }
    protected void rebuild() {
        if (content == null) return;
        int y = scroll.getScrollY();
        content.removeAllViews();
        buildContent();
        scroll.post(() -> scroll.scrollTo(0, y));
    }
    protected void choose(int title, String[] values, Consumer<Integer> selected) {
        showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(title))
                .setItems(values, (dialog, which) -> { selected.accept(which); rebuild(); })
                .setNegativeButton(getString(R.string.Cancel), null).create());
    }
    protected void edit(int title, String current, int inputType, Predicate<String> save) {
        org.telegram.ui.Components.EditTextBoldCursor input = new org.telegram.ui.Components.EditTextBoldCursor(context);
        input.setTextSize(18);
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setInputType(inputType);
        input.setText(current);
        input.setPadding(dp(24), dp(12), dp(24), dp(12));
        AlertDialog dialog = new AlertDialog.Builder(getParentActivity()).setTitle(getString(title)).setView(input)
                .setPositiveButton(getString(R.string.Save), (d, which) -> {})
                .setNegativeButton(getString(R.string.Cancel), null).create();
        showDialog(dialog);
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (save.test(input.getText().toString().trim())) { dialog.dismiss(); rebuild(); }
            else input.setError(getString(R.string.PlusgramInvalidValue));
        });
    }
    @Override public void onResume() { super.onResume(); rebuild(); }
}
