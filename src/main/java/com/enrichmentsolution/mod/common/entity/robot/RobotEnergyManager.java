package com.enrichmentsolution.mod.common.entity.robot;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import com.enrichmentsolution.mod.common.item.EnergyCellBlockItem;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * Manages the Robot's energy-cell storage, charge/consume operations,
 * power-state transitions, and auto-recharge charger coordinates.
 */
public class RobotEnergyManager {

    private boolean wasPowered = false;
    private boolean rechargeCancelled = false;
    @Nullable
    private BlockPos chargerPos = null;

    public int getEnergyStored(ItemStackHandler inventory) {
        int total = 0;
        for (int i = RobotEntity.SLOT_CELL_START; i < RobotEntity.TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof EnergyCellBlockItem) {
                total += stack.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0);
            }
        }
        return total;
    }

    public int getMaxEnergyStored(ItemStackHandler inventory) {
        int total = 0;
        for (int i = RobotEntity.SLOT_CELL_START; i < RobotEntity.TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof EnergyCellBlockItem cellItem) {
                total += cellItem.getTier().getCapacity();
            }
        }
        return total;
    }

    public void consumeEnergy(RobotEntity robot, ItemStackHandler inventory, int amount) {
        if (amount <= 0 || robot.level().isClientSide) {
            return;
        }
        int remaining = amount;
        for (int i = RobotEntity.SLOT_CELL_START; i < RobotEntity.TOTAL_SLOTS && remaining > 0; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof EnergyCellBlockItem)) {
                continue;
            }
            int cellEnergy = stack.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0);
            if (cellEnergy <= 0) {
                continue;
            }
            int take = Math.min(cellEnergy, remaining);
            int updated = cellEnergy - take;
            if (updated > 0) {
                stack.set(ModDataComponents.STORED_ENERGY.get(), updated);
            } else {
                stack.remove(ModDataComponents.STORED_ENERGY.get());
            }
            remaining -= take;
        }
        robot.syncEnergyData();
    }

    public int chargeEnergy(RobotEntity robot, ItemStackHandler inventory, int amount) {
        if (amount <= 0 || robot.level().isClientSide) {
            return 0;
        }
        int remaining = amount;
        int accepted = 0;
        for (int i = RobotEntity.SLOT_CELL_START; i < RobotEntity.TOTAL_SLOTS && remaining > 0; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof EnergyCellBlockItem cellItem)) {
                continue;
            }
            int current = stack.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0);
            int cap = cellItem.getTier().getCapacity();
            int space = cap - current;
            if (space <= 0) {
                continue;
            }
            int add = Math.min(space, remaining);
            stack.set(ModDataComponents.STORED_ENERGY.get(), current + add);
            remaining -= add;
            accepted += add;
        }
        if (accepted > 0) {
            robot.syncEnergyData();
        }
        return accepted;
    }

    /**
     * Updates power-state tracking and returns 1 on wake, -1 on power-off, or 0 if unchanged.
     */
    public int updatePowerTransition(int stored, int max) {
        if (max > 0 && stored > max * 0.10D) {
            this.rechargeCancelled = false;
        }
        boolean nowPowered = stored > 0;
        boolean prevPowered = wasPowered;
        wasPowered = nowPowered;
        if (prevPowered && !nowPowered) {
            return -1;
        }
        if (!prevPowered && nowPowered) {
            return 1;
        }
        return 0;
    }

    public boolean isRechargeCancelled() {
        return rechargeCancelled;
    }

    public void setRechargeCancelled(boolean rechargeCancelled) {
        this.rechargeCancelled = rechargeCancelled;
    }

    @Nullable
    public BlockPos getChargerPos() {
        return chargerPos;
    }

    public void setChargerPos(@Nullable BlockPos chargerPos) {
        this.chargerPos = chargerPos;
    }

    public void save(CompoundTag compound) {
        if (chargerPos != null) {
            compound.putInt("ChargerX", chargerPos.getX());
            compound.putInt("ChargerY", chargerPos.getY());
            compound.putInt("ChargerZ", chargerPos.getZ());
        }
    }

    public void load(CompoundTag compound) {
        if (compound.contains("ChargerX") && compound.contains("ChargerY") && compound.contains("ChargerZ")) {
            this.chargerPos = new BlockPos(
                    compound.getInt("ChargerX"),
                    compound.getInt("ChargerY"),
                    compound.getInt("ChargerZ"));
        }
    }
}
