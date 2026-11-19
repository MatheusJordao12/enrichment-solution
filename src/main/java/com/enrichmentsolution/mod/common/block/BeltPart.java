package com.enrichmentsolution.mod.common.block;

import net.minecraft.util.StringRepresentable;

/**
 * Segment shape of a {@link ConveyorBeltBlock} within a connected conveyor line.
 */
public enum BeltPart implements StringRepresentable {
    SINGLE("single"),
    START("start"),
    MIDDLE("middle"),
    END("end");

    private final String serializedName;

    BeltPart(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
