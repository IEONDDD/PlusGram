package com.plusgram.app.appearance;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.Random;

import tw.nekomimi.nekogram.NekoConfig;

/** Weather drawn behind the chat list title and controls. */
public final class HeaderWeatherEffect {
    public static final int AURORA = 4;
    public static final int THUNDERSTORM = 5;
    public static final int RAIN = 6;

    private static final int DROPS_PER_LAYER = 18;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path lightning = new Path();
    private final Path lightningBranches = new Path();
    private final Random random = new Random();
    private long animationStarted;
    private int currentEffect;
    private int width;
    private int height;
    private boolean dark;
    private Shader auroraMist;
    private Shader cloudBank;
    private Shader cloudVeil;
    private Shader lightningGlow;
    private final Matrix glowPosition = new Matrix();
    private Bitmap aurora;
    private int[] auroraPixels;
    private float auroraTime = Float.NaN;
    private final Rect auroraBounds = new Rect();
    private Drop[] drops;
    private int boltCycle = -1;
    private float boltX;

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
        if (view.getWidth() <= 0 || bottom <= top) return;
        long now = SystemClock.uptimeMillis();
        if (currentEffect != effect) {
            currentEffect = effect;
            animationStarted = now;
        }
        boolean animate = MotionConfig.enabled();
        float seconds = animate ? ((now - animationStarted) / 33) * .033f : 0f;
        drawFrame(canvas, effect, view.getWidth(), top, bottom, dark, seconds, animate);
        if (animate && view.isShown() && view.getWindowVisibility() == View.VISIBLE) {
            view.postInvalidateDelayed(33);
        }
    }

    void drawFrame(Canvas canvas, int effect, int width, int top, int bottom, boolean dark,
                   float seconds, boolean animate) {
        preparePalette(width, bottom - top, dark);
        canvas.save();
        canvas.clipRect(0, top, width, bottom);
        canvas.translate(0, top);
        if (effect == AURORA) {
            drawAurora(canvas, seconds);
        } else {
            boolean storm = effect == THUNDERSTORM;
            drawClouds(canvas, storm);
            drawRain(canvas, seconds, storm);
            if (storm && animate) drawLightning(canvas, seconds);
        }
        canvas.restore();
    }

    private void preparePalette(int width, int height, boolean dark) {
        if (this.width == width && this.height == height && this.dark == dark) return;
        this.width = width;
        this.height = height;
        this.dark = dark;
        boltCycle = -1;
        auroraTime = Float.NaN;
        auroraBounds.set(0, 0, width, height);
        auroraMist = haze(width * .69f, height * .56f, width * .60f, height * .49f,
                dark ? 0x586B94CF : 0x376EA6BB);
        cloudBank = haze(width * .70f, height * .15f, width * .64f, height * .75f,
                dark ? 0xAA4F6290 : 0x807583A0);
        cloudVeil = haze(width * .42f, height * .42f, width * .55f, height * .48f,
                dark ? 0x894E7292 : 0x6A7896A9);
        lightningGlow = new RadialGradient(0, 0, 1f,
                dark ? 0x805F81F7 : 0x58617DEC, 0x00516ED6, Shader.TileMode.CLAMP);
    }

    private static Shader haze(float x, float y, float radiusX, float radiusY, int color) {
        int edge = color & 0x00FFFFFF;
        int middle = edge | ((color >>> 24) / 2 << 24);
        RadialGradient gradient = new RadialGradient(0, 0, 1f,
                new int[]{color, middle, edge},
                new float[]{0f, .52f, 1f}, Shader.TileMode.CLAMP);
        Matrix matrix = new Matrix();
        matrix.setScale(radiusX, radiusY);
        matrix.postTranslate(x, y);
        gradient.setLocalMatrix(matrix);
        return gradient;
    }

    private void drawAurora(Canvas canvas, float seconds) {
        wash(canvas, auroraMist, 148);
        if (auroraTime != seconds) {
            renderCurtains(seconds);
            auroraTime = seconds;
        }
        paint.setAlpha(255);
        canvas.drawBitmap(aurora, null, auroraBounds, paint);
    }

    private void renderCurtains(float seconds) {
        // A low-resolution light field interpolates into feathered curtains on the GPU.
        int columns = Math.max(2, Math.min(220, width / 4));
        int rows = Math.max(2, Math.min(46, height / 4));
        if (aurora == null || aurora.getWidth() != columns || aurora.getHeight() != rows) {
            aurora = Bitmap.createBitmap(columns, rows, Bitmap.Config.ARGB_8888);
            auroraPixels = new int[columns * rows];
        }
        float drift = seconds * .24f;
        int mint = dark ? 0x68E8B0 : 0x22936F;
        int violet = dark ? 0xAC91F2 : 0x7761BA;
        for (int column = 0; column < columns; column++) {
            float x = column / (float) (columns - 1);
            float edge = .58f + .15f * wave(x * 6.7f + drift)
                    + .055f * wave(x * 13.7f - drift * .87f);
            float rearEdge = .53f + .12f * wave(x * 8.2f - drift * .58f + 1.2f)
                    + .075f * wave(x * 15f + drift);
            float fold = wave(x * 69f + drift * 1.7f + wave(x * 12.3f) * 1.8f);
            float fibers = .68f + .20f * fold + .12f * wave(x * 139f - drift);
            float rise = .26f + .08f * wave(x * 22f - drift) + .08f * fold;
            float horizon = smoothstep(x * 4.2f) * smoothstep((1f - x) * 5.1f);
            for (int row = 0; row < rows; row++) {
                float y = row / (float) (rows - 1);
                float front = curtainLight(y - edge, rise);
                float rear = curtainLight(y - rearEdge, .23f);
                float attenuation = horizon * smoothstep(y * 8f) * smoothstep((1f - y) * 6f);
                int opacity = (int) (attenuation * (front * fibers * 156f + rear * 78f));
                float tint = Math.min(1f, .22f * x + Math.max(0f, edge - y) * 2.7f + rear * .18f);
                auroraPixels[row * columns + column] = opacity << 24 | blendLight(mint, violet, tint);
            }
        }
        aurora.setPixels(auroraPixels, 0, columns, 0, 0, columns, rows);
    }

    private static float wave(float angle) {
        return (float) Math.sin(angle);
    }

    private static float smoothstep(float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        return t * t * (3f - 2f * t);
    }

    private static float curtainLight(float offset, float rise) {
        float distance = offset / (offset < 0f ? rise : .065f);
        float fade = 1f / (1f + distance * distance * 3f);
        return fade * fade;
    }

    private static int blendLight(int a, int b, float p) {
        float q = 1f - p;
        int red = (int) ((a >> 16) * q + (b >> 16) * p);
        int green = (int) (((a >> 8) & 255) * q + ((b >> 8) & 255) * p);
        int blue = (int) ((a & 255) * q + (b & 255) * p);
        return red << 16 | green << 8 | blue;
    }

    private void drawClouds(Canvas canvas, boolean storm) {
        wash(canvas, cloudBank, storm ? 124 : 62);
        wash(canvas, cloudVeil, storm ? 83 : 45);
    }

    private void wash(Canvas canvas, Shader shader, int opacity) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(shader);
        paint.setAlpha(opacity);
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);
    }

    private void drawRain(Canvas canvas, float seconds, boolean storm) {
        if (drops == null) {
            random.setSeed(46);
            drops = new Drop[3 * DROPS_PER_LAYER];
            for (int i = 0; i < drops.length; i++) {
                drops[i] = new Drop(random, i / DROPS_PER_LAYER);
            }
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(dark ? 0xFFBCD8EB : 0xFF486D92);
        float wind = (storm ? .27f : .16f) + (float) Math.sin(seconds * 1.28f) * .05f;
        for (int i = 0; i < drops.length; i++) {
            if (!storm && i % 5 == 0) continue;
            Drop drop = drops[i];
            float length = height * drop.length;
            float progress = drop.phase + seconds * drop.speed * (storm ? 1.22f : 1f);
            int pass = (int) progress;
            float fraction = progress - pass;
            float y = fraction * (height + 2 * length) - length;
            float column = drop.column + pass * drop.step;
            float x = (column - (float) Math.floor(column)) * width - y * wind;
            float edgeFade = Math.min(1f, Math.min(fraction, 1f - fraction) * 6f);
            float contrast = .55f + .45f * Math.min(1f, x / width + .18f);
            int opacity = (int) (drop.opacity * edgeFade * contrast * (storm ? 1.15f : 1f));
            float tailX = x + length * wind;
            float tailY = y - length;
            float midX = (x + tailX) * .5f;
            float midY = (y + tailY) * .5f;
            paint.setStrokeWidth(height * drop.thickness);
            paint.setAlpha(opacity / 3);
            canvas.drawLine(tailX, tailY, midX, midY, paint);
            paint.setAlpha(opacity);
            canvas.drawLine(midX, midY, x, y, paint);
        }
    }

    private void drawLightning(Canvas canvas, float seconds) {
        int cycle = (int) (seconds / 11f);
        float age = seconds % 11f - (1.4f + cycle % 4 * .64f);
        if (age < 0f || age >= .46f) return;
        if (boltCycle != cycle) prepareBolt(cycle);
        float brightness = 1f - age / .46f;
        brightness *= brightness;
        glowPosition.setScale(height * 1.9f, height * .65f);
        glowPosition.postTranslate(boltX, height * .37f);
        lightningGlow.setLocalMatrix(glowPosition);
        wash(canvas, lightningGlow, (int) (brightness * 150));

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(dark ? 0xFFA9B8FF : 0xFF626BAA);
        paint.setStrokeWidth(height * .040f);
        paint.setAlpha((int) (brightness * 85));
        canvas.drawPath(lightning, paint);
        paint.setStrokeWidth(height * .012f);
        paint.setAlpha((int) (brightness * 160));
        canvas.drawPath(lightningBranches, paint);
        paint.setColor(dark ? 0xFFECEFFF : 0xFF555C94);
        paint.setStrokeWidth(height * .014f);
        paint.setAlpha((int) (brightness * 220));
        canvas.drawPath(lightning, paint);
    }

    private void prepareBolt(int cycle) {
        boltCycle = cycle;
        random.setSeed(93L + cycle);
        boltX = width * (.52f + .20f * random.nextFloat());
        float x = boltX;
        float y = height * .06f;
        lightning.rewind();
        lightningBranches.rewind();
        lightning.moveTo(x, y);
        for (int i = 0; i < 9; i++) {
            x += height * (.3f * random.nextFloat() - .16f);
            y += height * (.055f + .026f * random.nextFloat());
            lightning.lineTo(x, y);
            if (i == 2 || i == 4 || i == 6) {
                float direction = i == 4 ? -1f : 1f;
                lightningBranches.moveTo(x, y);
                lightningBranches.lineTo(x + height * .13f * direction, y + height * .08f);
                lightningBranches.lineTo(x + height * .29f * direction, y + height * .13f);
                lightningBranches.lineTo(x + height * .36f * direction, y + height * .22f);
            }
        }
    }

    private static final class Drop {
        final float column;
        final float phase;
        final float speed;
        final float length;
        final float thickness;
        final float step;
        final int opacity;

        Drop(Random random, int depth) {
            column = random.nextFloat();
            phase = random.nextFloat();
            speed = .70f + depth * .61f + random.nextFloat() * .38f;
            length = .059f + depth * .048f + random.nextFloat() * .056f;
            thickness = .0080f + depth * .0033f;
            step = .13f + random.nextFloat() * .75f;
            opacity = 27 + depth * 22 + random.nextInt(28);
        }
    }
}
