package com.enrichmentsolution.mod.common.block;

import com.enrichmentsolution.mod.common.config.ModServerConfig;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Defines capacity and transfer rate tiers for energy cables.
 */
public enum CableTier implements StringRepresentable {
    BASIC("basic", ModServerConfig.CABLE_BASIC_CAPACITY, ModServerConfig.CABLE_BASIC_TRANSFER),
    ADVANCED("advanced", ModServerConfig.CABLE_ADVANCED_CAPACITY, ModServerConfig.CABLE_ADVANCED_TRANSFER),
    ULTIMATE("ultimate", ModServerConfig.CABLE_ULTIMATE_CAPACITY, ModServerConfig.CABLE_ULTIMATE_TRANSFER),
    ULTRA("ultra", ModServerConfig.CABLE_ULTRA_CAPACITY, ModServerConfig.CABLE_ULTRA_TRANSFER);

    private final String serializedName;
    private final ModConfigSpec.IntValue capacity;
    private final ModConfigSpec.IntValue transferRate;

    CableTier(String serializedName, ModConfigSpec.IntValue capacity, ModConfigSpec.IntValue transferRate) {
        this.serializedName = serializedName;
        this.capacity = capacity;
        this.transferRate = transferRate;
    }

    public int getCapacity() {
        return ModServerConfig.get(capacity);
    }

    public int getTransferRate() {
        return ModServerConfig.get(transferRate);
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
