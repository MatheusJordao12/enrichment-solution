package com.enrichmentsolution.mod.client.ctm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * Deterministic 47-tile Connected Texture (CTM) neighbor-mask lookup.
 * Maps the 8-neighbor connection bitmask of any block face to tile index 0..46
 * in an 8x6 (128x96) CTM atlas.
 */
public final class CtmLookup {

    public static final int ATLAS_COLS = 8;
    public static final int ATLAS_ROWS = 6;

    // Orthogonal bits
    private static final int ORTHO_UP = 1;
    private static final int ORTHO_RIGHT = 2;
    private static final int ORTHO_DOWN = 4;
    private static final int ORTHO_LEFT = 8;

    // Diagonal corner bits
    private static final int CORNER_UP_RIGHT = 1;
    private static final int CORNER_DOWN_RIGHT = 2;
    private static final int CORNER_DOWN_LEFT = 4;
    private static final int CORNER_UP_LEFT = 8;

    private static final int[][] LOOKUP = new int[16][16];

    static {
        int index = 0;
        for (int ortho = 0; ortho < 16; ortho++) {
            int validCorners = validCornerMask(ortho);
            for (int corner = 0; corner < 16; corner++) {
                if ((corner & ~validCorners) == 0) {
                    LOOKUP[ortho][corner] = index++;
                }
            }
        }
    }

    private CtmLookup() {
    }

    private static int validCornerMask(int ortho) {
        int valid = 0;
        if ((ortho & (ORTHO_UP | ORTHO_RIGHT)) == (ORTHO_UP | ORTHO_RIGHT)) {
            valid |= CORNER_UP_RIGHT;
        }
        if ((ortho & (ORTHO_DOWN | ORTHO_RIGHT)) == (ORTHO_DOWN | ORTHO_RIGHT)) {
            valid |= CORNER_DOWN_RIGHT;
        }
        if ((ortho & (ORTHO_DOWN | ORTHO_LEFT)) == (ORTHO_DOWN | ORTHO_LEFT)) {
            valid |= CORNER_DOWN_LEFT;
        }
        if ((ortho & (ORTHO_UP | ORTHO_LEFT)) == (ORTHO_UP | ORTHO_LEFT)) {
            valid |= CORNER_UP_LEFT;
        }
        return valid;
    }

    /**
     * Computes the 6 CTM tile indices (indexed by {@link Direction#get3DDataValue()})
     * for all 6 faces of the block at {@code pos}.
     */
    public static int[] computeFaceTiles(BlockAndTintGetter level, BlockPos pos, Predicate<BlockState> connectsTo) {
        int[] tiles = new int[6];
        for (Direction face : Direction.values()) {
            tiles[face.get3DDataValue()] = computeTileForFace(level, pos, face, connectsTo);
        }
        return tiles;
    }

    public static int computeTileForFace(BlockAndTintGetter level, BlockPos pos, Direction face,
                                         Predicate<BlockState> connectsTo) {
        Direction up = getUpDirection(face);
        Direction right = getRightDirection(face);
        Direction down = up.getOpposite();
        Direction left = right.getOpposite();

        int ortho = 0;
        if (isConnected(level, pos.relative(up), connectsTo)) ortho |= ORTHO_UP;
        if (isConnected(level, pos.relative(right), connectsTo)) ortho |= ORTHO_RIGHT;
        if (isConnected(level, pos.relative(down), connectsTo)) ortho |= ORTHO_DOWN;
        if (isConnected(level, pos.relative(left), connectsTo)) ortho |= ORTHO_LEFT;

        int corner = 0;
        if ((ortho & (ORTHO_UP | ORTHO_RIGHT)) == (ORTHO_UP | ORTHO_RIGHT)
                && isConnected(level, pos.relative(up).relative(right), connectsTo)) {
            corner |= CORNER_UP_RIGHT;
        }
        if ((ortho & (ORTHO_DOWN | ORTHO_RIGHT)) == (ORTHO_DOWN | ORTHO_RIGHT)
                && isConnected(level, pos.relative(down).relative(right), connectsTo)) {
            corner |= CORNER_DOWN_RIGHT;
        }
        if ((ortho & (ORTHO_DOWN | ORTHO_LEFT)) == (ORTHO_DOWN | ORTHO_LEFT)
                && isConnected(level, pos.relative(down).relative(left), connectsTo)) {
            corner |= CORNER_DOWN_LEFT;
        }
        if ((ortho & (ORTHO_UP | ORTHO_LEFT)) == (ORTHO_UP | ORTHO_LEFT)
                && isConnected(level, pos.relative(up).relative(left), connectsTo)) {
            corner |= CORNER_UP_LEFT;
        }

        return LOOKUP[ortho][corner];
    }

    private static boolean isConnected(BlockAndTintGetter level, BlockPos pos, Predicate<BlockState> connectsTo) {
        return connectsTo.test(level.getBlockState(pos));
    }

    /**
     * Returns the texture "up" direction in world space for the given face.
     */
    private static Direction getUpDirection(Direction face) {
        return switch (face) {
            case UP -> Direction.NORTH;
            case DOWN -> Direction.SOUTH;
            case NORTH, SOUTH, WEST, EAST -> Direction.UP;
        };
    }

    /**
     * Returns the texture "right" direction in world space when looking directly at {@code face}.
     */
    private static Direction getRightDirection(Direction face) {
        return switch (face) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            case EAST -> Direction.NORTH;
            case UP, DOWN -> Direction.EAST;
        };
    }
}
