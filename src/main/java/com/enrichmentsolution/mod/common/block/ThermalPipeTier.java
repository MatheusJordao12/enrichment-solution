package com.enrichmentsolution.mod.common.block;

import com.enrichmentsolution.mod.common.config.ModServerConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Insulated fluid pipe tiers. Same transfer mechanics as {@link FluidPipeTier},
 * but with progressively better heat retention (the last one loses nothing at all).
 */
public enum ThermalPipeTier implements FluidConduitTier {
    ISOLATED("isolated", ModServerConfig.THERMAL_PIPE_ISOLATED_CAPACITY, ModServerConfig.THERMAL_PIPE_ISOLATED_TRANSFER, ModServerConfig.THERMAL_PIPE_ISOLATED_RETENTION),
    REINFORCED("reinforced", ModServerConfig.THERMAL_PIPE_REINFORCED_CAPACITY, ModServerConfig.THERMAL_PIPE_REINFORCED_TRANSFER, ModServerConfig.THERMAL_PIPE_REINFORCED_RETENTION),
    VACUUM("vacuum", ModServerConfig.THERMAL_PIPE_VACUUM_CAPACITY, ModServerConfig.THERMAL_PIPE_VACUUM_TRANSFER, ModServerConfig.THERMAL_PIPE_VACUUM_RETENTION),
    CRYOGENIC("cryogenic", ModServerConfig.THERMAL_PIPE_CRYOGENIC_CAPACITY, ModServerConfig.THERMAL_PIPE_CRYOGENIC_TRANSFER, ModServerConfig.THERMAL_PIPE_CRYOGENIC_RETENTION);

    private final String serializedName;
    private final ModConfigSpec.IntValue capacity;
    private final ModConfigSpec.IntValue transferRate;
    private final ModConfigSpec.DoubleValue heatRetention;

    ThermalPipeTier(String serializedName, ModConfigSpec.IntValue capacity, ModConfigSpec.IntValue transferRate, ModConfigSpec.DoubleValue heatRetention) {
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
