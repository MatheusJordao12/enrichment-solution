package com.enrichmentsolution.mod.common.block.entity.base;

import com.enrichmentsolution.mod.common.energy.ModEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Base BlockEntity for all machines and reactor controllers in Enrichment Solution.
 * Provides unified Forge Energy (FE) storage, item inventory handling, and NBT persistence.
 */
public abstract class AbstractMachineBlockEntity extends BlockEntity {

    protected final ModEnergyStorage energyStorage;
    protected final ItemStackHandler itemHandler;

    protected AbstractMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState blockState,
            int energyCapacity,
            int maxReceive,
            int maxExtract,
            int inventorySlots
    ) {
        super(type, pos, blockState);
        this.energyStorage = new ModEnergyStorage(energyCapacity, maxReceive, maxExtract, this::setChanged);
        this.itemHandler = new ItemStackHandler(inventorySlots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    /**
     * Server-side tick entry point wired by machine blocks.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractMachineBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.tickServer(level, pos, state);
        }
    }

    protected abstract void tickServer(Level level, BlockPos pos, BlockState state);

    @Nullable
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energyStorage;
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    public void dropContents(Level level, BlockPos pos) {
        SimpleContainer container = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            container.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, container);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.put("Inventory", itemHandler.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energyStorage.setEnergy(tag.getInt("Energy"));
        }
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }
}
