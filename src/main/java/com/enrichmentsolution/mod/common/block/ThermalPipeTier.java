package com.enrichmentsolution.mod.common.block;

/**
 * Insulated fluid pipe tiers. Same transfer mechanics as {@link FluidPipeTier},
 * but with progressively better heat retention (the last one loses nothing at all).
 */
public enum ThermalPipeTier implements FluidConduitTier {
    ISOLATED("isolated", 2_000, 500, 0.9950D),
    REINFORCED("reinforced", 8_000, 2_000, 0.9975D),
    VACUUM("vacuum", 32_000, 8_000, 0.9990D),
    CRYOGENIC("cryogenic", 128_000, 32_000, 1.0D);

    private final String serializedName;
    private final int capacity;
    private final int transferRate;
    private final double heatRetention;

    ThermalPipeTier(String serializedName, int capacity, int transferRate, double heatRetention) {
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
