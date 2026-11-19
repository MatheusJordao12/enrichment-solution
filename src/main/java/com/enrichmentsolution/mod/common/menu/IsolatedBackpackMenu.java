package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.item.IsolatedBackpackItem;
import com.enrichmentsolution.mod.registry.ModMenuTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * 3x9 chest-style menu for {@link IsolatedBackpackItem}, backed by {@link DataComponents#CONTAINER}.
 * Locks the player slot holding the open backpack and prevents nesting backpacks inside themselves.
 */
public class IsolatedBackpackMenu extends AbstractContainerMenu {

    public static final int SLOTS = 27;

    private final Inventory playerInventory;
    private final ItemStack backpackStack;
    private final int lockedSlotIndex;
    private final SimpleContainer container;

    public IsolatedBackpackMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readVarInt());
    }

    private IsolatedBackpackMenu(int containerId, Inventory playerInventory, int lockedSlotIndex) {
        this(containerId, playerInventory, safeGetStack(playerInventory, lockedSlotIndex), lockedSlotIndex);
    }

    public IsolatedBackpackMenu(int containerId, Inventory playerInventory, ItemStack backpackStack, int lockedSlotIndex) {
        super(ModMenuTypes.ISOLATED_BACKPACK.get(), containerId);
        this.playerInventory = playerInventory;
        this.backpackStack = backpackStack;
        this.lockedSlotIndex = lockedSlotIndex;
        this.container = new SimpleContainer(SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (!playerInventory.player.level().isClientSide) {
                    saveToBackpack();
                }
            }
        };

        loadFromBackpack();

        // 3x9 backpack inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !(stack.getItem() instanceof IsolatedBackpackItem);
                    }
                });
            }
        }

        // Player inventory (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9;
                addSlot(createPlayerSlot(playerInventory, slotIndex, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar (1x9)
        for (int col = 0; col < 9; col++) {
            addSlot(createPlayerSlot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private Slot createPlayerSlot(Inventory inv, int index, int x, int y) {
        if (index == lockedSlotIndex) {
            return new Slot(inv, index, x, y) {
                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            };
        }
        return new Slot(inv, index, x, y);
    }

    private static ItemStack safeGetStack(Inventory inv, int slot) {
        if (slot >= 0 && slot < inv.getContainerSize()) {
            return inv.getItem(slot);
        }
        return ItemStack.EMPTY;
    }

    private void loadFromBackpack() {
        if (backpackStack.isEmpty()) {
            return;
        }
        ItemContainerContents contents = backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        contents.copyInto(items);
        for (int i = 0; i < SLOTS; i++) {
            container.setItem(i, items.get(i));
        }
    }

    private void saveToBackpack() {
        ItemStack target = lockedSlotIndex >= 0 && lockedSlotIndex < playerInventory.getContainerSize()
                ? playerInventory.getItem(lockedSlotIndex)
                : backpackStack;
        if (target.isEmpty() || !(target.getItem() instanceof IsolatedBackpackItem)) {
            target = backpackStack;
        }
        if (target.isEmpty() || !(target.getItem() instanceof IsolatedBackpackItem)) {
            return;
        }
        List<ItemStack> items = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            items.add(container.getItem(i).copy());
        }
        target.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        playerInventory.setChanged();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP && button == lockedSlotIndex) {
            return;
        }
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);
            if (slot.container == playerInventory && slot.getContainerSlot() == lockedSlotIndex) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (slot.container == playerInventory && slot.getContainerSlot() == lockedSlotIndex) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, SLOTS + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stack.getItem() instanceof IsolatedBackpackItem || !moveItemStackTo(stack, 0, SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (lockedSlotIndex < 0 || lockedSlotIndex >= player.getInventory().getContainerSize()) {
            return false;
        }
        ItemStack current = player.getInventory().getItem(lockedSlotIndex);
        return current.getItem() instanceof IsolatedBackpackItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            saveToBackpack();
        }
    }
}
