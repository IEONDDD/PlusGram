package com.plusgram.app.appearance;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import tw.nekomimi.nekogram.NekoConfig;

/** Weather drawn behind the chat list title and controls. */
public final class HeaderWeatherEffect {
    public static final int AURORA = 4;
    public static final int THUNDERSTORM = 5;
    public static final int RAIN = 6;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final long started = SystemClock.uptimeMillis();
    private LinearGradient auroraColors;
    private int gradientWidth;
    private boolean gradientDark;

    public static boolean isWeather(int effect) {
        return effect >= AURORA && effect <= RAIN;
    }

    public static String[] options() {
        return new String[]{
                LocaleController.getString(R.string.DependsOnDate),
                LocaleController.getString(R.string.Snowflakes),
                LocaleController.getString(R.string.Fireworks),
                LocaleController.getString(R.string.DecorationNone),
                LocaleController.getString(R.string.PlusgramHeaderAurora),
                LocaleController.getString(R.string.PlusgramHeaderThunderstorm),
                LocaleController.getString(R.string.PlusgramHeaderRain)
        };
    }

    public static String selectedLabel() {
        String[] labels = options();
        int index = NekoConfig.actionBarDecoration.Int();
        return labels[index >= 0 && index < labels.length ? index : 0];
    }

    public void draw(View view, Canvas canvas, int effect, int top, int bottom, boolean dark) {
        int width = view.getWidth();
        int height = bottom - top;
        if (width <= 0 || height <= 0) return;
        boolean animate = MotionConfig.enabled();
        float seconds = animate ? (SystemClock.uptimeMillis() - started) / 1000f : 0f;
        canvas.save();
        canvas.clipRect(0, top, width, bottom);
        canvas.translate(0, top);
        if (effect == AURORA) {
            drawAurora(canvas, width, height, seconds, dark);
        } else {
            drawRain(canvas, width, height, seconds, dark, effect == THUNDERSTORM);
        }
        canvas.restore();
        if (animate && view.isShown() && view.getWindowVisibility() == View.VISIBLE) {
            view.postInvalidateDelayed(33);
        }
    }

    private void drawAurora(Canvas canvas, int width, int height, float seconds, boolean dark) {
        if (auroraColors == null || width != gradientWidth || dark != gradientDark) {
            gradientWidth = width;
            gradientDark = dark;
            auroraColors = new LinearGradient(0, 0, width, 0,
                    dark ? new int[]{0x0020D5BA, 0xAA47E4BD, 0xAAAC80FF, 0x006A78FF}
                            : new int[]{0x00209F86, 0xAA158D7A, 0xAA7443AE, 0x005148B4},
                    null, Shader.TileMode.CLAMP);
        }
        paint.setShader(auroraColors);
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 5; i++) {
            float wave = (float) Math.sin(seconds * .8 + i * .48) * height * .12f;
            float y = height * (.30f + i * .085f);
            path.rewind();
            path.moveTo(0, y + wave);
            path.cubicTo(width * .24f, y - height * .38f - wave,
                    width * .58f, y + height * .44f + wave, width, y - wave);
            paint.setStrokeWidth(height * (.15f - i * .019f));
            paint.setAlpha(51 + i * 12);
            canvas.drawPath(path, paint);
        }
        paint.setShader(null);
    }

    private void drawRain(Canvas canvas, int width, int height, float seconds, boolean dark, boolean storm) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(AndroidUtilities.dpf2(storm ? 1.25f : 1f));
        int drops = storm ? 38 : 25;
        for (int i = 0; i < drops; i++) {
            float length = height * (.10f + (i % 5) * .021f);
            float x = width * ((i * 43) % 97) / 97f;
            float y = ((i * 17f + seconds * (55 + i % 7 * 13)) % 100) * (height + length * 2) / 100f - length;
            paint.setColor(dark ? 0xFF9FCAFF : 0xFF32689C);
            paint.setAlpha(35 + i % 5 * 14);
            canvas.drawLine(x, y, x - length * .30f, y + length, paint);
        }
        if (storm) {
            float cycle = seconds % 8f;
            float glow = seconds == 0f ? .35f : Math.max(0f, 1f - Math.abs(cycle - 1.8f) / .55f);
            if (glow <= 0f) return;
            float x = width * .73f;
            path.rewind();
            path.moveTo(x + height * .15f, height * .11f);
            path.lineTo(x, height * .45f);
            path.lineTo(x + height * .14f, height * .45f);
            path.lineTo(x - height * .09f, height * .84f);
            paint.setColor(dark ? 0xFFD7CBFF : 0xFF553697);
            paint.setAlpha((int) (glow * 145));
            paint.setStrokeWidth(AndroidUtilities.dpf2(2f));
            canvas.drawPath(path, paint);
        }
    }
}
