package com.enrichmentsolution.mod.common.block;

/**
 * Defines internal buffer capacity, transfer rate (mB/t) and heat retention for standard fluid pipes.
 * Heat retention is the fraction of the temperature above ambient kept per pipe block.
 */
public enum FluidPipeTier implements FluidConduitTier {
    BASIC("basic", 1_000, 200, 0.9600D),
    ADVANCED("advanced", 4_000, 1_000, 0.9700D),
    ULTIMATE("ultimate", 16_000, 4_000, 0.9800D),
    ULTRA("ultra", 64_000, 16_000, 0.9850D);

    private final String serializedName;
    private final int capacity;
    private final int transferRate;
    private final double heatRetention;

    FluidPipeTier(String serializedName, int capacity, int transferRate, double heatRetention) {
        this.serializedName = serializedName;
        this.capacity = capacity;
        this.transferRate = transferRate;
        this.heatRetention = heatRetention;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public int getTransferRate() {
        return transferRate;
    }

    @Override
    public double getHeatRetention() {
        return heatRetention;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
