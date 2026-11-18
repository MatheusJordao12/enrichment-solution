package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import com.enrichmentsolution.mod.common.item.EnergyCellBlockItem;
import com.enrichmentsolution.mod.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class RobotMenu extends AbstractContainerMenu {

    private final RobotEntity robot;

    public RobotMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveRobot(playerInventory, extraData.readInt()));
    }

    public RobotMenu(int containerId, Inventory playerInventory, RobotEntity robot) {
        super(ModMenuTypes.ROBOT.get(), containerId);
        this.robot = robot;

        ItemStackHandler handler = robot != null ? robot.getInventory() : new ItemStackHandler(RobotEntity.TOTAL_SLOTS);

        // Slot 0: Tool slot (pickaxe or sword)
        addSlot(new SlotItemHandler(handler, RobotEntity.SLOT_PICKAXE, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return RobotEntity.isValidTool(stack);
            }
        });

        // Slots 1..9: 3x3 storage
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = RobotEntity.SLOT_STORAGE_START + row * 3 + col;
                addSlot(new SlotItemHandler(handler, index, 62 + col * 18, 17 + row * 18));
            }
        }

        // Slots 10..12: 3 Energy Cell slots
        for (int row = 0; row < RobotEntity.CELL_SLOTS; row++) {
            int index = RobotEntity.SLOT_CELL_START + row;
            addSlot(new SlotItemHandler(handler, index, 124, 17 + row * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof EnergyCellBlockItem;
                }
            });
        }

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

    private static RobotEntity resolveRobot(Inventory playerInventory, int entityId) {
        Entity entity = playerInventory.player.level().getEntity(entityId);
        return entity instanceof RobotEntity r ? r : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int robotSlots = RobotEntity.TOTAL_SLOTS;
        int playerStart = robotSlots;
        int playerEnd = playerStart + 36;

        int storageEnd = RobotEntity.SLOT_STORAGE_START + RobotEntity.STORAGE_SLOTS;
        int cellStart = RobotEntity.SLOT_CELL_START;
        int cellEnd = cellStart + RobotEntity.CELL_SLOTS;

        if (index < robotSlots) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (RobotEntity.isValidTool(stack)) {
                if (!moveItemStackTo(stack, RobotEntity.SLOT_PICKAXE, RobotEntity.SLOT_PICKAXE + 1, false)) {
                    if (!moveItemStackTo(stack, RobotEntity.SLOT_STORAGE_START, storageEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (stack.getItem() instanceof EnergyCellBlockItem) {
                if (!moveItemStackTo(stack, cellStart, cellEnd, false)) {
                    if (!moveItemStackTo(stack, RobotEntity.SLOT_STORAGE_START, storageEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!moveItemStackTo(stack, RobotEntity.SLOT_STORAGE_START, storageEnd, false)) {
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
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && robot != null && robot.isOwner(player)) {
            robot.setFollowOwner(!robot.isFollowOwner());
            return true;
        }
        return false;
    }

    public boolean isFollowOwner() {
        return robot != null && robot.isFollowOwner();
    }

    public int getEnergyStored() {
        return robot != null ? robot.getEnergyStored() : 0;
    }

    public int getMaxEnergyStored() {
        return robot != null ? robot.getMaxEnergyStored() : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return robot != null && robot.isAlive() && robot.distanceToSqr(player) <= 64.0D && robot.isOwner(player);
    }
}
