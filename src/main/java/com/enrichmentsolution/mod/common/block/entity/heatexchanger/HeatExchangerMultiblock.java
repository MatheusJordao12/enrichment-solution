package com.enrichmentsolution.mod.common.block.entity.heatexchanger;

import com.enrichmentsolution.mod.common.block.HeatExchangerBlock;
import com.enrichmentsolution.mod.common.block.entity.HeatExchangerBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.network.MultiblockNetwork;
import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import com.enrichmentsolution.mod.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates Heat Exchanger multiblock topology, fluid merging on rebuild,
 * split handling on block break, and member LIT/capability updates.
 */
public class HeatExchangerMultiblock {

    private BlockPos masterPos;
    private int blockCount = 1;
    private final List<BlockPos> members = new ArrayList<>();

    public HeatExchangerMultiblock(BlockPos initialPos) {
        this.masterPos = initialPos;
        this.members.add(initialPos);
    }

    public boolean isMaster(BlockPos selfPos) {
        return selfPos.equals(masterPos);
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public List<BlockPos> getScanList(BlockPos fallbackPos) {
        return members.isEmpty() ? List.of(fallbackPos) : members;
    }

    public void updateLitState(Level level, BlockPos selfPos, boolean isWorking) {
        for (BlockPos memberPos : getScanList(selfPos)) {
            BlockState memberState = level.getBlockState(memberPos);
            if (memberState.getBlock() instanceof HeatExchangerBlock
                    && memberState.getValue(HeatExchangerBlock.LIT) != isWorking) {
                level.setBlock(memberPos, memberState.setValue(HeatExchangerBlock.LIT, isWorking), Block.UPDATE_ALL);
            }
        }
    }

    public void invalidateMemberCapabilities(Level level, BlockPos selfPos) {
        for (BlockPos memberPos : getScanList(selfPos)) {
            level.invalidateCapabilities(memberPos);
            level.updateNeighborsAt(memberPos, level.getBlockState(memberPos).getBlock());
        }
    }

    public void save(CompoundTag tag) {
        MultiblockNetwork.saveMembers(tag, masterPos, blockCount, members);
    }

    public void load(CompoundTag tag, BlockPos fallbackPos) {
        NbtUtils.readBlockPos(tag, "MasterPos").ifPresent(pos -> this.masterPos = pos);
        if (tag.contains("BlockCount")) {
            this.blockCount = Math.max(1, tag.getInt("BlockCount"));
        }
        MultiblockNetwork.loadMembers(tag, fallbackPos, this.members);
    }

    public static void rebuildNetwork(Level level, BlockPos seedPos, @Nullable BlockPos ignoredPos) {
        int maxSpan = ModServerConfig.get(ModServerConfig.HEAT_EXCHANGER_MAX_SPAN);
        List<BlockPos> component = MultiblockNetwork.discoverComponent(
                level, seedPos, ignoredPos, maxSpan, state -> state.getBlock() instanceof HeatExchangerBlock);
        if (component.isEmpty()) {
            return;
        }

        BlockPos newMasterPos = component.get(0);

        int totalWater = 0;
        double weightedTempSum = 0.0D;
        double weightedMultSum = 0.0D;
        int totalLiquid = 0;
        int totalGas = 0;

        for (BlockPos pos : component) {
            if (level.getBlockEntity(pos) instanceof HeatExchangerBlockEntity be) {
                FluidStack w = be.getRawWaterTank().getFluid();
                if (!w.isEmpty()) {
                    int amt = w.getAmount();
                    totalWater += amt;
                    weightedTempSum += w.getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), 20.0D) * amt;
                    weightedMultSum += w.getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), 1.0D) * amt;
                }
                totalLiquid += be.getRawLiquidIsobutaneTank().getFluidAmount();
                totalGas += be.getRawGaseousIsobutaneTank().getFluidAmount();
            }
        }

        int waterCap = ModServerConfig.get(ModServerConfig.HEAT_EXCHANGER_WATER_TANK_CAPACITY);
        int isoCap = ModServerConfig.get(ModServerConfig.HEAT_EXCHANGER_ISOBUTANE_TANK_CAPACITY);
        int newWaterCapacity = component.size() * waterCap;
        int newIsoCapacity = component.size() * isoCap;
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
                be.applyNetworkState(newMasterPos, component, pos.equals(newMasterPos),
                        waterCap, isoCap, newWaterCapacity, newIsoCapacity,
                        mergedWater, clampedLiquid, clampedGas);
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
        final int[] remWater = {previousWaterAmount};
        final int[] remLiquid = {previousLiquidAmount};
        final int[] remGas = {previousGasAmount};
        MultiblockNetwork.handleBlockBroken(
                level,
                brokenPos,
                state -> state.getBlock() instanceof HeatExchangerBlock,
                HeatExchangerBlockEntity.class,
                HeatExchangerMultiblock::rebuildNetwork,
                HeatExchangerBlockEntity::getMasterEntity,
                master -> master.getMultiblock().members,
                master -> {
                    if (remWater[0] > 0 && master.getRawWaterTank().getFluidAmount() == 0) {
                        int fillAmt = Math.min(remWater[0], master.getRawWaterTank().getCapacity());
                        FluidStack water = new FluidStack(Fluids.WATER, fillAmt);
                        water.set(ModDataComponents.FLUID_TEMPERATURE.get(), previousWaterTemp);
                        water.set(ModDataComponents.FLUID_MULTIPLIER.get(), previousWaterMult);
                        master.getRawWaterTank().fill(water, IFluidHandler.FluidAction.EXECUTE);
                        remWater[0] = 0;
                    }
                    if (remLiquid[0] > 0 && master.getRawLiquidIsobutaneTank().getFluidAmount() == 0) {
                        master.getRawLiquidIsobutaneTank().fill(new FluidStack(ModFluids.LIQUID_ISOBUTANE.get(),
                                Math.min(remLiquid[0], master.getRawLiquidIsobutaneTank().getCapacity())), IFluidHandler.FluidAction.EXECUTE);
                        remLiquid[0] = 0;
                    }
                    if (remGas[0] > 0 && master.getRawGaseousIsobutaneTank().getFluidAmount() == 0) {
                        master.getRawGaseousIsobutaneTank().fill(new FluidStack(ModFluids.GASEOUS_ISOBUTANE.get(),
                                Math.min(remGas[0], master.getRawGaseousIsobutaneTank().getCapacity())), IFluidHandler.FluidAction.EXECUTE);
                        remGas[0] = 0;
                    }
                }
        );
    }

    public void applyState(BlockPos newMasterPos, List<BlockPos> component, boolean isNewMaster) {
        this.masterPos = newMasterPos;
        if (isNewMaster) {
            this.blockCount = component.size();
            this.members.clear();
            this.members.addAll(component);
        } else {
            this.blockCount = 1;
            this.members.clear();
        }
    }
}
