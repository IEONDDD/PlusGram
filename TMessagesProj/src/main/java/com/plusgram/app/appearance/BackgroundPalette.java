package com.plusgram.app.appearance;

import com.google.android.material.color.utilities.Hct;

import org.telegram.messenger.R;

enum BackgroundPalette {
    SYSTEM(R.string.PlusgramColorsBackgroundSystem, 0),
    GRAY(R.string.PlusgramColorsBackgroundGray, 0),
    BLACK(R.string.PlusgramColorsBackgroundBlack, 0),
    LAVENDER(R.string.PlusgramBackgroundLavender, 0xFF9164B2),
    OCEAN(R.string.PlusgramBackgroundOcean, 0xFF276C9A),
    FOREST(R.string.PlusgramBackgroundForest, 0xFF397756),
    SAND(R.string.PlusgramBackgroundSand, 0xFFA26A35),
    CUSTOM(R.string.PlusgramBackgroundCustom, 0);

    final int label;
    private final int seed;
    private static final BackgroundPalette[] OPTIONS = values();

    BackgroundPalette(int label, int seed) {
        this.label = label;
        this.seed = seed;
    }

    // The persisted order starts with the original System, Gray and Black modes.
    static BackgroundPalette selected() {
        int mode = MaterialYouConfig.backgroundMode();
        return mode >= 0 && mode < OPTIONS.length ? OPTIONS[mode] : SYSTEM;
    }

    Integer colorAtTone(int tone) {
        switch (this) {
            case SYSTEM: return null;
            case GRAY: return Hct.from(Hct.fromInt(MaterialYouConfig.accentColor()).getHue(), 4, tone).toInt();
            case BLACK: return Hct.from(0, 0, tone <= 30 ? 0 : tone).toInt();
            default:
                Hct color = Hct.fromInt(this == CUSTOM ? MaterialYouConfig.backgroundColor() : seed);
                return Hct.from(color.getHue(), Math.min(color.getChroma(), 25), tone).toInt();
        }
    }
}
