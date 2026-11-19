package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.heatexchanger.HeatExchangerIsobutaneHandler;
import com.enrichmentsolution.mod.common.block.entity.heatexchanger.HeatExchangerMultiblock;
import com.enrichmentsolution.mod.common.block.entity.heatexchanger.HeatExchangerThermalHandler;
import com.enrichmentsolution.mod.common.block.entity.network.MultiblockNetwork;
import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.enrichmentsolution.mod.common.menu.HeatExchangerMenu;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import com.enrichmentsolution.mod.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Multiblock Heat Exchanger BlockEntity — Stage 1 of the Geothermal Binary Cycle (closed-loop Option A).
 * Delegates thermal water evaluation, isobutane boiling/pushing, and multiblock topology to focused handlers.
 */
public class HeatExchangerBlockEntity extends AbstractMachineBlockEntity implements ISideConfigurableBlockEntity, MenuProvider {

    private final HeatExchangerMultiblock multiblock;
    private final HeatExchangerThermalHandler thermalHandler;
    private final HeatExchangerIsobutaneHandler isobutaneHandler;
    private final SideConfigHolder sideConfig = SideConfigHolder.createDefault(
            SideConfigMode.NONE,
            SideConfigMode.INPUT,
            SideConfigMode.INPUT,
            SideConfigMode.OUTPUT,
            SideConfigMode.INPUT,
            SideConfigMode.INPUT
    );

    private int activeBoilRate = 0;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            HeatExchangerBlockEntity master = getMasterEntity();
            if (master == null) {
                return 0;
            }
            FluidTank water = master.thermalHandler.getWaterTank();
            FluidTank liquid = master.isobutaneHandler.getLiquidIsobutaneTank();
            FluidTank gas = master.isobutaneHandler.getGaseousIsobutaneTank();
            int waterAmt = water.getFluidAmount();
            int waterCap = water.getCapacity();
            int liquidAmt = liquid.getFluidAmount();
            int liquidCap = liquid.getCapacity();
            int gasAmt = gas.getFluidAmount();
            int gasCap = gas.getCapacity();
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
                case HeatExchangerMenu.DATA_TEMP_TENTHS -> Mth.clamp((int) Math.round(master.thermalHandler.getCurrentTemperature() * 10.0D), 0, Short.MAX_VALUE);
                case HeatExchangerMenu.DATA_MULT_HUNDREDTHS -> Mth.clamp((int) Math.round(master.thermalHandler.getCurrentMultiplier() * 100.0D), 0, Short.MAX_VALUE);
                case HeatExchangerMenu.DATA_BOIL_RATE_LOW -> boilRate & 0xFFFF;
                case HeatExchangerMenu.DATA_BOIL_RATE_HIGH -> (boilRate >>> 16) & 0xFFFF;
                case HeatExchangerMenu.DATA_BLOCK_COUNT -> master.multiblock.getBlockCount();
                case HeatExchangerMenu.DATA_WATER_TEMP_TENTHS -> Mth.clamp(
                        (int) Math.round(water.getFluid().getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), 20.0D) * 10.0D),
                        0, Short.MAX_VALUE);
                case HeatExchangerMenu.DATA_WATER_MULT_HUNDREDTHS -> Mth.clamp(
                        (int) Math.round(water.getFluid().getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), 1.0D) * 100.0D),
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
        this.multiblock = new HeatExchangerMultiblock(pos);
        this.thermalHandler = new HeatExchangerThermalHandler(this::setChanged);
        this.isobutaneHandler = new HeatExchangerIsobutaneHandler(thermalHandler.getWaterTank(), this::setChanged);
    }

    public boolean isMaster() {
        return multiblock.isMaster(worldPosition);
    }

    public BlockPos getMasterPos() {
        return multiblock.getMasterPos();
    }

    @Nullable
    public HeatExchangerBlockEntity getMasterEntity() {
        return MultiblockNetwork.resolveMaster(level, worldPosition, multiblock.getMasterPos(), HeatExchangerBlockEntity.class, this);
    }

    public HeatExchangerMultiblock getMultiblock() {
        return multiblock;
    }

    public int getBlockCount() {
        HeatExchangerBlockEntity master = getMasterEntity();
        return master != null ? master.multiblock.getBlockCount() : 1;
    }

    public FluidTank getWaterTank() {
        HeatExchangerBlockEntity master = getMasterEntity();
        return master != null ? master.thermalHandler.getWaterTank() : thermalHandler.getWaterTank();
    }

    public FluidTank getLiquidIsobutaneTank() {
        HeatExchangerBlockEntity master = getMasterEntity();
        return master != null ? master.isobutaneHandler.getLiquidIsobutaneTank() : isobutaneHandler.getLiquidIsobutaneTank();
    }

    public FluidTank getGaseousIsobutaneTank() {
        HeatExchangerBlockEntity master = getMasterEntity();
        return master != null ? master.isobutaneHandler.getGaseousIsobutaneTank() : isobutaneHandler.getGaseousIsobutaneTank();
    }

    public FluidTank getRawWaterTank() {
        return thermalHandler.getWaterTank();
    }

    public FluidTank getRawLiquidIsobutaneTank() {
        return isobutaneHandler.getLiquidIsobutaneTank();
    }

    public FluidTank getRawGaseousIsobutaneTank() {
        return isobutaneHandler.getGaseousIsobutaneTank();
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

    @Override
    public SideConfigMode getRelativeSideMode(RelativeSide side) {
        HeatExchangerBlockEntity master = getMasterEntity();
        HeatExchangerBlockEntity target = master != null ? master : this;
        return target.sideConfig.getMode(side);
    }

    @Override
    public void cycleRelativeSideMode(int ordinal) {
        HeatExchangerBlockEntity master = getMasterEntity();
        HeatExchangerBlockEntity target = master != null ? master : this;
        target.sideConfig.cycleMode(RelativeSide.fromOrdinal(ordinal));
        target.setChanged();
        if (target.level != null) {
            target.multiblock.invalidateMemberCapabilities(target.level, target.worldPosition);
        }
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        HeatExchangerBlockEntity master = getMasterEntity();
        HeatExchangerBlockEntity target = master != null ? master : this;
        Direction facing = getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        return target.sideConfig.wrapFluidHandler(target.isobutaneHandler.getFluidHandler(), side, facing);
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        if (!isMaster()) {
            return;
        }

        List<BlockPos> scanList = multiblock.getScanList(pos);
        HeatExchangerThermalHandler.HeatSourceResult heat = thermalHandler.evaluateHeatSources(level, scanList);

        int count = Math.max(1, multiblock.getBlockCount());
        int scaledRate = Mth.clamp(
                (int) Math.round(5.0D * thermalHandler.getCurrentMultiplier()) * count,
                ModServerConfig.get(ModServerConfig.HEAT_EXCHANGER_MIN_BOIL_RATE) * count,
                ModServerConfig.get(ModServerConfig.HEAT_EXCHANGER_MAX_BOIL_RATE) * count
        );

        boolean isWorking = false;
        activeBoilRate = 0;
        if (heat.hasHeatWater()) {
            int boiled = isobutaneHandler.boil(scaledRate);
            if (boiled > 0) {
                thermalHandler.consumeHeatWater(heat);
                activeBoilRate = boiled;
                isWorking = true;
                setChanged();
            }
        }

        isobutaneHandler.pushGasToNeighbors(level, scanList, sideConfig, count, this::setChanged);
        multiblock.updateLitState(level, pos, isWorking);
    }

    public void applyNetworkState(
            BlockPos newMasterPos,
            List<BlockPos> component,
            boolean isNewMaster,
            int waterCap,
            int isoCap,
            int newWaterCapacity,
            int newIsoCapacity,
            FluidStack mergedWater,
            int clampedLiquid,
            int clampedGas
    ) {
        multiblock.applyState(newMasterPos, component, isNewMaster);
        FluidTank water = thermalHandler.getWaterTank();
        FluidTank liquid = isobutaneHandler.getLiquidIsobutaneTank();
        FluidTank gas = isobutaneHandler.getGaseousIsobutaneTank();
        if (isNewMaster) {
            water.setCapacity(newWaterCapacity);
            water.setFluid(mergedWater);
            liquid.setCapacity(newIsoCapacity);
            liquid.setFluid(clampedLiquid > 0 ? new FluidStack(ModFluids.LIQUID_ISOBUTANE.get(), clampedLiquid) : FluidStack.EMPTY);
            gas.setCapacity(newIsoCapacity);
            gas.setFluid(clampedGas > 0 ? new FluidStack(ModFluids.GASEOUS_ISOBUTANE.get(), clampedGas) : FluidStack.EMPTY);
        } else {
            this.activeBoilRate = 0;
            water.setCapacity(waterCap);
            water.setFluid(FluidStack.EMPTY);
            liquid.setCapacity(isoCap);
            liquid.setFluid(FluidStack.EMPTY);
            gas.setCapacity(isoCap);
            gas.setFluid(FluidStack.EMPTY);
        }
        setChanged();
    }

    public static void rebuildNetwork(Level level, BlockPos seedPos, @Nullable BlockPos ignoredPos) {
        HeatExchangerMultiblock.rebuildNetwork(level, seedPos, ignoredPos);
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
        HeatExchangerMultiblock.onBlockBroken(
                level, brokenPos, previousWaterAmount, previousWaterTemp,
                previousWaterMult, previousLiquidAmount, previousGasAmount);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        multiblock.save(tag);
        thermalHandler.save(tag, registries);
        isobutaneHandler.save(tag, registries);
        sideConfig.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        multiblock.load(tag, worldPosition);
        int count = multiblock.getBlockCount();
        thermalHandler.load(tag, registries, count);
        isobutaneHandler.load(tag, registries, count);
        sideConfig.loadAdditional(tag);
    }
}
