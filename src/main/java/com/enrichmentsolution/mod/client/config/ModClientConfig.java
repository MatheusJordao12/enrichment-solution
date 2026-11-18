package com.enrichmentsolution.mod.client.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side configuration for Enrichment Solution visual and animation settings.
 */
public final class ModClientConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<SolarAnimationLimit> SOLAR_ANIMATION_LIMIT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("animation_solution");
        SOLAR_ANIMATION_LIMIT = builder
                .comment("Maximum number of solar panels that animate simultaneously (OFF, 1, 5, 10, 15, 20, 25, 50, 100, MAX).")
                .defineEnum("solarAnimationLimit", SolarAnimationLimit.MAX);
        builder.pop();

        SPEC = builder.build();
    }

    public static SolarAnimationLimit getLimit() {
        return SOLAR_ANIMATION_LIMIT.get();
    }

    public static void setLimit(SolarAnimationLimit limit) {
        SOLAR_ANIMATION_LIMIT.set(limit);
        SPEC.save();
    }

    private ModClientConfig() {}
}
