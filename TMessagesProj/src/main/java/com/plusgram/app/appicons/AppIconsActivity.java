package com.plusgram.app.appicons;

import static org.telegram.messenger.LocaleController.getString;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Components.AppIconBulletinLayout;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceColor;
import org.telegram.ui.LauncherIconController.LauncherIcon;
import org.telegram.ui.PremiumPreviewFragment;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.util.ArrayList;

public class AppIconsActivity extends BaseFragment {

    private static final int PLUSGRAM_ICON = 0;
    private static final int OTHER_ICONS_HEADER = 1;
    private static final int OTHER_ICON = 2;

    private final ArrayList<LauncherIcon> icons = new ArrayList<>();
    private final Runnable onIconChanged;
    private int brandCount;
    private boolean applying;

    private LauncherIcon appliedIcon;
    private LauncherIcon previewIcon;

    private ContentView contentView;
    private AppIconHeroView heroView;
    private RecyclerListView listView;
    private GridLayoutManager layoutManager;
    private FrameLayout buttonContainer;
    private ButtonWithCounterView button;

    private int tint;
    private ValueAnimator tintAnimator;
    private BlurredBackgroundSourceColor glassSource;
    private int heroHeight;
    private int bottomInset;
    private float collapse;
    private float topFade;

    public AppIconsActivity(Runnable onIconChanged) {
        this.onIconChanged = onIconChanged;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    public View createView(Context context) {
        icons.clear();
        icons.addAll(AppIcons.available(currentAccount));
        brandCount = 0;
        for (LauncherIcon icon : icons) {
            if (icon.isPlusgram()) {
                brandCount++;
            }
        }
        appliedIcon = previewIcon = AppIcons.current();
        tint = AppIcons.tint(AppIcons.accent(context, previewIcon));

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(false);
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);
        actionBar.setItemsColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText), false);
        actionBar.setItemsBackgroundColor(getThemedColor(Theme.key_listSelector), false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        contentView = new ContentView(context);

        glassSource = new BlurredBackgroundSourceColor();
        glassSource.setColor(topColor());
        actionBar.setupGlass(new BlurredBackgroundDrawableViewFactory(glassSource),
                BlurredBackgroundProviderImpl.topPanelChatActivity(getResourceProvider()));
        actionBar.setGlassOnlyBack();

        heroView = new AppIconHeroView(context, getResourceProvider());
        heroView.set(previewIcon);
        contentView.addView(heroView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        listView = new RecyclerListView(context, getResourceProvider());
        layoutManager = new GridLayoutManager(context, 4);
        listView.setLayoutManager(layoutManager);
        Adapter adapter = new Adapter();
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int span = layoutManager.getSpanCount();
                switch (adapter.getItemViewType(position)) {
                    case PLUSGRAM_ICON:
                        return span / (span >= 8 ? 4 : 2);
                    case OTHER_ICONS_HEADER:
                        return span;
                    default:
                        return 1;
                }
            }
        });
        listView.setAdapter(adapter);
        listView.setClipToPadding(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setSelectorDrawableColor(Color.TRANSPARENT);
        listView.setOnItemClickListener((view, position) -> {
            if (!applying && view instanceof AppIconCell) {
                preview(((AppIconCell) view).getIcon());
            }
        });
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateCollapse();
            }
        });
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        button = new ButtonWithCounterView(context, true, getResourceProvider());
        button.setRoundRadius(24);
        button.setOnClickListener(v -> apply());
        updateButton(false);
        buttonContainer = new FrameLayout(context);
        buttonContainer.addView(button, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 16, 12, 16, 12));
        buttonContainer.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> updateListPadding());
        contentView.addView(buttonContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));

        contentView.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        updateButtonFade();
        fragmentView = contentView;
        return contentView;
    }

    @Override
    public void onFragmentDestroy() {
        cancelTintAnimation();
        super.onFragmentDestroy();
    }

    private void cancelTintAnimation() {
        if (tintAnimator != null) {
            tintAnimator.removeAllListeners();
            tintAnimator.removeAllUpdateListeners();
            tintAnimator.cancel();
            tintAnimator = null;
        }
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        bottomInset = bottom;
        if (contentView != null) {
            contentView.requestLayout();
        }
        updateListPadding();
    }

    @Override
    public boolean isLightStatusBar() {
        return AndroidUtilities.computePerceivedBrightness(topColor()) > 0.6f;
    }

    private void preview(LauncherIcon icon) {
        if (icon == previewIcon) {
            return;
        }
        previewIcon = icon;
        heroView.set(icon);
        animateTint(AppIcons.tint(AppIcons.accent(getContext(), icon)));
        updateButton(true);
        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            if (child instanceof AppIconCell) {
                AppIconCell cell = (AppIconCell) child;
                cell.set(cell.getIcon(), cell.getIcon() == previewIcon, true);
            }
        }
    }

    private void apply() {
        if (applying || previewIcon == null) {
            return;
        }
        if (previewIcon == appliedIcon) {
            finishFragment();
            return;
        }
        if (AppIcons.locked(previewIcon)) {
            showDialog(new PremiumFeatureBottomSheet(this, PremiumPreviewFragment.PREMIUM_FEATURE_APPLICATION_ICONS, true));
            return;
        }
        LauncherIcon selected = previewIcon;
        applying = true;
        button.setLoading(true);
        AppIcons.apply(selected, () -> {
            appliedIcon = selected;
            applying = false;
            if (onIconChanged != null) {
                onIconChanged.run();
            }
            if (getParentActivity() != null) {
                button.setLoading(false);
                updateButton(true);
                Bulletin.make(this, new AppIconBulletinLayout(getContext(), selected, getResourceProvider()),
                        Bulletin.DURATION_SHORT).show();
            }
        }, () -> {
            applying = false;
            if (getParentActivity() != null) {
                button.setLoading(false);
                showDialog(new AlertDialog.Builder(getContext(), getResourceProvider())
                        .setMessage(getString(R.string.PlusgramAppIconChangeFailed))
                        .setPositiveButton(getString(R.string.OK), null).create());
            }
        });
    }

    private void updateButton(boolean animated) {
        if (button != null) {
            String label = getString(previewIcon == appliedIcon ? R.string.AppIconsKeep : R.string.AppIconsSelect);
            button.setText(label, animated);
            button.setContentDescription(label);
        }
    }

    private int baseColor() {
        int gray = getThemedColor(Theme.key_windowBackgroundGray);
        return tint == 0 ? gray : ColorUtils.blendARGB(gray, tint, 0.25f);
    }

    private int topColor() {
        return tint == 0 ? baseColor() : ColorUtils.blendARGB(baseColor(), tint, 0.7f);
    }

    private void animateTint(int to) {
        cancelTintAnimation();
        if (tint == to) {
            return;
        }
        int from = tint;
        if (from == 0 || to == 0) {
            tint = to;
            invalidateTop();
            updateStatusBar();
            return;
        }
        tintAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration(280);
        tintAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        tintAnimator.addUpdateListener(a -> {
            tint = ColorUtils.blendARGB(from, to, (float) a.getAnimatedValue());
            invalidateTop();
        });
        tintAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tint = to;
                updateStatusBar();
            }
        });
        tintAnimator.start();
    }

    private void invalidateTop() {
        contentView.invalidate();
        if (glassSource != null) {
            glassSource.setColor(topColor());
            actionBar.invalidate();
        }
    }

    private void updateStatusBar() {
        if (getParentActivity() != null) {
            AndroidUtilities.setLightStatusBar(getParentActivity().getWindow(), isLightStatusBar());
        }
    }

    private void updateButtonFade() {
        int color = getThemedColor(Theme.key_windowBackgroundWhite);
        buttonContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ColorUtils.setAlphaComponent(color, 0), color, color}));
    }

    private void updateListPadding() {
        if (listView == null) {
            return;
        }
        int side = AndroidUtilities.dp(8);
        int top = heroHeight + AndroidUtilities.dp(10);
        int bottom = buttonContainer != null && buttonContainer.getHeight() > 0
                ? buttonContainer.getHeight() : bottomInset + AndroidUtilities.dp(72);
        if (listView.getPaddingTop() != top || listView.getPaddingBottom() != bottom || listView.getPaddingLeft() != side) {
            listView.setPadding(side, top, side, bottom);
        }
        updateCollapse();
    }

    private int collapseRange() {
        return Math.max(0, heroHeight - ActionBar.getCurrentActionBarHeight() - AndroidUtilities.statusBarHeight);
    }

    private void updateCollapse() {
        if (listView == null || heroView == null) {
            return;
        }
        int scrolled = listView.getChildCount() > 0 ? Integer.MAX_VALUE / 2 : 0;
        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            if (listView.getChildAdapterPosition(child) == 0) {
                scrolled = listView.getPaddingTop() - child.getTop();
                break;
            }
        }
        scrolled = Math.max(0, scrolled);
        int range = collapseRange();
        float newCollapse = range <= 0 ? 0f : Math.min(1f, scrolled / (float) range);
        float newFade = Math.min(1f, scrolled / (float) AndroidUtilities.dp(16));
        if (Math.abs(newCollapse - collapse) > 0.0005f || Math.abs(newFade - topFade) > 0.0005f) {
            collapse = newCollapse;
            topFade = newFade;
            heroView.setCollapse(collapse);
            contentView.invalidate();
        }
    }

    private int spanCount(int width) {
        return width >= AndroidUtilities.dp(640) ? 8 : 4;
    }

    private class ContentView extends FrameLayout {

        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path panelPath = new Path();
        private final float[] radii = new float[8];
        private int glowColor;
        private float glowCy;
        private int glowWidth;
        private GradientDrawable topFadeDrawable;
        private int topFadeColor;

        ContentView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            heroView.setPadding(0, AndroidUtilities.statusBarHeight + AndroidUtilities.dp(28), 0, AndroidUtilities.dp(40));
            int available = (int) (MeasureSpec.getSize(heightMeasureSpec) * 0.45f) - AndroidUtilities.statusBarHeight
                    - AndroidUtilities.dp(68) - heroView.getTextBlockHeight();
            heroView.setPreviewSizeDp(Math.max(64, Math.min(AppIconHeroView.PREVIEW_DP, (int) (available / AndroidUtilities.density))));
            buttonContainer.setPadding(0, 0, 0, bottomInset);
            int span = spanCount(MeasureSpec.getSize(widthMeasureSpec));
            if (layoutManager.getSpanCount() != span) {
                layoutManager.setSpanCount(span);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int height = heroView.getMeasuredHeight();
            if (height > 0 && height != heroHeight) {
                heroHeight = height;
                AndroidUtilities.runOnUIThread(AppIconsActivity.this::updateListPadding);
            }
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            updateCollapse();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(baseColor());
            if (tint == 0 || collapse >= 1f || heroHeight <= 0) {
                return;
            }
            float cy = heroView.previewCenterY();
            if (glowColor != tint || glowWidth != getWidth() || glowCy != cy) {
                glowColor = tint;
                glowWidth = getWidth();
                glowCy = cy;
                float radius = Math.max(getWidth(), heroHeight) * 0.9f;
                glowPaint.setShader(new RadialGradient(getWidth() / 2f, cy, radius,
                        new int[]{ColorUtils.setAlphaComponent(tint, 200), ColorUtils.setAlphaComponent(tint, 0)},
                        new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            }
            glowPaint.setAlpha((int) ((1f - collapse) * 255));
            canvas.drawRect(0, 0, getWidth(), getHeight(), glowPaint);
        }

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            if (child != listView) {
                return super.drawChild(canvas, child, drawingTime);
            }
            float radius = AndroidUtilities.dp(20);
            radii[0] = radii[1] = radii[2] = radii[3] = radius;
            float top = listView.getY() + heroHeight - collapseRange() * collapse;
            AndroidUtilities.rectTmp.set(0, top, getWidth(), getHeight());
            panelPath.rewind();
            panelPath.addRoundRect(AndroidUtilities.rectTmp, radii, Path.Direction.CW);
            int white = getThemedColor(Theme.key_windowBackgroundWhite);
            panelPaint.setColor(white);
            canvas.drawPath(panelPath, panelPaint);
            canvas.save();
            canvas.clipPath(panelPath);
            boolean result = super.drawChild(canvas, child, drawingTime);
            if (topFade > 0f) {
                if (topFadeDrawable == null || topFadeColor != white) {
                    topFadeColor = white;
                    topFadeDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{white, ColorUtils.setAlphaComponent(white, 0)});
                }
                topFadeDrawable.setBounds(0, (int) top, getWidth(), (int) top + AndroidUtilities.dp(24));
                topFadeDrawable.setAlpha((int) (topFade * 255));
                topFadeDrawable.draw(canvas);
            }
            canvas.restore();
            return result;
        }
    }

    private class Adapter extends RecyclerListView.SelectionAdapter {

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.itemView instanceof AppIconCell;
        }

        @Override
        public int getItemCount() {
            return icons.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position < brandCount) {
                return PLUSGRAM_ICON;
            }
            return position == brandCount ? OTHER_ICONS_HEADER : OTHER_ICON;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View cell;
            if (viewType == OTHER_ICONS_HEADER) {
                HeaderCell header = new HeaderCell(parent.getContext(), getResourceProvider());
                header.setText(getString(R.string.PlusgramOtherAppIcons));
                cell = header;
            } else {
                cell = new AppIconCell(parent.getContext(), getResourceProvider(), viewType == PLUSGRAM_ICON);
            }
            cell.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.itemView instanceof AppIconCell) {
                LauncherIcon icon = icons.get(position < brandCount ? position : position - 1);
                ((AppIconCell) holder.itemView).set(icon, icon == previewIcon, false);
            }
        }
    }
}
