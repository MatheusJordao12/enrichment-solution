package com.enrichmentsolution.mod.common.block.entity.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A fluid handler candidate reachable from a specific pipe, together with how much of the fluid's
 * temperature above ambient survives the trip. The handler itself is resolved on delivery.
 *
 * @param pos           absolute position of the candidate receiver
 * @param accessSide    side of the receiver that faces the conduit
 * @param heatRetention product of the heat retention of every pipe on the path, including the
 *                      pipe that pushes the fluid
 */
public record FluidDeliveryTarget(BlockPos pos, Direction accessSide, double heatRetention) {
}
