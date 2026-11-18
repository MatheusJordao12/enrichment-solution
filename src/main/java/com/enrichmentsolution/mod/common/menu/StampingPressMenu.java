package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.StampingPressBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class StampingPressMenu extends AbstractContainerMenu implements ISideConfigurableMenu {

    public static final int DATA_ENERGY_LOW = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_MAX_ENERGY_LOW = 2;
    public static final int DATA_MAX_ENERGY_HIGH = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_MAX_PROGRESS = 5;
    public static final int DATA_SIDE_BASE = 6;
    public static final int DATA_COUNT = 12;

    public static final int BUTTON_SIDE_BASE = 0;

    private final StampingPressBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess levelAccess;

    public StampingPressMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public StampingPressMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.STAMPING_PRESS.get(), containerId);
        this.blockEntity = blockEntity instanceof StampingPressBlockEntity press ? press : null;
        this.data = data;
        this.levelAccess = blockEntity != null && blockEntity.getLevel() != null
                ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                : ContainerLevelAccess.NULL;

        addDataSlots(data);

        IItemHandler handler = this.blockEntity != null
                ? this.blockEntity.getItemHandler(null)
                : new ItemStackHandler(StampingPressBlockEntity.TOTAL_SLOTS);

        // 4 input slots (2x2 grid)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                addSlot(new SlotItemHandler(handler, row * 2 + col, 34 + col * 18, 24 + row * 18));
            }
        }

        // Output slot
        addSlot(new SlotItemHandler(handler, StampingPressBlockEntity.SLOT_OUTPUT, 112, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player inventory (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar (1x9)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public SideConfigMode getRelativeSideMode(RelativeSide side) {
        return SideConfigMode.fromOrdinal(data.get(DATA_SIDE_BASE + side.ordinal()));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_SIDE_BASE && id < BUTTON_SIDE_BASE + RelativeSide.values().length && blockEntity != null) {
            blockEntity.cycleRelativeSideMode(id - BUTTON_SIDE_BASE);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int machineSlots = StampingPressBlockEntity.TOTAL_SLOTS;
        int playerStart = machineSlots;
        int playerEnd = playerStart + 36;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, 0, StampingPressBlockEntity.INPUT_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.STAMPING_PRESS.get());
    }

    public int getEnergyStored() {
        return (data.get(DATA_ENERGY_HIGH) << 16) | (data.get(DATA_ENERGY_LOW) & 0xFFFF);
    }

    public int getMaxEnergyStored() {
        return (data.get(DATA_MAX_ENERGY_HIGH) << 16) | (data.get(DATA_MAX_ENERGY_LOW) & 0xFFFF);
    }

    public int getProgress() {
        return data.get(DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return data.get(DATA_MAX_PROGRESS);
    }
}
