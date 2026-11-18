package com.enrichmentsolution.mod.common.block;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * Block-relative face orientation for Mekanism-style side configuration.
 */
public enum RelativeSide {
    FRONT("front", "F", "Front"),
    BACK("back", "B", "Back"),
    LEFT("left", "L", "Left"),
    RIGHT("right", "R", "Right"),
    TOP("top", "T", "Top"),
    BOTTOM("bottom", "D", "Bottom");

    private final String key;
    private final String shortLabel;
    private final String displayName;

    RelativeSide(String key, String shortLabel, String displayName) {
        this.key = key;
        this.shortLabel = shortLabel;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Maps this relative side to a world {@link Direction} given the block's horizontal facing.
     */
    public Direction toWorldDirection(@Nullable Direction facing) {
        Direction horizontalFacing = normalizeFacing(facing);
        return switch (this) {
            case FRONT -> horizontalFacing;
            case BACK -> horizontalFacing.getOpposite();
            case LEFT -> horizontalFacing.getCounterClockWise();
            case RIGHT -> horizontalFacing.getClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }

    /**
     * Maps a world {@link Direction} to its {@link RelativeSide} given the block's horizontal facing.
     */
    public static RelativeSide fromWorldDirection(Direction worldSide, @Nullable Direction facing) {
        if (worldSide == Direction.UP) {
            return TOP;
        }
        if (worldSide == Direction.DOWN) {
            return BOTTOM;
        }
        Direction horizontalFacing = normalizeFacing(facing);
        if (worldSide == horizontalFacing) {
            return FRONT;
        }
        if (worldSide == horizontalFacing.getOpposite()) {
            return BACK;
        }
        if (worldSide == horizontalFacing.getCounterClockWise()) {
            return LEFT;
        }
        if (worldSide == horizontalFacing.getClockWise()) {
            return RIGHT;
        }
        return FRONT;
    }

    public static RelativeSide fromOrdinal(int ordinal) {
        RelativeSide[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return FRONT;
        }
        return values[ordinal];
    }

    private static Direction normalizeFacing(@Nullable Direction facing) {
        if (facing == null || facing.getAxis().isVertical()) {
            return Direction.NORTH;
        }
        return facing;
    }
}
