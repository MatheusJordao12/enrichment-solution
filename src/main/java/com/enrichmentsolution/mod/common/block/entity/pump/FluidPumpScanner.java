package com.enrichmentsolution.mod.common.block.entity.pump;

import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.enrichmentsolution.mod.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles downward fluid scanning, horizontal pool edge BFS, and upstream Crude Oil
 * source tracing for {@link com.enrichmentsolution.mod.common.block.entity.FluidPumpBlockEntity}.
 */
public final class FluidPumpScanner {

    private FluidPumpScanner() {
    }

    /**
     * Scans downward from below the pump through open air or pumpable fluid, stopping
     * immediately at any solid block so it never pulls fluid through stone/ground.
     */
    public static boolean tryPump(Level level, BlockPos pos, FluidTank outputTank) {
        int minY = level.getMinBuildHeight();
        int maxScanDepth = ModServerConfig.get(ModServerConfig.FLUID_PUMP_MAX_SCAN_DEPTH);

        for (int depth = 1; depth <= maxScanDepth; depth++) {
            BlockPos checkPos = pos.below(depth);
            if (checkPos.getY() < minY) {
                break;
            }
            BlockState blockState = level.getBlockState(checkPos);
            FluidState fluidState = blockState.getFluidState();

            if (isPumpable(fluidState, outputTank)) {
                BlockPos poolTarget = findConnectedPoolTarget(level, checkPos, fluidState, outputTank);
                if (poolTarget != null && pumpAt(level, poolTarget, outputTank)) {
                    return true;
                }
                if (pumpAt(level, checkPos, outputTank)) {
                    return true;
                }
                // Stop at the first fluid layer if it could not be pumped (e.g. tank holds another fluid).
                break;
            }

            // Stop immediately if we hit a solid block (do not scan through stone/ground).
            if (!blockState.isAir()) {
                break;
            }
        }

        return false;
    }

    /**
     * BFS through connected fluid blocks at or below the hit layer so the pump drains
     * the outer edges of the connected pool first without reaching through solid walls.
     */
    @Nullable
    private static BlockPos findConnectedPoolTarget(
            Level level,
            BlockPos startPos,
            FluidState startFluid,
            FluidTank outputTank
    ) {
        boolean oil = isCrudeOil(startFluid);
        if (oil && !canAcceptFluid(ModFluids.CRUDE_OIL.get(), outputTank)) {
            return null;
        }
        if (!oil && !canAcceptFluid(Fluids.WATER, outputTank)) {
            return null;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        HashSet<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        BlockPos best = null;
        int bestDistSq = -1;
        int horizontalRadius = ModServerConfig.get(ModServerConfig.FLUID_PUMP_HORIZONTAL_RADIUS);
        int maxDistSq = horizontalRadius * horizontalRadius;
        int maxTraceNodes = ModServerConfig.get(ModServerConfig.FLUID_PUMP_MAX_TRACE_NODES);

        while (!queue.isEmpty() && visited.size() < maxTraceNodes) {
            BlockPos current = queue.poll();
            int dx = current.getX() - startPos.getX();
            int dz = current.getZ() - startPos.getZ();
            int distSq = dx * dx + dz * dz;

            if (distSq > bestDistSq) {
                best = current;
                bestDistSq = distSq;
            }

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(dir);
                if (!visited.add(next)) {
                    continue;
                }
                int ndx = next.getX() - startPos.getX();
                int ndz = next.getZ() - startPos.getZ();
                if (ndx * ndx + ndz * ndz > maxDistSq) {
                    continue;
                }
                FluidState nextFluid = level.getFluidState(next);
                boolean matches = oil ? isCrudeOil(nextFluid) : isWaterSource(nextFluid);
                if (matches) {
                    queue.add(next);
                }
            }
        }
        return best;
    }

    private static boolean isPumpable(FluidState fluidState, FluidTank outputTank) {
        if (isCrudeOil(fluidState)) {
            return canAcceptFluid(ModFluids.CRUDE_OIL.get(), outputTank);
        }
        if (isWaterSource(fluidState)) {
            return canAcceptFluid(Fluids.WATER, outputTank);
        }
        return false;
    }

    private static boolean canAcceptFluid(Fluid fluid, FluidTank outputTank) {
        return outputTank.fill(new FluidStack(fluid, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.SIMULATE)
                == FluidType.BUCKET_VOLUME;
    }

    private static boolean pumpAt(Level level, BlockPos targetPos, FluidTank outputTank) {
        FluidState fluidState = level.getFluidState(targetPos);

        if (isCrudeOil(fluidState)) {
            if (!canAcceptFluid(ModFluids.CRUDE_OIL.get(), outputTank)) {
                return false;
            }
            if (fluidState.isSource()) {
                level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                outputTank.fill(new FluidStack(ModFluids.CRUDE_OIL.get(), FluidType.BUCKET_VOLUME),
                        IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
            BlockPos sourcePos = traceCrudeOilSource(level, targetPos);
            if (sourcePos != null) {
                level.setBlock(sourcePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                outputTank.fill(new FluidStack(ModFluids.CRUDE_OIL.get(), FluidType.BUCKET_VOLUME),
                        IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return false;
        }

        if (isWaterSource(fluidState)) {
            if (!canAcceptFluid(Fluids.WATER, outputTank)) {
                return false;
            }
            int adjacentSources = 0;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                FluidState adj = level.getFluidState(targetPos.relative(dir));
                if (isWaterSource(adj)) {
                    adjacentSources++;
                }
            }
            outputTank.fill(new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME),
                    IFluidHandler.FluidAction.EXECUTE);
            // Preserve infinite water pools (2+ adjacent horizontal water sources).
            if (adjacentSources < 2 && level.getBlockState(targetPos).getBlock() instanceof LiquidBlock) {
                level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            return true;
        }

        return false;
    }

    /**
     * Walks upstream from a flowing Crude Oil block (climbing up and toward higher fluid levels)
     * to locate the originating Crude Oil source block.
     */
    @Nullable
    private static BlockPos traceCrudeOilSource(Level level, BlockPos startPos) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        int maxTraceNodes = ModServerConfig.get(ModServerConfig.FLUID_PUMP_MAX_TRACE_NODES);
        while (!queue.isEmpty() && visited.size() < maxTraceNodes) {
            BlockPos current = queue.poll();
            FluidState currentState = level.getFluidState(current);
            int currentAmount = currentState.getAmount();

            BlockPos above = current.above();
            if (visited.add(above)) {
                FluidState aboveState = level.getFluidState(above);
                if (isCrudeOil(aboveState)) {
                    if (aboveState.isSource()) {
                        return above;
                    }
                    queue.add(above);
                }
            }

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = current.relative(dir);
                if (!visited.add(neighbor)) {
                    continue;
                }
                FluidState neighborState = level.getFluidState(neighbor);
                if (!isCrudeOil(neighborState)) {
                    continue;
                }
                if (neighborState.isSource()) {
                    return neighbor;
                }
                if (neighborState.getAmount() >= currentAmount) {
                    queue.add(neighbor);
                }
            }
        }

        return null;
    }

    private static boolean isCrudeOil(FluidState state) {
        Fluid type = state.getType();
        return type == ModFluids.CRUDE_OIL.get() || type == ModFluids.FLOWING_CRUDE_OIL.get();
    }

    private static boolean isWaterSource(FluidState state) {
        return state.getType() == Fluids.WATER && state.isSource();
    }
}
