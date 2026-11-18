package com.enrichmentsolution.mod.client.config;

import net.minecraft.network.chat.Component;

/**
 * Available limits for how many solar panels animate per frame.
 */
public enum SolarAnimationLimit {
    OFF(0, "OFF", "Desativado"),
    ONE(1, "1", "1"),
    FIVE(5, "5", "5"),
    TEN(10, "10", "10"),
    FIFTEEN(15, "15", "15"),
    TWENTY(20, "20", "20"),
    TWENTY_FIVE(25, "25", "25"),
    FIFTY(50, "50", "50"),
    HUNDRED(100, "100", "100"),
    MAX(Integer.MAX_VALUE, "MAX", "MAX");

    private final int maxAnimated;
    private final String enLabel;
    private final String ptLabel;

    SolarAnimationLimit(int maxAnimated, String enLabel, String ptLabel) {
        this.maxAnimated = maxAnimated;
        this.enLabel = enLabel;
        this.ptLabel = ptLabel;
    }

    public int getMaxAnimated() {
        return maxAnimated;
    }

    public Component getDisplayName() {
        if (this == OFF) {
            return Component.translatable("option.enrichment_solution.solar_animation.off");
        }
        if (this == MAX) {
            return Component.translatable("option.enrichment_solution.solar_animation.max");
        }
        return Component.literal(enLabel);
    }
}
