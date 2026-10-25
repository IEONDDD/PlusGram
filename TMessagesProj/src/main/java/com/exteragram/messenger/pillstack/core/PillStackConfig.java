package com.exteragram.messenger.pillstack.core;

import java.util.ArrayList;

public final class PillStackConfig {
    private PillStackConfig() {
    }

    public static ArrayList<Integer> getActivePills() {
        com.plusgram.app.pillstack.PillStackConfig.loadConfig(false);
        return com.plusgram.app.pillstack.PillStackConfig.getActivePills();
    }

    public static ArrayList<Integer> getHiddenPills() {
        com.plusgram.app.pillstack.PillStackConfig.loadConfig(false);
        return com.plusgram.app.pillstack.PillStackConfig.getHiddenPills();
    }

    public static int getLastActivePillId() {
        com.plusgram.app.pillstack.PillStackConfig.loadConfig(false);
        return com.plusgram.app.pillstack.PillStackConfig.lastActivePillId.Int();
    }
}
