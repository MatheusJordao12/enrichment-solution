package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.HeatExchangerBlock;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.menu.HeatExchangerMenu;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import com.enrichmentsolution.mod.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Multiblock Heat Exchanger BlockEntity — Stage 1 of the Geothermal Binary Cycle (closed-loop Option A).
 * Connected HeatExchangerBlocks form a multiblock up to 20x20x20, scaling tank capacities and boil rate
 * with blockCount. Reads temperature and thermal multiplier from both adjacent TankBlockEntity/TankPortBlockEntity
 * structures and piped warm water DataComponents, taking the maximum of the two.
 */
public class HeatExchangerBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    public static final int MAX_SPAN = 20;
    public static final int ISOBUTANE_TANK_CAPACITY = 16_000;
    public static final int WATER_TANK_CAPACITY = 8_000;
    public static final int MIN_BOIL_RATE = 5;
    public static final int MAX_BOIL_RATE = 50;
    public static final int MAX_PUSH_RATE = 200;

    private BlockPos masterPos;
    private int blockCount = 1;
    private final List<BlockPos> members = new ArrayList<>();

    private final FluidTank waterTank = new FluidTank(WATER_TANK_CAPACITY,
            stack -> stack.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != Fluids.WATER) {
                return 0;
            }
            if (fluid.isEmpty()) {
                int filled = Math.min(capacity, resource.getAmount());
                if (action.execute() && filled > 0) {
                    fluid = resource.copyWithAmount(filled);
                    onContentsChanged();
                }
                return filled;
            }
            if (fluid.getFluid() != Fluids.WATER) {
                return 0;
            }
            int space = capacity - fluid.getAmount();
            int filled = Math.min(space, resource.getAmount());
            if (action.execute() && filled > 0) {
                int oldAmt = fluid.getAmount();
                int newAmt = oldAmt + filled;
                double existingTemp = fluid.getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), 20.0D);
                double existingMult = fluid.getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), 1.0D);
                double incomingTemp = resource.getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), 20.0D);
                double incomingMult = resource.getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), 1.0D);
                fluid.setAmount(newAmt);
                fluid.set(ModDataComponents.FLUID_TEMPERATURE.get(), (existingTemp * oldAmt + incomingTemp * filled) / newAmt);
                fluid.set(ModDataComponents.FLUID_MULTIPLIER.get(), (existingMult * oldAmt + incomingMult * filled) / newAmt);
                onContentsChanged();
            }
            return filled;
        }
    };

    private final FluidTank liquidIsobutaneTank = new FluidTank(ISOBUTANE_TANK_CAPACITY,
            stack -> stack.getFluid() == ModFluids.LIQUID_ISOBUTANE.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final FluidTank gaseousIsobutaneTank = new FluidTank(ISOBUTANE_TANK_CAPACITY,
            stack -> stack.getFluid() == ModFluids.GASEOUS_ISOBUTANE.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final IFluidHandler fluidHandler = new ExchangerFluidHandler();
    private final SideConfigHolder sideConfig = SideConfigHolder.createDefault(
            SideConfigMode.NONE,
            SideConfigMode.INPUT,
            SideConfigMode.INPUT,
            SideConfigMode.OUTPUT,
            SideConfigMode.INPUT,
            SideConfigMode.INPUT
    );

    private double currentTemperature = 20.0D;
    private double currentMultiplier = 1.0D;
    private int activeBoilRate = 0;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            HeatExchangerBlockEntity master = getMasterEntity();
            if (master == null) {
                return 0;
            }
            int waterAmt = master.waterTank.getFluidAmount();
            int waterCap = master.waterTank.getCapacity();
            int liquidAmt = master.liquidIsobutaneTank.getFluidAmount();
            int liquidCap = master.liquidIsobutaneTank.getCapacity();
            int gasAmt = master.gaseousIsobutaneTank.getFluidAmount();
            int gasCap = master.gaseousIsobutaneTank.getCapacity();
            int boilRate = master.activeBoilRate;
            return switch (index) {
                case HeatExchangerMenu.DATA_WATER_AMOUNT_LOW -> waterAmt & 0xFFFF;
                case HeatExchangerMenu.DATA_WATER_AMOUNT_HIGH -> (waterAmt >>> 16) & 0xFFFF;
                case HeatExchangerMenu.DATA_WATER_CAP_LOW -> waterCap & 0xFFFF;
                case HeatExchangerMenu.DATA_WATER_CAP_HIGH -> (waterCap >>> 16) & 0xFFFF;
                case HeatExchangerMenu.DATA_LIQUID_AMOUNT_LOW -> liquidAmt & 0xFFFF;
                case HeatExchangerMenu.DATA_LIQUID_AMOUNT_HIGH -> (liquidAmt >>> 16) & 0xFFFF;
                case HeatExchangerMenu.DATA_LIQUID_CAP_LOW -> liquidCap & 0xFFFF;
                case HeatExchangerMenu.DATA_LIQUID_CAP_HIGH -> (liquidCap >>> 16) & 0xFFFF;
                case HeatExchangerMenu.DATA_GAS_AMOUNT_LOW -> gasAmt & 0xFFFF;
                case HeatExchangerMenu.DATA_GAS_AMOUNT_HIGH -> (gasAmt >>> 16) & 0xFFFF;
                case HeatExchangerMenu.DATA_GAS_CAP_LOW -> gasCap & 0xFFFF;
                case HeatExchangerMenu.DATA_GAS_CAP_HIGH -> (gasCap >>> 16) & 0xFFFF;
                case HeatExchangerMenu.DATA_TEMP_TENTHS -> Mth.clamp((int) Math.round(master.currentTemperature * 10.0D), 0, Short.MAX_VALUE);
                case HeatExchangerMenu.DATA_MULT_HUNDREDTHS -> Mth.clamp((int) Math.round(master.currentMultiplier * 100.0D), 0, Short.MAX_VALUE);
                case HeatExchangerMenu.DATA_BOIL_RATE_LOW -> boilRate & 0xFFFF;
                case HeatExchangerMenu.DATA_BOIL_RATE_HIGH -> (boilRate >>> 16) & 0xFFFF;
                case HeatExchangerMenu.DATA_BLOCK_COUNT -> master.blockCount;
                case HeatExchangerMenu.DATA_WATER_TEMP_TENTHS -> Mth.clamp(
                        (int) Math.round(master.waterTank.getFluid()
                                .getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), 20.0D) * 10.0D),
                        0, Short.MAX_VALUE);
                case HeatExchangerMenu.DATA_WATER_MULT_HUNDREDTHS -> Mth.clamp(
                        (int) Math.round(master.waterTank.getFluid()
                                .getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), 1.0D) * 100.0D),
                        0, Short.MAX_VALUE);
                default -> {
                    if (index >= HeatExchangerMenu.DATA_SIDE_START && index < HeatExchangerMenu.DATA_SIDE_START + 6) {
                        yield master.sideConfig.getMode(RelativeSide.fromOrdinal(index - HeatExchangerMenu.DATA_SIDE_START)).ordinal();
                    }
                    yield 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {
            // Synced server -> client only
        }

        @Override
        public int getCount() {
            return HeatExchangerMenu.DATA_COUNT;
        }
    };

    public HeatExchangerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.HEAT_EXCHANGER.get(), pos, blockState, 0, 0, 0, 0);
        this.masterPos = pos;
        this.members.add(pos);
    }

    public boolean isMaster() {
        return worldPosition.equals(masterPos);
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    @Nullable
    public HeatExchangerBlockEntity getMasterEntity() {
        if (isMaster()) {
            return this;
        }
        if (level != null && level.getBlockEntity(masterPos) instanceof HeatExchangerBlockEntity master) {
            return master;
        }
        return this;
    }

    public int getBlockCount() {
        HeatExchangerBlockEntity master = getMasterEntity();
        return master != null ? master.blockCount : 1;
    }

    public FluidTank getWaterTank() {
        HeatExchangerBlockEntity master = getMasterEntity();
        return master != null ? master.waterTank : this.waterTank;
    }

    public FluidTank getLiquidIsobutaneTank() {
        HeatExchangerBlockEntity master = getMasterEntity();
        return master != null ? master.liquidIsobutaneTank : this.liquidIsobutaneTank;
    }

    public FluidTank getGaseousIsobutaneTank() {
        HeatExchangerBlockEntity master = getMasterEntity();
        return master != null ? master.gaseousIsobutaneTank : this.gaseousIsobutaneTank;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enrichment_solution.heat_exchanger");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        HeatExchangerBlockEntity master = getMasterEntity();
        HeatExchangerBlockEntity target = master != null ? master : this;
        return new HeatExchangerMenu(containerId, playerInventory, target, target.data);
    }

    public SideConfigMode getRelativeSideMode(RelativeSide side) {
        HeatExchangerBlockEntity master = getMasterEntity();
        HeatExchangerBlockEntity target = master != null ? master : this;
        return target.sideConfig.getMode(side);
    }

    public void cycleRelativeSideMode(int ordinal) {
        HeatExchangerBlockEntity master = getMasterEntity();
        HeatExchangerBlockEntity target = master != null ? master : this;
        target.sideConfig.cycleMode(RelativeSide.fromOrdinal(ordinal));
        target.setChanged();
        if (target.level != null) {
            List<BlockPos> scanList = target.members.isEmpty() ? List.of(target.worldPosition) : target.members;
            for (BlockPos memberPos : scanList) {
                target.level.invalidateCapabilities(memberPos);
                target.level.updateNeighborsAt(memberPos, target.level.getBlockState(memberPos).getBlock());
            }
        }
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        HeatExchangerBlockEntity master = getMasterEntity();
        HeatExchangerBlockEntity target = master != null ? master : this;
        Direction facing = getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        return target.sideConfig.wrapFluidHandler(target.fluidHandler, side, facing);
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        if (!isMaster()) {
            return;
        }

        TankBlockEntity adjacentTank = findBestAdjacentTankMaster(level);
        boolean hasTankWater = adjacentTank != null && adjacentTank.getMasterTank().getFluidAmount() > 0;
        double tankTemp = adjacentTank != null ? adjacentTank.getTemperature() : 20.0D;
        double tankMult = adjacentTank != null ? adjacentTank.getMultiplier() : 1.0D;

        FluidStack pipedStack = waterTank.getFluid();
        boolean hasPipedWater = !pipedStack.isEmpty();
        double pipedTemp = hasPipedWater
                ? pipedStack.getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), 20.0D)
                : 20.0D;
        double pipedMult = hasPipedWater
                ? pipedStack.getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), 1.0D)
                : 1.0D;

        boolean hasHeatWater = hasTankWater || hasPipedWater;
        if (hasTankWater && hasPipedWater) {
            currentTemperature = Math.max(tankTemp, pipedTemp);
            currentMultiplier = Math.max(tankMult, pipedMult);
        } else if (hasTankWater) {
            currentTemperature = tankTemp;
            currentMultiplier = tankMult;
        } else if (hasPipedWater) {
            currentTemperature = pipedTemp;
            currentMultiplier = pipedMult;
        } else {
            currentTemperature = adjacentTank != null ? tankTemp : 20.0D;
            currentMultiplier = adjacentTank != null ? tankMult : 1.0D;
        }

        int count = Math.max(1, blockCount);
        int scaledRate = Mth.clamp(
                (int) Math.round(5.0D * currentMultiplier) * count,
                MIN_BOIL_RATE * count,
                MAX_BOIL_RATE * count
        );
        boolean isWorking = false;
        activeBoilRate = 0;

        if (hasHeatWater && !liquidIsobutaneTank.isEmpty() && gaseousIsobutaneTank.getSpace() > 0) {
            int toBoil = Math.min(scaledRate, Math.min(liquidIsobutaneTank.getFluidAmount(), gaseousIsobutaneTank.getSpace()));
            if (toBoil > 0) {
                liquidIsobutaneTank.drain(toBoil, IFluidHandler.FluidAction.EXECUTE);
                gaseousIsobutaneTank.fill(new FluidStack(ModFluids.GASEOUS_ISOBUTANE.get(), toBoil), IFluidHandler.FluidAction.EXECUTE);

                if (hasTankWater && (!hasPipedWater || tankMult >= pipedMult)) {
                    adjacentTank.getMasterTank().drain(1, IFluidHandler.FluidAction.EXECUTE);
                } else {
                    waterTank.drain(1, IFluidHandler.FluidAction.EXECUTE);
                }

                activeBoilRate = toBoil;
                isWorking = true;
                setChanged();
            }
        }

        pushGasToNeighbors(level, count);

        List<BlockPos> scanList = members.isEmpty() ? List.of(pos) : members;
        for (BlockPos memberPos : scanList) {
            BlockState memberState = level.getBlockState(memberPos);
            if (memberState.getBlock() instanceof HeatExchangerBlock && memberState.getValue(HeatExchangerBlock.LIT) != isWorking) {
                level.setBlock(memberPos, memberState.setValue(HeatExchangerBlock.LIT, isWorking), Block.UPDATE_ALL);
            }
        }
    }

    @Nullable
    private TankBlockEntity findBestAdjacentTankMaster(Level level) {
        TankBlockEntity best = null;
        boolean bestHasWater = false;
        List<BlockPos> scanList = members.isEmpty() ? List.of(worldPosition) : members;
        for (BlockPos member : scanList) {
            for (Direction dir : Direction.values()) {
                BlockEntity be = level.getBlockEntity(member.relative(dir));
                TankBlockEntity candidate = null;
                if (be instanceof TankBlockEntity tank) {
                    candidate = tank.getMasterEntity();
                } else if (be instanceof TankPortBlockEntity port) {
                    candidate = port.findConnectedMaster();
                }
                if (candidate == null) {
                    continue;
                }
                boolean hasWater = candidate.getMasterTank().getFluidAmount() > 0;
                if (best == null
                        || (hasWater && !bestHasWater)
                        || (hasWater == bestHasWater && candidate.getMultiplier() > best.getMultiplier())) {
                    best = candidate;
                    bestHasWater = hasWater;
                }
            }
        }
        return best;
    }

    private void pushGasToNeighbors(Level level, int count) {
        if (gaseousIsobutaneTank.isEmpty()) {
            return;
        }
        int maxPush = MAX_PUSH_RATE * count;
        List<BlockPos> scanList = members.isEmpty() ? List.of(worldPosition) : members;
        for (BlockPos memberPos : scanList) {
            if (gaseousIsobutaneTank.isEmpty()) {
                break;
            }
            BlockState memberState = level.getBlockState(memberPos);
            Direction memberFacing = memberState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? memberState.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : Direction.NORTH;
            for (Direction dir : Direction.values()) {
                if (gaseousIsobutaneTank.isEmpty()) {
                    break;
                }
                if (!sideConfig.canPushTo(dir, memberFacing)) {
                    continue;
                }
                BlockPos neighborPos = memberPos.relative(dir);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                if (neighbor == null
                        || neighbor instanceof HeatExchangerBlockEntity
                        || neighbor instanceof TankBlockEntity
                        || neighbor instanceof TankPortBlockEntity) {
                    continue;
                }
                IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
                if (target == null) {
                    continue;
                }
                int amountToPush = Math.min(maxPush, gaseousIsobutaneTank.getFluidAmount());
                FluidStack toSend = new FluidStack(ModFluids.GASEOUS_ISOBUTANE.get(), amountToPush);
                int accepted = target.fill(toSend, IFluidHandler.FluidAction.EXECUTE);
                if (accepted > 0) {
                    gaseousIsobutaneTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                    setChanged();
                }
            }
        }
    }

    public static void rebuildNetwork(Level level, BlockPos seedPos, @Nullable BlockPos ignoredPos) {
        if (level.isClientSide) {
            return;
        }
        if (seedPos.equals(ignoredPos) || !(level.getBlockState(seedPos).getBlock() instanceof HeatExchangerBlock)) {
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> component = new ArrayList<>();

        visited.add(seedPos);
        queue.add(seedPos);

        int minX = seedPos.getX(), maxX = seedPos.getX();
        int minY = seedPos.getY(), maxY = seedPos.getY();
        int minZ = seedPos.getZ(), maxZ = seedPos.getZ();

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            component.add(current);

            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (next.equals(ignoredPos) || visited.contains(next)) {
                    continue;
                }
                if (!(level.getBlockState(next).getBlock() instanceof HeatExchangerBlock)) {
                    continue;
                }
                int nMinX = Math.min(minX, next.getX());
                int nMaxX = Math.max(maxX, next.getX());
                int nMinY = Math.min(minY, next.getY());
                int nMaxY = Math.max(maxY, next.getY());
                int nMinZ = Math.min(minZ, next.getZ());
                int nMaxZ = Math.max(maxZ, next.getZ());
                if (nMaxX - nMinX + 1 > MAX_SPAN || nMaxY - nMinY + 1 > MAX_SPAN || nMaxZ - nMinZ + 1 > MAX_SPAN) {
                    continue;
                }
                minX = nMinX; maxX = nMaxX;
                minY = nMinY; maxY = nMaxY;
                minZ = nMinZ; maxZ = nMaxZ;
                visited.add(next);
                queue.add(next);
            }
        }

        if (component.isEmpty()) {
            return;
        }

        component.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getZ));
        BlockPos newMasterPos = component.get(0);

        int totalWater = 0;
        double weightedTempSum = 0.0D;
        double weightedMultSum = 0.0D;
        int totalLiquid = 0;
        int totalGas = 0;

        for (BlockPos pos : component) {
            if (level.getBlockEntity(pos) instanceof HeatExchangerBlockEntity be) {
                FluidStack w = be.waterTank.getFluid();
                if (!w.isEmpty()) {
                    int amt = w.getAmount();
                    totalWater += amt;
                    weightedTempSum += w.getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), 20.0D) * amt;
                    weightedMultSum += w.getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), 1.0D) * amt;
                }
                totalLiquid += be.liquidIsobutaneTank.getFluidAmount();
                totalGas += be.gaseousIsobutaneTank.getFluidAmount();
            }
        }

        int newWaterCapacity = component.size() * WATER_TANK_CAPACITY;
        int newIsoCapacity = component.size() * ISOBUTANE_TANK_CAPACITY;
        int clampedWater = Math.min(totalWater, newWaterCapacity);
        int clampedLiquid = Math.min(totalLiquid, newIsoCapacity);
        int clampedGas = Math.min(totalGas, newIsoCapacity);

        FluidStack mergedWater = FluidStack.EMPTY;
        if (clampedWater > 0) {
            mergedWater = new FluidStack(Fluids.WATER, clampedWater);
            if (totalWater > 0) {
                mergedWater.set(ModDataComponents.FLUID_TEMPERATURE.get(), weightedTempSum / totalWater);
                mergedWater.set(ModDataComponents.FLUID_MULTIPLIER.get(), weightedMultSum / totalWater);
            }
        }

        for (BlockPos pos : component) {
            if (level.getBlockEntity(pos) instanceof HeatExchangerBlockEntity be) {
                be.masterPos = newMasterPos;
                if (pos.equals(newMasterPos)) {
                    be.blockCount = component.size();
                    be.members.clear();
                    be.members.addAll(component);
                    be.waterTank.setCapacity(newWaterCapacity);
                    be.waterTank.setFluid(mergedWater);
                    be.liquidIsobutaneTank.setCapacity(newIsoCapacity);
                    be.liquidIsobutaneTank.setFluid(clampedLiquid > 0
                            ? new FluidStack(ModFluids.LIQUID_ISOBUTANE.get(), clampedLiquid)
                            : FluidStack.EMPTY);
                    be.gaseousIsobutaneTank.setCapacity(newIsoCapacity);
                    be.gaseousIsobutaneTank.setFluid(clampedGas > 0
                            ? new FluidStack(ModFluids.GASEOUS_ISOBUTANE.get(), clampedGas)
                            : FluidStack.EMPTY);
                } else {
                    be.blockCount = 1;
                    be.members.clear();
                    be.activeBoilRate = 0;
                    be.waterTank.setCapacity(WATER_TANK_CAPACITY);
                    be.waterTank.setFluid(FluidStack.EMPTY);
                    be.liquidIsobutaneTank.setCapacity(ISOBUTANE_TANK_CAPACITY);
                    be.liquidIsobutaneTank.setFluid(FluidStack.EMPTY);
                    be.gaseousIsobutaneTank.setCapacity(ISOBUTANE_TANK_CAPACITY);
                    be.gaseousIsobutaneTank.setFluid(FluidStack.EMPTY);
                }
                be.setChanged();
            }
        }
    }

    public static void onBlockBroken(
            Level level,
            BlockPos brokenPos,
            int previousWaterAmount,
            double previousWaterTemp,
            double previousWaterMult,
            int previousLiquidAmount,
            int previousGasAmount
    ) {
        List<BlockPos> neighborExchangers = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos adj = brokenPos.relative(dir);
            if (level.getBlockState(adj).getBlock() instanceof HeatExchangerBlock) {
                neighborExchangers.add(adj);
            }
        }
        if (neighborExchangers.isEmpty()) {
            return;
        }
        Set<BlockPos> processed = new HashSet<>();
        for (BlockPos neighbor : neighborExchangers) {
            if (processed.contains(neighbor)) {
                continue;
            }
            rebuildNetwork(level, neighbor, brokenPos);
            if (level.getBlockEntity(neighbor) instanceof HeatExchangerBlockEntity be) {
                HeatExchangerBlockEntity master = be.getMasterEntity();
                if (master != null) {
                    processed.addAll(master.members);
                    if (previousWaterAmount > 0 && master.waterTank.getFluidAmount() == 0) {
                        int fillAmt = Math.min(previousWaterAmount, master.waterTank.getCapacity());
                        FluidStack water = new FluidStack(Fluids.WATER, fillAmt);
                        water.set(ModDataComponents.FLUID_TEMPERATURE.get(), previousWaterTemp);
                        water.set(ModDataComponents.FLUID_MULTIPLIER.get(), previousWaterMult);
                        master.waterTank.fill(water, IFluidHandler.FluidAction.EXECUTE);
                        previousWaterAmount = 0;
                    }
                    if (previousLiquidAmount > 0 && master.liquidIsobutaneTank.getFluidAmount() == 0) {
                        master.liquidIsobutaneTank.fill(new FluidStack(ModFluids.LIQUID_ISOBUTANE.get(),
                                Math.min(previousLiquidAmount, master.liquidIsobutaneTank.getCapacity())), IFluidHandler.FluidAction.EXECUTE);
                        previousLiquidAmount = 0;
                    }
                    if (previousGasAmount > 0 && master.gaseousIsobutaneTank.getFluidAmount() == 0) {
                        master.gaseousIsobutaneTank.fill(new FluidStack(ModFluids.GASEOUS_ISOBUTANE.get(),
                                Math.min(previousGasAmount, master.gaseousIsobutaneTank.getCapacity())), IFluidHandler.FluidAction.EXECUTE);
                        previousGasAmount = 0;
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("MasterPos", NbtUtils.writeBlockPos(masterPos));
        tag.putInt("BlockCount", blockCount);
        tag.putDouble("CurrentTemperature", currentTemperature);
        tag.putDouble("CurrentMultiplier", currentMultiplier);
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("LiquidIsobutaneTank", liquidIsobutaneTank.writeToNBT(registries, new CompoundTag()));
        tag.put("GaseousIsobutaneTank", gaseousIsobutaneTank.writeToNBT(registries, new CompoundTag()));
        sideConfig.saveAdditional(tag);

        ListTag memberList = new ListTag();
        for (BlockPos m : members) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", m.getX());
            entry.putInt("Y", m.getY());
            entry.putInt("Z", m.getZ());
            memberList.add(entry);
        }
        tag.put("Members", memberList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NbtUtils.readBlockPos(tag, "MasterPos").ifPresent(pos -> this.masterPos = pos);
        if (tag.contains("BlockCount")) {
            this.blockCount = Math.max(1, tag.getInt("BlockCount"));
        }
        if (tag.contains("CurrentTemperature")) {
            this.currentTemperature = tag.getDouble("CurrentTemperature");
        }
        if (tag.contains("CurrentMultiplier")) {
            this.currentMultiplier = tag.getDouble("CurrentMultiplier");
        }
        this.waterTank.setCapacity(this.blockCount * WATER_TANK_CAPACITY);
        this.liquidIsobutaneTank.setCapacity(this.blockCount * ISOBUTANE_TANK_CAPACITY);
        this.gaseousIsobutaneTank.setCapacity(this.blockCount * ISOBUTANE_TANK_CAPACITY);

        if (tag.contains("WaterTank")) {
            waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        }
        if (tag.contains("LiquidIsobutaneTank")) {
            liquidIsobutaneTank.readFromNBT(registries, tag.getCompound("LiquidIsobutaneTank"));
        }
        if (tag.contains("GaseousIsobutaneTank")) {
            gaseousIsobutaneTank.readFromNBT(registries, tag.getCompound("GaseousIsobutaneTank"));
        }
        sideConfig.loadAdditional(tag);

        this.members.clear();
        if (tag.contains("Members", Tag.TAG_LIST)) {
            ListTag memberList = tag.getList("Members", Tag.TAG_COMPOUND);
            for (int i = 0; i < memberList.size(); i++) {
                CompoundTag entry = memberList.getCompound(i);
                this.members.add(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
            }
        }
        if (this.members.isEmpty()) {
            this.members.add(worldPosition);
        }
    }

    private class ExchangerFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> liquidIsobutaneTank.getFluid();
                case 1 -> gaseousIsobutaneTank.getFluid();
                case 2 -> waterTank.getFluid();
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> liquidIsobutaneTank.getCapacity();
                case 1 -> gaseousIsobutaneTank.getCapacity();
                case 2 -> waterTank.getCapacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return switch (tank) {
                case 0 -> liquidIsobutaneTank.isFluidValid(stack);
                case 1 -> gaseousIsobutaneTank.isFluidValid(stack);
                case 2 -> waterTank.isFluidValid(stack);
                default -> false;
            };
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            if (resource.getFluid() == ModFluids.LIQUID_ISOBUTANE.get()) {
                return liquidIsobutaneTank.fill(resource, action);
            }
            if (resource.getFluid() == Fluids.WATER) {
                return waterTank.fill(resource, action);
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != ModFluids.GASEOUS_ISOBUTANE.get()) {
                return FluidStack.EMPTY;
            }
            return gaseousIsobutaneTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            return gaseousIsobutaneTank.drain(maxDrain, action);
        }
    }
}
