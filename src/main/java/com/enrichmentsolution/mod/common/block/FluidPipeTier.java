package com.enrichmentsolution.mod.common.block;

import com.enrichmentsolution.mod.common.config.ModServerConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Defines internal buffer capacity, transfer rate (mB/t) and heat retention for standard fluid pipes.
 * Heat retention is the fraction of the temperature above ambient kept per pipe block.
 */
public enum FluidPipeTier implements FluidConduitTier {
    BASIC("basic", ModServerConfig.FLUID_PIPE_BASIC_CAPACITY, ModServerConfig.FLUID_PIPE_BASIC_TRANSFER, ModServerConfig.FLUID_PIPE_BASIC_RETENTION),
    ADVANCED("advanced", ModServerConfig.FLUID_PIPE_ADVANCED_CAPACITY, ModServerConfig.FLUID_PIPE_ADVANCED_TRANSFER, ModServerConfig.FLUID_PIPE_ADVANCED_RETENTION),
    ULTIMATE("ultimate", ModServerConfig.FLUID_PIPE_ULTIMATE_CAPACITY, ModServerConfig.FLUID_PIPE_ULTIMATE_TRANSFER, ModServerConfig.FLUID_PIPE_ULTIMATE_RETENTION),
    ULTRA("ultra", ModServerConfig.FLUID_PIPE_ULTRA_CAPACITY, ModServerConfig.FLUID_PIPE_ULTRA_TRANSFER, ModServerConfig.FLUID_PIPE_ULTRA_RETENTION);

    private final String serializedName;
    private final ModConfigSpec.IntValue capacity;
    private final ModConfigSpec.IntValue transferRate;
    private final ModConfigSpec.DoubleValue heatRetention;

    FluidPipeTier(String serializedName, ModConfigSpec.IntValue capacity, ModConfigSpec.IntValue transferRate, ModConfigSpec.DoubleValue heatRetention) {
        this.serializedName = serializedName;
        this.capacity = capacity;
        this.transferRate = transferRate;
        this.heatRetention = heatRetention;
    }

    @Override
    public int getCapacity() {
        return ModServerConfig.get(capacity);
    }

    @Override
    public int getTransferRate() {
        return ModServerConfig.get(transferRate);
    }

    @Override
    public double getHeatRetention() {
        return ModServerConfig.get(heatRetention);
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
