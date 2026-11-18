package com.enrichmentsolution.mod.common.block;

/**
 * I/O mode for each face of a configurable energy block.
 */
public enum SideConfigMode {
    INPUT("input", "IN", 0xFF2563EB),
    OUTPUT("output", "OUT", 0xFFEA580C),
    NONE("none", "OFF", 0xFF52525B);

    private final String key;
    private final String shortLabel;
    private final int color;

    SideConfigMode(String key, String shortLabel, int color) {
        this.key = key;
        this.shortLabel = shortLabel;
        this.color = color;
    }

    public boolean canReceive() {
        return this == INPUT;
    }

    public boolean canExtract() {
        return this == OUTPUT;
    }

    public SideConfigMode next() {
        SideConfigMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String getKey() {
        return key;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public int getColor() {
        return color;
    }

    public static SideConfigMode fromOrdinal(int ordinal) {
        SideConfigMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return INPUT;
        }
        return values[ordinal];
    }
}
