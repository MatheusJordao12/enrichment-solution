package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.ReactorCoreBlock;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Nuclear Reactor Core BlockEntity skeleton.
 * Tracks fuel burn time, core heat, and generates Forge Energy (FE) while pushing power to adjacent blocks.
 * Slot 0: Nuclear Fuel (e.g. Enriched Uranium).
 */
public class ReactorCoreBlockEntity extends AbstractMachineBlockEntity {

    public static final int SLOT_FUEL = 0;

    private int burnTime = 0;
    private int heat = 0;

    public ReactorCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.REACTOR_CORE.get(),
                pos,
                blockState,
                ModServerConfig.get(ModServerConfig.REACTOR_ENERGY_CAPACITY),
                0,
                ModServerConfig.get(ModServerConfig.REACTOR_MAX_EXTRACT),
                1
        );
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        boolean active = false;

        if (isStructureValid(level, pos)) {
            if (burnTime <= 0 && hasFuel()) {
                ItemStack consumed = itemHandler.extractItem(SLOT_FUEL, 1, false);
                int baseBurnTime = ModServerConfig.get(ModServerConfig.REACTOR_FUEL_BURN_TIME);
                burnTime = consumed.is(ModItems.URANIUM_PELLET.get()) ? baseBurnTime * 2 : baseBurnTime;
                setChanged();
            }

            if (burnTime > 0) {
                burnTime--;
                energyStorage.generateInternal(ModServerConfig.get(ModServerConfig.REACTOR_FE_PER_TICK), false);
                heat = Math.min(getMaxHeat(), heat + 1);
                active = true;
                setChanged();
            } else if (heat > 0) {
                heat = Math.max(0, heat - 2);
                setChanged();
            }
        } else if (heat > 0) {
            heat = Math.max(0, heat - 2);
            setChanged();
        }

        if (state.getValue(ReactorCoreBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(ReactorCoreBlock.ACTIVE, active), Block.UPDATE_ALL);
        }

        pushEnergyToNeighbors(level, pos);
    }

    private boolean hasFuel() {
        ItemStack fuel = itemHandler.getStackInSlot(SLOT_FUEL);
        return !fuel.isEmpty() && (fuel.is(ModItems.ENRICHED_URANIUM.get()) || fuel.is(ModItems.URANIUM_PELLET.get()));
    }

    /**
     * Placeholder multiblock validation hook.
     * Expand this to require Reactor Casing / Coolant / Control Rods around the core.
     */
    public boolean isStructureValid(Level level, BlockPos pos) {
        return true;
    }

    private void pushEnergyToNeighbors(Level level, BlockPos pos) {
        if (energyStorage.getEnergyStored() <= 0) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null) {
                continue;
            }
            IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());
            if (receiver != null && receiver.canReceive()) {
                int extractable = energyStorage.extractEnergy(ModServerConfig.get(ModServerConfig.REACTOR_MAX_EXTRACT), true);
                int accepted = receiver.receiveEnergy(extractable, false);
                if (accepted > 0) {
                    energyStorage.extractEnergy(accepted, false);
                }
            }
        }
    }

    public int getHeat() {
        return heat;
    }

    public int getMaxHeat() {
        return ModServerConfig.get(ModServerConfig.REACTOR_MAX_HEAT);
    }

    public int getBurnTime() {
        return burnTime;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("Heat", heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTime = tag.getInt("BurnTime");
        heat = tag.getInt("Heat");
    }
}
