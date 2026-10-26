package com.plusgram.app.appearance;

import com.google.android.material.color.utilities.Hct;

/**
 * Тональные палитры для собственных цветов Material You.
 *
 * Подставляются в MonetHelper вместо системных android.R.color.system_accent1..3
 * (акцент) и system_neutral1..2 (фон), когда в {@link MaterialYouConfig} выбран
 * свой акцент или режим фона. Каждая палитра генерируется HCT из seed-цвета
 * акцента; методы возвращают null, если соответствующий режим — «система»,
 * и MonetHelper продолжает работать как раньше.
 */
public final class CustomMonetPalette {

    private CustomMonetPalette() {}

    /**
     * Цвет по полному имени токена вида {@code a1_500} или {@code n2_100}.
     * Возвращает null, если токен не из перекрываемых палитр или режим — «система».
     */
    public static Integer resolveToken(String token) {
        if (token == null) {
            return null;
        }
        String palette = paletteOf(token);
        if (palette == null) {
            return null;
        }
        int tone = toneOf(token);
        if (tone < 0) {
            return null;
        }
        return resolveTone(palette, tone);
    }

    /**
     * Цвет палитры {@code palette} ({@code a1}/{@code a2}/{@code a3}/{@code n1}/{@code n2})
     * в тоне {@code tone} (0–100). Возвращает null для системного режима.
     */
    public static Integer resolveTone(String palette, int tone) {
        if (palette == null) {
            return null;
        }
        switch (palette) {
            case "a1":
                return accentActive() ? accent(tone, 1.0f, 0f) : null;
            case "a2":
                return accentActive() ? accent(tone, 0.5f, 0f) : null;
            case "a3":
                return accentActive() ? accent(tone, 0.8f, 60f) : null;
            case "n1":
            case "n2":
                return neutral(tone);
            default:
                return null;
        }
    }

    private static boolean accentActive() {
        return MaterialYouConfig.customAccent();
    }

    private static int accent(int tone, float chromaFactor, float hueShift) {
        Hct seed = Hct.fromInt(MaterialYouConfig.accentColor());
        double chroma = Math.max(8.0, seed.getChroma() * chromaFactor);
        double hue = (seed.getHue() + hueShift) % 360.0;
        if (hue < 0) {
            hue += 360.0;
        }
        return Hct.from(hue, chroma, tone).toInt();
    }

    private static Integer neutral(int tone) {
        int mode = MaterialYouConfig.backgroundMode();
        if (mode == MaterialYouConfig.BACKGROUND_SYSTEM) {
            return null;
        }
        Hct seed = Hct.fromInt(MaterialYouConfig.accentColor());
        double chroma = mode == MaterialYouConfig.BACKGROUND_GRAY ? 4.0 : 0.0;
        int effectiveTone = tone;
        if (mode == MaterialYouConfig.BACKGROUND_BLACK && tone <= 30) {
            effectiveTone = 0;
        }
        return Hct.from(seed.getHue(), chroma, effectiveTone).toInt();
    }

    private static String paletteOf(String token) {
        int underscore = token.indexOf('_');
        if (underscore <= 0 || underscore >= token.length() - 1) {
            return null;
        }
        String prefix = token.substring(0, underscore);
        if ("a1".equals(prefix) || "a2".equals(prefix) || "a3".equals(prefix)
                || "n1".equals(prefix) || "n2".equals(prefix)) {
            return prefix;
        }
        return null;
    }

    /** Обратная карта MonetHelper.toneToShade: shade → tone (0–100). */
    private static int toneOf(String token) {
        int underscore = token.lastIndexOf('_');
        if (underscore <= 0 || underscore >= token.length() - 1) {
            return -1;
        }
        int shade;
        try {
            shade = Integer.parseInt(token.substring(underscore + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
        switch (shade) {
            case 0:    return 100;
            case 10:   return 99;
            case 50:   return 95;
            case 100:  return 90;
            case 200:  return 80;
            case 300:  return 70;
            case 400:  return 60;
            case 500:  return 50;
            case 600:  return 40;
            case 700:  return 30;
            case 800:  return 20;
            case 900:  return 10;
            case 1000: return 0;
            default:   return -1;
        }
    }
}
