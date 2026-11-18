package com.enrichmentsolution.mod.common.block;

import net.minecraft.util.StringRepresentable;

/**
 * Defines capacity and transfer rate tiers for energy cables.
 */
public enum CableTier implements StringRepresentable {
    BASIC("basic", 4_000, 2_000),
    ADVANCED("advanced", 16_000, 8_000),
    ULTIMATE("ultimate", 64_000, 32_000),
    ULTRA("ultra", 256_000, 128_000);

    private final String serializedName;
    private final int capacity;
    private final int transferRate;

    CableTier(String serializedName, int capacity, int transferRate) {
        this.serializedName = serializedName;
        this.capacity = capacity;
        this.transferRate = transferRate;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getTransferRate() {
        return transferRate;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
