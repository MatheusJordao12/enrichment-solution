package com.enrichmentsolution.mod.common.block;

import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Defines storage capacity and I/O transfer rate tiers for energy cells,
 * mirroring {@link CableTier} with significantly larger storage capacities.
 */
public enum EnergyCellTier implements StringRepresentable {
    BASIC("basic", ModServerConfig.ENERGY_CELL_BASIC_CAPACITY, ModServerConfig.ENERGY_CELL_BASIC_TRANSFER, 0xFF55FF55),
    ADVANCED("advanced", ModServerConfig.ENERGY_CELL_ADVANCED_CAPACITY, ModServerConfig.ENERGY_CELL_ADVANCED_TRANSFER, 0xFFFFAA00),
    ULTIMATE("ultimate", ModServerConfig.ENERGY_CELL_ULTIMATE_CAPACITY, ModServerConfig.ENERGY_CELL_ULTIMATE_TRANSFER, 0xFF00E5FF),
    ULTRA("ultra", ModServerConfig.ENERGY_CELL_ULTRA_CAPACITY, ModServerConfig.ENERGY_CELL_ULTRA_TRANSFER, 0xFFAA55FF);

    public static final Codec<EnergyCellTier> CODEC = StringRepresentable.fromEnum(EnergyCellTier::values);

    private final String serializedName;
    private final ModConfigSpec.IntValue capacity;
    private final ModConfigSpec.IntValue transferRate;
    private final int accentColor;

    EnergyCellTier(String serializedName, ModConfigSpec.IntValue capacity, ModConfigSpec.IntValue transferRate, int accentColor) {
        this.serializedName = serializedName;
        this.capacity = capacity;
        this.transferRate = transferRate;
        this.accentColor = accentColor;
    }

    public int getCapacity() {
        return ModServerConfig.get(capacity);
    }

    public int getTransferRate() {
        return ModServerConfig.get(transferRate);
    }

    public int getAccentColor() {
        return accentColor;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
