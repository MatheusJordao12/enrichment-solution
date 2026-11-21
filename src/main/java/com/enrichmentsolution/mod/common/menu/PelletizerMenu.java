package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.block.entity.PelletizerControllerBlockEntity;
import com.enrichmentsolution.mod.registry.ModBlocks;
import com.enrichmentsolution.mod.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Container menu for the Pelletizer Multiblock Controller.
 * Syncs progress, 32-bit FE energy values, and multiblock validity via ContainerData.
 */
public class PelletizerMenu extends AbstractContainerMenu {

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_ENERGY_LOW = 2;
    public static final int DATA_ENERGY_HIGH = 3;
    public static final int DATA_MAX_ENERGY_LOW = 4;
    public static final int DATA_MAX_ENERGY_HIGH = 5;
    public static final int DATA_STRUCTURE_VALID = 6;
    public static final int DATA_COUNT = 7;

    private static final int MACHINE_SLOT_COUNT = 2;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final PelletizerControllerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public PelletizerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(DATA_COUNT));
    }

    public PelletizerMenu(
            int containerId,
            Inventory playerInventory,
            PelletizerControllerBlockEntity blockEntity,
            ContainerData data
    ) {
        super(ModMenuTypes.PELLETIZER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        IItemHandler handler = blockEntity.getItemHandler(null);
        if (handler != null) {
            this.addSlot(new SlotItemHandler(handler, PelletizerControllerBlockEntity.SLOT_INPUT, 56, 35));

            this.addSlot(new SlotItemHandler(handler, PelletizerControllerBlockEntity.SLOT_OUTPUT, 116, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    private static PelletizerControllerBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof PelletizerControllerBlockEntity controller) {
            return controller;
        }
        throw new IllegalStateException("Expected PelletizerControllerBlockEntity at menu position");
    }

    public int getEnergy() {
        return (data.get(DATA_ENERGY_LOW) & 0xFFFF) | ((data.get(DATA_ENERGY_HIGH) & 0xFFFF) << 16);
    }

    public int getMaxEnergy() {
        return (data.get(DATA_MAX_ENERGY_LOW) & 0xFFFF) | ((data.get(DATA_MAX_ENERGY_HIGH) & 0xFFFF) << 16);
    }

    public int getProgress() {
        return data.get(DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return data.get(DATA_MAX_PROGRESS);
    }

    public boolean isStructureValid() {
        return data.get(DATA_STRUCTURE_VALID) != 0;
    }

    public int getScaledProgress(int arrowWidth) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        return maxProgress != 0 && progress != 0 ? progress * arrowWidth / maxProgress : 0;
    }

    public int getScaledEnergy(int barHeight) {
        int energy = getEnergy();
        int maxEnergy = getMaxEnergy();
        return maxEnergy > 0 && energy > 0 ? energy * barHeight / maxEnergy : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(sourceStack, 0, 1, false)) {
                if (index < PLAYER_INV_START + 27) {
                    if (!moveItemStackTo(sourceStack, PLAYER_INV_START + 27, PLAYER_INV_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(sourceStack, PLAYER_INV_START, PLAYER_INV_START + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(player, sourceStack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player,
                ModBlocks.PELLETIZER_CONTROLLER.get()
        );
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }
}
