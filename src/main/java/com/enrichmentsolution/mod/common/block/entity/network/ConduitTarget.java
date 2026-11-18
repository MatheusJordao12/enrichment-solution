package com.enrichmentsolution.mod.common.block.entity.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A non-conduit block entity directly adjacent to a conduit network that the network may hand
 * something to. Whether it actually exposes a fluid or energy handler at that side is resolved on
 * every delivery, so machine side configuration changes work without rebuilding the cache.
 *
 * @param pos        absolute position of the candidate receiver
 * @param accessSide side of the receiver that faces the conduit, i.e. the side that must be
 *                   queried on the block capability
 */
public record ConduitTarget(BlockPos pos, Direction accessSide) {
}
