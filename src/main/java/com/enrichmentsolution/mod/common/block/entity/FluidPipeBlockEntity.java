package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.FluidConduitTier;
import com.enrichmentsolution.mod.common.block.FluidPipeBlock;
import com.enrichmentsolution.mod.common.block.FluidPipeTier;
import com.enrichmentsolution.mod.common.block.entity.network.ConduitLink;
import com.enrichmentsolution.mod.common.block.entity.network.ConduitNetworkCache;
import com.enrichmentsolution.mod.common.block.entity.network.FluidDeliveryTarget;
import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.enrichmentsolution.mod.common.fluid.ThermalFluidHelper;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Pulls and distributes fluids across connected FluidPipeBlock networks according to its FluidConduitTier.
 * Fluid sits in the pulling pipe and is delivered through the network, losing heat per pipe block on the way.
 */
public class FluidPipeBlockEntity extends BlockEntity implements com.enrichmentsolution.mod.common.block.IConduitConfigurable {

    /** Pre-computed routes of every pipe of every loaded level, shared by all pipe block entities. */
    private static final ConduitNetworkCache<FluidDeliveryTarget> NETWORK_CACHE = new ConduitNetworkCache<>();

    private final com.enrichmentsolution.mod.common.block.ConduitSideConfig conduitConfig = new com.enrichmentsolution.mod.common.block.ConduitSideConfig();

    @Override
    public com.enrichmentsolution.mod.common.block.ConduitSideConfig getConduitConfig() {
        return conduitConfig;
    }

    @Override
    public void onConduitConfigChanged() {
        if (level != null) {
            invalidateNetwork(level);
        }
        setChanged();
    }

    /**
     * Marks the cached pipe routes of {@code level} stale. Must be called whenever the network
     * shape changes: a pipe or machine is placed/removed next to a pipe, or a pipe's side
     * configuration changes.
     */
    public static void invalidateNetwork(Level level) {
        NETWORK_CACHE.invalidate(level);
    }

    private final int transferRate;
    private final double heatRetention;
    private final FluidTank fluidTank;
    private Fluid lockedFluid = Fluids.EMPTY;
    private int emptyTicks = 0;
    @Nullable
    private BlockPos lastSourcePos;

    public FluidPipeBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, resolveTier(blockState));
    }

    private FluidPipeBlockEntity(BlockPos pos, BlockState blockState, FluidConduitTier tier) {
        super(ModBlockEntities.FLUID_PIPE.get(), pos, blockState);
        this.transferRate = tier.getTransferRate();
        this.heatRetention = tier.getHeatRetention();
        this.fluidTank = new ThermalPipeFluidTank(tier.getCapacity());
    }

    private class ThermalPipeFluidTank extends FluidTank {

        private ThermalPipeFluidTank(int capacity) {
            super(capacity);
        }

        @Override
        protected void onContentsChanged() {
            if (!isEmpty()) {
                lockedFluid = fluid.getFluid();
                emptyTicks = 0;
            }
            setChanged();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            if (lockedFluid != Fluids.EMPTY && !stack.isEmpty() && !stack.getFluid().isSame(lockedFluid)) {
                return false;
            }
            return super.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()
                    || (lockedFluid != Fluids.EMPTY && !resource.getFluid().isSame(lockedFluid))
                    || !isFluidValid(resource)) {
                return 0;
            }
            if (fluid.isEmpty()) {
                int filled = Math.min(capacity, resource.getAmount());
                if (action.execute() && filled > 0) {
                    fluid = ThermalFluidHelper.withThermalData(
                            resource.copyWithAmount(filled),
                            ThermalFluidHelper.getTemperature(resource),
                            ThermalFluidHelper.getMultiplier(resource));
                    onContentsChanged();
                }
                return filled;
            }
            if (fluid.getFluid() == Fluids.WATER && resource.getFluid() == Fluids.WATER) {
                int filled = Math.min(capacity - fluid.getAmount(), resource.getAmount());
                if (action.execute() && filled > 0) {
                    fluid = ThermalFluidHelper.mergeThermalStacks(fluid, resource, filled);
                    onContentsChanged();
                }
                return filled;
            }
            return super.fill(resource, action);
        }
    }

    private static FluidConduitTier resolveTier(BlockState state) {
        return state.getBlock() instanceof FluidPipeBlock pipe ? pipe.getTier() : FluidPipeTier.BASIC;
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return fluidTank;
    }

    /** Server-side accessor used by the goggles inspector overlay. */
    public FluidStack getStoredFluid() {
        return fluidTank.getFluid();
    }

    public int getTankCapacity() {
        return fluidTank.getCapacity();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        blockEntity.pullFluidFromAdjacentSources(level, pos);
        if (!blockEntity.fluidTank.isEmpty()) {
            blockEntity.distributeFluidAlongNetwork(level, pos);
        }
        if (blockEntity.fluidTank.isEmpty()) {
            if (blockEntity.lockedFluid != Fluids.EMPTY) {
                blockEntity.emptyTicks++;
                if (blockEntity.emptyTicks >= ModServerConfig.get(ModServerConfig.FLUID_PIPE_EMPTY_UNLOCK_TICKS)) {
                    blockEntity.lockedFluid = Fluids.EMPTY;
                    blockEntity.emptyTicks = 0;
                    blockEntity.setChanged();
                }
            }
        } else {
            blockEntity.lockedFluid = blockEntity.fluidTank.getFluid().getFluid();
            blockEntity.emptyTicks = 0;
        }
    }

    /**
     * Actively draws fluid from adjacent non-pipe IFluidHandler providers up to this tier's transfer rate.
     */
    private void pullFluidFromAdjacentSources(Level level, BlockPos pos) {
        if (fluidTank.getCapacity() - fluidTank.getFluidAmount() <= 0) {
            return;
        }
        ConduitLink.pullFromAdjacent(
                level,
                pos,
                FluidPipeBlockEntity.class,
                conduitConfig,
                Capabilities.FluidHandler.BLOCK,
                (neighborPos, dir, source) -> {
                    int toPull = Math.min(transferRate, fluidTank.getCapacity() - fluidTank.getFluidAmount());
                    if (toPull <= 0) {
                        return false;
                    }
                    FluidStack simulated;
                    if (fluidTank.isEmpty()) {
                        simulated = source.drain(toPull, IFluidHandler.FluidAction.SIMULATE);
                    } else {
                        FluidStack request = fluidTank.getFluid().copyWithAmount(toPull);
                        simulated = source.drain(request, IFluidHandler.FluidAction.SIMULATE);
                    }
                    if (!simulated.isEmpty()) {
                        int accepted = fluidTank.fill(simulated, IFluidHandler.FluidAction.EXECUTE);
                        if (accepted > 0) {
                            lastSourcePos = neighborPos;
                            source.drain(simulated.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                    return fluidTank.getCapacity() - fluidTank.getFluidAmount() > 0;
                }
        );
    }

    /**
     * Hands the buffered fluid back to the machine it was pulled from (and then to any other
     * adjacent non-pipe fluid handler) before this pipe is broken. Whatever does not fit is lost.
     */
    public void returnFluidToNeighbors(Level level, BlockPos pos) {
        if (fluidTank.isEmpty()) {
            return;
        }
        if (lastSourcePos != null) {
            Direction dir = directionTowards(pos, lastSourcePos);
            if (dir != null) {
                pushBackInto(level, lastSourcePos, dir);
            }
        }
        for (Direction dir : Direction.values()) {
            if (fluidTank.isEmpty()) {
                return;
            }
            BlockPos neighborPos = pos.relative(dir);
            if (neighborPos.equals(lastSourcePos)) {
                continue;
            }
            pushBackInto(level, neighborPos, dir);
        }
    }

    private void pushBackInto(Level level, BlockPos targetPos, Direction dir) {
        BlockEntity neighborBe = level.getBlockEntity(targetPos);
        if (neighborBe == null || neighborBe instanceof FluidPipeBlockEntity) {
            return;
        }
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, dir.getOpposite());
        if (target == null) {
            return;
        }
        FluidStack toSend = fluidTank.getFluid().copyWithAmount(fluidTank.getFluidAmount());
        int accepted = target.fill(toSend, IFluidHandler.FluidAction.EXECUTE);
        if (accepted > 0) {
            fluidTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Nullable
    private static Direction directionTowards(BlockPos from, BlockPos to) {
        for (Direction dir : Direction.values()) {
            if (from.relative(dir).equals(to)) {
                return dir;
            }
        }
        return null;
    }

    /**
     * Applies this pipe's heat retention {@code factor} to {@code stack}, losing part of the
     * temperature above ambient for every pipe block the fluid travelled through.
     */
    private FluidStack applyHeatLoss(FluidStack stack, double factor) {
        if (stack.isEmpty() || factor >= 1.0D
                || !stack.has(ModDataComponents.FLUID_TEMPERATURE.get())
                && !stack.has(ModDataComponents.FLUID_MULTIPLIER.get())) {
            return stack;
        }
        double temperature = ThermalFluidHelper.getTemperature(stack);
        double multiplier = ThermalFluidHelper.getMultiplier(stack);
        double cooledTemperature = FluidConduitTier.AMBIENT_TEMPERATURE
                + (temperature - FluidConduitTier.AMBIENT_TEMPERATURE) * factor;
        double cooledMultiplier = ThermalFluidHelper.DEFAULT_MULTIPLIER
                + (multiplier - ThermalFluidHelper.DEFAULT_MULTIPLIER) * factor;
        return ThermalFluidHelper.withThermalData(stack, cooledTemperature, cooledMultiplier);
    }

    /**
     * Delivers the buffered fluid to the receivers pre-computed for this pipe. The route list is
     * rebuilt only after the network changed, so a steady network costs one cache lookup per tick.
     */
    private void distributeFluidAlongNetwork(Level level, BlockPos startPos) {
        List<FluidDeliveryTarget> targets = NETWORK_CACHE.routes(level, startPos, () -> scanNetwork(level, startPos));
        ConduitLink.distribute(
                level,
                targets,
                FluidDeliveryTarget::pos,
                FluidDeliveryTarget::accessSide,
                Capabilities.FluidHandler.BLOCK,
                () -> !fluidTank.isEmpty(),
                (target, receiver) -> {
                    int maxPush = Math.min(transferRate, fluidTank.getFluidAmount());
                    if (maxPush <= 0) {
                        return;
                    }
                    FluidStack toSend = applyHeatLoss(fluidTank.getFluid().copyWithAmount(maxPush), target.heatRetention());
                    int accepted = receiver.fill(toSend, IFluidHandler.FluidAction.EXECUTE);
                    if (accepted > 0) {
                        fluidTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                    }
                }
        );
    }

    /**
     * Walks the connected pipe network once and records every reachable non-pipe fluid handler.
     */
    private List<FluidDeliveryTarget> scanNetwork(Level level, BlockPos startPos) {
        return ConduitLink.scanNetwork(
                level,
                startPos,
                FluidPipeBlockEntity.class,
                FluidPipeBlockEntity::getConduitConfig,
                heatRetention,
                (retention, pipe) -> retention * pipe.heatRetention,
                FluidDeliveryTarget::new
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluidTank.writeToNBT(registries, tag);
        ResourceLocation lockedFluidKey = BuiltInRegistries.FLUID.getKey(lockedFluid);
        tag.putString("LockedFluid", lockedFluidKey.toString());
        tag.putInt("EmptyTicks", emptyTicks);
        conduitConfig.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidTank.readFromNBT(registries, tag);
        if (tag.contains("LockedFluid")) {
            ResourceLocation key = ResourceLocation.tryParse(tag.getString("LockedFluid"));
            Fluid parsed = key != null ? BuiltInRegistries.FLUID.get(key) : Fluids.EMPTY;
            lockedFluid = parsed != null ? parsed : Fluids.EMPTY;
        } else {
            lockedFluid = fluidTank.isEmpty() ? Fluids.EMPTY : fluidTank.getFluid().getFluid();
        }
        emptyTicks = tag.getInt("EmptyTicks");
        conduitConfig.load(tag);
        if (!fluidTank.isEmpty()) {
            lockedFluid = fluidTank.getFluid().getFluid();
            emptyTicks = 0;
        }
    }
}
