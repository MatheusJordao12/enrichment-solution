package com.enrichmentsolution.mod.common.block;

import net.minecraft.util.StringRepresentable;

/**
 * Shared statistics for fluid conduits: internal buffer capacity, transfer rate (mB/t)
 * and how much heat survives each pipe block the fluid travels through.
 */
public interface FluidConduitTier extends StringRepresentable {

    /** Ambient temperature (Celsius) used as the baseline for heat loss. */
    double AMBIENT_TEMPERATURE = 20.0D;

    int getCapacity();

    int getTransferRate();

    /**
     * Fraction of the temperature above ambient kept after passing through one pipe block.
     * {@code 1.0} keeps 100% of the heat, {@code 0.96} loses 4% per block.
     */
    double getHeatRetention();

    /** False only for fully insulated tiers (cryogenic), which skip the cooling math entirely. */
    default boolean losesHeat() {
        return getHeatRetention() < 1.0D;
    }

    /** Resolves a tier from its serialized name, used by the block codec. */
    static FluidConduitTier bySerializedName(String name) {
        for (FluidPipeTier tier : FluidPipeTier.values()) {
            if (tier.getSerializedName().equals(name)) {
                return tier;
            }
        }
        for (ThermalPipeTier tier : ThermalPipeTier.values()) {
            if (tier.getSerializedName().equals(name)) {
                return tier;
            }
        }
        return FluidPipeTier.BASIC;
    }
}
