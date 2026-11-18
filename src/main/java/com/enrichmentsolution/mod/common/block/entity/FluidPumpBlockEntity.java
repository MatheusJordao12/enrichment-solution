package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.FluidPumpBlock;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.menu.FluidPumpMenu;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Fluid Pump block entity that extracts Water and finite Crude Oil from the world.
 * Scans downward up to 64 blocks (with an 8-block horizontal pool scan) and traces
 * flowing Crude Oil upstream to its source block so oil deposits are finite.
 */
public class FluidPumpBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    public static final int TANK_CAPACITY = 16_000;
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int ENERGY_PER_PUMP = 200;
    public static final int PUMP_INTERVAL = 10;

    private static final int MAX_SCAN_DEPTH = 64;
    private static final int HORIZONTAL_RADIUS = 8;
    private static final int MAX_TRACE_NODES = 512;
    private static final int AUTO_PUSH_RATE = 500;

    private int progress = 0;
    private boolean isWorking = false;

    private final SideConfigHolder sideConfig = SideConfigHolder.createDefault(
            SideConfigMode.NONE,
            SideConfigMode.INPUT,
            SideConfigMode.INPUT,
            SideConfigMode.OUTPUT,
            SideConfigMode.OUTPUT,
            SideConfigMode.NONE
    );

    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY,
            stack -> stack.getFluid() == Fluids.WATER || stack.getFluid() == ModFluids.CRUDE_OIL.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? outputTank.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? outputTank.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return outputTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return outputTank.drain(maxDrain, action);
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int energy = energyStorage.getEnergyStored();
            int maxEnergy = energyStorage.getMaxEnergyStored();
            int outFluid = outputTank.getFluidAmount();
            int outCap = outputTank.getCapacity();
            int fluidType = getSyncedFluidType();
            return switch (index) {
                case FluidPumpMenu.DATA_PROGRESS -> progress;
                case FluidPumpMenu.DATA_MAX_PROGRESS -> PUMP_INTERVAL;
                case FluidPumpMenu.DATA_ENERGY_LOW -> energy & 0xFFFF;
                case FluidPumpMenu.DATA_ENERGY_HIGH -> (energy >>> 16) & 0xFFFF;
                case FluidPumpMenu.DATA_MAX_ENERGY_LOW -> maxEnergy & 0xFFFF;
                case FluidPumpMenu.DATA_MAX_ENERGY_HIGH -> (maxEnergy >>> 16) & 0xFFFF;
                case FluidPumpMenu.DATA_OUTPUT_FLUID_LOW -> outFluid & 0xFFFF;
                case FluidPumpMenu.DATA_OUTPUT_FLUID_HIGH -> (outFluid >>> 16) & 0xFFFF;
                case FluidPumpMenu.DATA_OUTPUT_CAP_LOW -> outCap & 0xFFFF;
                case FluidPumpMenu.DATA_OUTPUT_CAP_HIGH -> (outCap >>> 16) & 0xFFFF;
                case FluidPumpMenu.DATA_FLUID_TYPE -> fluidType;
                default -> {
                    if (index >= FluidPumpMenu.DATA_SIDE_BASE
                            && index < FluidPumpMenu.DATA_SIDE_BASE + RelativeSide.values().length) {
                        yield sideConfig.getMode(RelativeSide.fromOrdinal(index - FluidPumpMenu.DATA_SIDE_BASE)).ordinal();
                    }
                    yield 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == FluidPumpMenu.DATA_PROGRESS) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return FluidPumpMenu.DATA_COUNT;
        }
    };

    public FluidPumpBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.FLUID_PUMP.get(), pos, blockState, ENERGY_CAPACITY, MAX_RECEIVE, 0, 0);
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(FluidPumpBlock.FACING) ? state.getValue(FluidPumpBlock.FACING) : Direction.NORTH;
    }

    public SideConfigMode getRelativeSideMode(RelativeSide side) {
        return sideConfig.getMode(side);
    }

    public void cycleRelativeSideMode(int ordinal) {
        sideConfig.cycleMode(RelativeSide.fromOrdinal(ordinal));
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    @Nullable
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return sideConfig.wrapEnergyStorage(energyStorage, side, getFacing());
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return sideConfig.wrapFluidHandler(fluidHandler, side, getFacing());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enrichment_solution.fluid_pump");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidPumpMenu(containerId, playerInventory, this, data);
    }

    private int getSyncedFluidType() {
        if (outputTank.isEmpty()) {
            return FluidPumpMenu.FLUID_NONE;
        }
        Fluid fluid = outputTank.getFluid().getFluid();
        if (fluid == Fluids.WATER) {
            return FluidPumpMenu.FLUID_WATER;
        }
        if (fluid == ModFluids.CRUDE_OIL.get()) {
            return FluidPumpMenu.FLUID_CRUDE_OIL;
        }
        return FluidPumpMenu.FLUID_NONE;
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        if (outputTank.getSpace() >= FluidType.BUCKET_VOLUME
                && energyStorage.getEnergyStored() >= ENERGY_PER_PUMP) {
            progress++;
            if (progress >= PUMP_INTERVAL) {
                progress = 0;
                if (tryPump(level, pos)) {
                    energyStorage.consumeInternal(ENERGY_PER_PUMP, false);
                    isWorking = true;
                } else {
                    isWorking = false;
                }
            }
            setChanged();
        } else {
            if (progress != 0 || isWorking) {
                progress = 0;
                isWorking = false;
                setChanged();
            }
        }

        if (state.getValue(FluidPumpBlock.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(FluidPumpBlock.LIT, isWorking), Block.UPDATE_ALL);
        }

        pushOutputFluid(level, pos);
    }

    /**
     * Scans downward from below the pump through open air or pumpable fluid, stopping
     * immediately at any solid block so it never pulls fluid through stone/ground.
     */
    private boolean tryPump(Level level, BlockPos pos) {
        int minY = level.getMinBuildHeight();

        for (int depth = 1; depth <= MAX_SCAN_DEPTH; depth++) {
            BlockPos checkPos = pos.below(depth);
            if (checkPos.getY() < minY) {
                break;
            }
            BlockState blockState = level.getBlockState(checkPos);
            FluidState fluidState = blockState.getFluidState();

            if (isPumpable(fluidState)) {
                BlockPos poolTarget = findConnectedPoolTarget(level, checkPos, fluidState);
                if (poolTarget != null && pumpAt(level, poolTarget)) {
                    return true;
                }
                if (pumpAt(level, checkPos)) {
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
    private BlockPos findConnectedPoolTarget(Level level, BlockPos startPos, FluidState startFluid) {
        boolean oil = isCrudeOil(startFluid);
        if (oil && !canAcceptFluid(ModFluids.CRUDE_OIL.get())) {
            return null;
        }
        if (!oil && !canAcceptFluid(Fluids.WATER)) {
            return null;
        }

        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.HashSet<BlockPos> visited = new java.util.HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        BlockPos best = null;
        int bestDistSq = -1;
        int maxDistSq = HORIZONTAL_RADIUS * HORIZONTAL_RADIUS;

        while (!queue.isEmpty() && visited.size() < MAX_TRACE_NODES) {
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

    private boolean isPumpable(FluidState fluidState) {
        if (isCrudeOil(fluidState)) {
            return canAcceptFluid(ModFluids.CRUDE_OIL.get());
        }
        if (isWaterSource(fluidState)) {
            return canAcceptFluid(Fluids.WATER);
        }
        return false;
    }

    private boolean canAcceptFluid(Fluid fluid) {
        return outputTank.fill(new FluidStack(fluid, FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.SIMULATE)
                == FluidType.BUCKET_VOLUME;
    }

    private boolean pumpAt(Level level, BlockPos targetPos) {
        FluidState fluidState = level.getFluidState(targetPos);

        if (isCrudeOil(fluidState)) {
            if (!canAcceptFluid(ModFluids.CRUDE_OIL.get())) {
                return false;
            }
            if (fluidState.isSource()) {
                level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                outputTank.fill(new FluidStack(ModFluids.CRUDE_OIL.get(), FluidType.BUCKET_VOLUME),
                        IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
            // Flowing Crude Oil: trace upstream to find and remove the source block.
            BlockPos sourcePos = traceCrudeOilSource(level, targetPos);
            if (sourcePos != null) {
                level.setBlock(sourcePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                outputTank.fill(new FluidStack(ModFluids.CRUDE_OIL.get(), FluidType.BUCKET_VOLUME),
                        IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
            // Stray flowing oil with no remaining source block: clear it without yielding fluid.
            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return false;
        }

        if (isWaterSource(fluidState)) {
            if (!canAcceptFluid(Fluids.WATER)) {
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
    private BlockPos traceCrudeOilSource(Level level, BlockPos startPos) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && visited.size() < MAX_TRACE_NODES) {
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

    private void pushOutputFluid(Level level, BlockPos pos) {
        if (outputTank.isEmpty()) {
            return;
        }
        Direction facing = getFacing();
        for (Direction dir : Direction.values()) {
            if (outputTank.isEmpty()) {
                break;
            }
            if (!sideConfig.canPushTo(dir, facing)) {
                continue;
            }
            BlockPos neighborPos = pos.relative(dir);
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
            if (target == null) {
                continue;
            }
            FluidStack toSend = outputTank.drain(AUTO_PUSH_RATE, IFluidHandler.FluidAction.SIMULATE);
            if (!toSend.isEmpty()) {
                int accepted = target.fill(toSend, IFluidHandler.FluidAction.EXECUTE);
                if (accepted > 0) {
                    outputTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
        tag.putBoolean("IsWorking", isWorking);
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        sideConfig.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Progress")) {
            progress = tag.getInt("Progress");
        }
        if (tag.contains("IsWorking")) {
            isWorking = tag.getBoolean("IsWorking");
        }
        if (tag.contains("OutputTank")) {
            outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
        sideConfig.loadAdditional(tag);
    }
}
