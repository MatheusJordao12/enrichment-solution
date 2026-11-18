package com.enrichmentsolution.mod.common.block;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Defines storage capacity and I/O transfer rate tiers for energy cells,
 * mirroring {@link CableTier} with significantly larger storage capacities.
 */
public enum EnergyCellTier implements StringRepresentable {
    BASIC("basic", 1_000_000, 2_000, 0xFF55FF55),
    ADVANCED("advanced", 4_000_000, 8_000, 0xFFFFAA00),
    ULTIMATE("ultimate", 16_000_000, 32_000, 0xFF00E5FF),
    ULTRA("ultra", 64_000_000, 128_000, 0xFFAA55FF);

    public static final Codec<EnergyCellTier> CODEC = StringRepresentable.fromEnum(EnergyCellTier::values);

    private final String serializedName;
    private final int capacity;
    private final int transferRate;
    private final int accentColor;

    EnergyCellTier(String serializedName, int capacity, int transferRate, int accentColor) {
        this.serializedName = serializedName;
        this.capacity = capacity;
        this.transferRate = transferRate;
        this.accentColor = accentColor;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getTransferRate() {
        return transferRate;
    }

    public int getAccentColor() {
        return accentColor;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
