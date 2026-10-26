package com.plusgram.app.badges;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * Адаптивный значок (бейдж) профиля Plusgram.
 * Волнистая розетка-бейдж автоматически адаптируется под текущий акцентный цвет темы (Material You),
 * а символ (молоточек/1+) рисуется чистым белым цветом в центре.
 */
public class PlusgramBadgeDrawable extends Drawable {

    private final Bitmap scallopBackgroundBitmap;
    private final Bitmap emblemBitmap;
    private final Theme.ResourcesProvider resourcesProvider;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private int customColor = 0;
    private int sizeDp = 20;

    public PlusgramBadgeDrawable(Bitmap scallopBackgroundBitmap, Bitmap emblemBitmap, Theme.ResourcesProvider resourcesProvider) {
        this.scallopBackgroundBitmap = scallopBackgroundBitmap;
        this.emblemBitmap = emblemBitmap;
        this.resourcesProvider = resourcesProvider;
    }

    public PlusgramBadgeDrawable(Bitmap scallopBackgroundBitmap, Bitmap emblemBitmap, Theme.ResourcesProvider resourcesProvider, int sizeDp) {
        this.scallopBackgroundBitmap = scallopBackgroundBitmap;
        this.emblemBitmap = emblemBitmap;
        this.resourcesProvider = resourcesProvider;
        this.sizeDp = sizeDp;
    }

    // Constructor backward compatibility
    public PlusgramBadgeDrawable(Bitmap emblemBitmap, Theme.ResourcesProvider resourcesProvider) {
        this(null, emblemBitmap, resourcesProvider);
    }

    public PlusgramBadgeDrawable(Bitmap emblemBitmap, Theme.ResourcesProvider resourcesProvider, int sizeDp) {
        this(null, emblemBitmap, resourcesProvider, sizeDp);
    }

    public void setCustomColor(int color) {
        this.customColor = color;
        invalidateSelf();
    }

    public int getAccentColor() {
        if (customColor != 0) {
            return customColor;
        }
        int color = Theme.getColor(Theme.key_chats_actionBackground, resourcesProvider);
        if (color == 0 || color == 0xFFFFFFFF || color == 0x00000000) {
            color = Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider);
        }
        if (color == 0 || color == 0xFFFFFFFF || color == 0x00000000) {
            color = Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider);
        }
        if (color == 0 || color == 0xFFFFFFFF || color == 0x00000000) {
            color = 0xFF2196F3;
        }
        return color;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }

        int accentColor = getAccentColor();

        if (scallopBackgroundBitmap != null && !scallopBackgroundBitmap.isRecycled()) {
            bgPaint.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(scallopBackgroundBitmap, null, bounds, bgPaint);
        } else {
            bgPaint.setColorFilter(null);
            bgPaint.setColor(accentColor);
            float cx = bounds.exactCenterX();
            float cy = bounds.exactCenterY();
            float radius = Math.min(bounds.width(), bounds.height()) / 2f;
            canvas.drawCircle(cx, cy, radius, bgPaint);
        }

        if (emblemBitmap != null && !emblemBitmap.isRecycled()) {
            canvas.drawBitmap(emblemBitmap, null, bounds, bitmapPaint);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return AndroidUtilities.dp(sizeDp);
    }

    @Override
    public int getIntrinsicHeight() {
        return AndroidUtilities.dp(sizeDp);
    }

    @Override
    public void setAlpha(int alpha) {
        bgPaint.setAlpha(alpha);
        bitmapPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
