package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.plusgram.app.updater.GitHubUpdater;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.voip.CellFlickerDrawable;

import tw.nekomimi.nekogram.TextViewEffects;
import tw.nekomimi.nekogram.helpers.remote.UpdateHelper;

public class BlockingUpdateView extends FrameLayout {

    private TextView textView;
    private TextView acceptTextView;
    private FrameLayout acceptButton;
    private ScrollView scrollView;

    private int pressCount;

    public BlockingUpdateView(final Context context) {
        super(context);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        int top = Build.VERSION.SDK_INT >= 21 ? (int) (AndroidUtilities.statusBarHeight / AndroidUtilities.density) : 0;

        FrameLayout view = new FrameLayout(context);
        addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(176) + (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0)));

        ImageView imageView = new ImageView(context);
        imageView.setImageResource(R.mipmap.ic_launcher);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setPadding(0, 0, 0, AndroidUtilities.dp(14));
        view.addView(imageView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, top, 0, 0));
        imageView.setOnClickListener(v -> {
            pressCount++;
            if (pressCount >= 10) {
                setVisibility(GONE);
                SharedConfig.pendingAppUpdate = null;
                SharedConfig.saveConfig();
            }
        });

        FrameLayout container = new FrameLayout(context);
        scrollView = new ScrollView(context);
        AndroidUtilities.setScrollViewEdgeEffectColor(scrollView, Theme.getColor(Theme.key_actionBarDefault));
        scrollView.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16));
        scrollView.setClipToPadding(false);
        addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 27, 178 + top, 27, 130));

        scrollView.addView(container);

        TextView titleTextView = new TextView(context);
        titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleTextView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        titleTextView.setTypeface(AndroidUtilities.bold());
        titleTextView.setText(LocaleController.getString(R.string.UpdateTelegram).replace("Telegram", LocaleController.getString(R.string.NagramX)));
        container.addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP));

        textView = new TextViewEffects(context);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setGravity(Gravity.LEFT | Gravity.TOP);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        container.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 44, 0, 0));

        acceptButton = new FrameLayout(context) {
            CellFlickerDrawable cellFlickerDrawable;

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                if (cellFlickerDrawable == null) {
                    cellFlickerDrawable = new CellFlickerDrawable();
                    cellFlickerDrawable.drawFrame = false;
                    cellFlickerDrawable.repeatProgress = 2f;
                }
                cellFlickerDrawable.setParentWidth(getMeasuredWidth());
                AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                cellFlickerDrawable.draw(canvas, AndroidUtilities.rectTmp, AndroidUtilities.dp(4), null);
                invalidate();
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int size = MeasureSpec.getSize(widthMeasureSpec);
                if (size > AndroidUtilities.dp(260)) {
                    super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(320), MeasureSpec.EXACTLY), heightMeasureSpec);
                } else {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                }
            }
        };
        acceptButton.setPadding(AndroidUtilities.dp(34), 0, AndroidUtilities.dp(34), 0);
        acceptButton.setBackgroundDrawable(Theme.AdaptiveRipple.filledRectByKey(Theme.key_featuredStickers_addButton, 4));
        acceptButton.setPadding(AndroidUtilities.dp(34), 0, AndroidUtilities.dp(34), 0);
        addView(acceptButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 46, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0, 0, 45));
        acceptButton.setOnClickListener(view1 -> {
            Browser.openUrlInSystemBrowser(getContext(), GitHubUpdater.RELEASES_URL);
        });

        acceptTextView = new TextView(context);
        acceptTextView.setGravity(Gravity.CENTER);
        acceptTextView.setTypeface(AndroidUtilities.bold());
        acceptTextView.setTextColor(0xffffffff);
        acceptTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        acceptButton.addView(acceptTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
    }

    public void show(int account, TLRPC.TL_help_appUpdate update, boolean check) {
        pressCount = 0;
        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(update.text);
        MessageObject.addEntitiesToText(builder, update.entities, false, false, false, false);
        MessageObject.replaceAnimatedEmoji(builder, update.entities, textView.getPaint().getFontMetricsInt());
        textView.setText(builder);
        acceptTextView.setText(LocaleController.getString(R.string.OEUpdaterOpenRelease));
        if (check) {
            UpdateHelper.getInstance().checkNewVersionAvailable((response, error) -> AndroidUtilities.runOnUIThread(() -> {
                if (response != null) {
                    if (!response.can_not_skip) {
                        setVisibility(GONE);
                        SharedConfig.pendingAppUpdate = null;
                        SharedConfig.saveConfig();
                    }
                }
            }));
        }
    }

    Drawable gradientDrawableTop = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {Theme.getColor(Theme.key_windowBackgroundWhite), Color.TRANSPARENT });
    Drawable gradientDrawableBottom = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[] {Theme.getColor(Theme.key_windowBackgroundWhite), Color.TRANSPARENT });

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        gradientDrawableTop.setBounds(scrollView.getLeft(), scrollView.getTop(), scrollView.getRight(), scrollView.getTop() + AndroidUtilities.dp(16));
        gradientDrawableTop.draw(canvas);

        gradientDrawableBottom.setBounds(scrollView.getLeft(), scrollView.getBottom() - AndroidUtilities.dp(18), scrollView.getRight(), scrollView.getBottom());
        gradientDrawableBottom.draw(canvas);
    }

}
