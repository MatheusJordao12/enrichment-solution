package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import com.enrichmentsolution.mod.common.item.EnergyCellBlockItem;
import com.enrichmentsolution.mod.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.Predicate;

public class RobotMenu extends AbstractContainerMenu {

    public static final int OFFSCREEN = -1000;
    public static final int VISIBLE_EXPANDED_ROWS = 6;
    public static final int EXPANDED_Y_OFFSET = 54;

    private final RobotEntity robot;

    public RobotMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveRobot(playerInventory, extraData.readInt()));
    }

    public RobotMenu(int containerId, Inventory playerInventory, RobotEntity robot) {
        super(ModMenuTypes.ROBOT.get(), containerId);
        this.robot = robot;

        ItemStackHandler handler = robot != null ? robot.getInventory() : new ItemStackHandler(RobotEntity.TOTAL_SLOTS);

        // Slot 0: Tool slot (pickaxe or sword)
        addSlot(new DynamicSlotItemHandler(handler, RobotEntity.SLOT_PICKAXE, 26, 35,
                RobotEntity::isValidTool));

        // Slots 1..3: 3 Energy Cell slots (placed offscreen until Battery tab is opened)
        for (int row = 0; row < RobotEntity.CELL_SLOTS; row++) {
            int index = RobotEntity.SLOT_CELL_START + row;
            addSlot(new DynamicSlotItemHandler(handler, index, OFFSCREEN, OFFSCREEN,
                    stack -> stack.getItem() instanceof EnergyCellBlockItem));
        }

        // Slots 4..84: Up to 81 storage slots (Tier 0 shows 3x3 at (62, 17); higher tiers use expanded view)
        int initialTier = getSlotUpgradeTier();
        for (int i = 0; i < RobotEntity.MAX_STORAGE_SLOTS; i++) {
            int index = RobotEntity.SLOT_STORAGE_START + i;
            int sx = OFFSCREEN;
            int sy = OFFSCREEN;
            if (initialTier == 0 && i < 9) {
                sx = 62 + (i % 3) * 18;
                sy = 17 + (i / 3) * 18;
            }
            final int storageIndex = i;
            addSlot(new DynamicSlotItemHandler(handler, index, sx, sy,
                    stack -> storageIndex < getStorageCapacity()));
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

    /**
     * Updates client-side slot coordinates for the current view (main tab, battery tab, or expanded storage).
     */
    public void updateSlotPositions(boolean expandedStorageOpen, boolean batteryTabOpen, int scrollRow, int guiWidth) {
        // Tool slot (0)
        if (!expandedStorageOpen) {
            moveSlot(RobotEntity.SLOT_PICKAXE, 26, 35, true);
        } else {
            moveSlot(RobotEntity.SLOT_PICKAXE, OFFSCREEN, OFFSCREEN, false);
        }

        // Energy cell slots (1..3)
        for (int row = 0; row < RobotEntity.CELL_SLOTS; row++) {
            int slotIndex = RobotEntity.SLOT_CELL_START + row;
            if (!expandedStorageOpen && batteryTabOpen) {
                moveSlot(slotIndex, guiWidth + 30, 17 + row * 18, true);
            } else {
                moveSlot(slotIndex, OFFSCREEN, OFFSCREEN, false);
            }
        }

        // Storage slots (4..84)
        int tier = getSlotUpgradeTier();
        int capacity = getStorageCapacity();
        for (int i = 0; i < RobotEntity.MAX_STORAGE_SLOTS; i++) {
            int slotIndex = RobotEntity.SLOT_STORAGE_START + i;
            if (!expandedStorageOpen) {
                if (tier == 0 && i < 9) {
                    moveSlot(slotIndex, 62 + (i % 3) * 18, 17 + (i / 3) * 18, true);
                } else {
                    moveSlot(slotIndex, OFFSCREEN, OFFSCREEN, false);
                }
            } else {
                int row = i / 9;
                int col = i % 9;
                if (i < capacity && row >= scrollRow && row < scrollRow + VISIBLE_EXPANDED_ROWS) {
                    int visRow = row - scrollRow;
                    moveSlot(slotIndex, 8 + col * 18, 17 + visRow * 18, true);
                } else {
                    moveSlot(slotIndex, OFFSCREEN, OFFSCREEN, false);
                }
            }
        }

        // Player inventory & hotbar
        int yOffset = (expandedStorageOpen && tier >= 2) ? EXPANDED_Y_OFFSET : 0;
        int playerBase = RobotEntity.TOTAL_SLOTS;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                moveSlot(playerBase + row * 9 + col, 8 + col * 18, 84 + row * 18 + yOffset, true);
            }
        }
        for (int col = 0; col < 9; col++) {
            moveSlot(playerBase + 27 + col, 8 + col * 18, 142 + yOffset, true);
        }
    }

    private void moveSlot(int menuSlotIndex, int newX, int newY, boolean active) {
        Slot rawSlot = this.slots.get(menuSlotIndex);
        if (rawSlot instanceof DynamicSlotItemHandler oldSlot) {
            if (oldSlot.x == newX && oldSlot.y == newY && oldSlot.isActive() == active) {
                return;
            }
            DynamicSlotItemHandler newSlot = oldSlot.withPosition(newX, newY, active);
            this.slots.set(menuSlotIndex, newSlot);
            return;
        }
        if (rawSlot.x == newX && rawSlot.y == newY) {
            return;
        }
        Slot newSlot = new Slot(rawSlot.container, rawSlot.getContainerSlot(), newX, newY);
        newSlot.index = rawSlot.index;
        this.slots.set(menuSlotIndex, newSlot);
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

        int storageStart = RobotEntity.SLOT_STORAGE_START;
        int storageEnd = storageStart + getStorageCapacity();
        int cellStart = RobotEntity.SLOT_CELL_START;
        int cellEnd = cellStart + RobotEntity.CELL_SLOTS;

        if (index < robotSlots) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (RobotEntity.isValidTool(stack)) {
                if (!moveItemStackTo(stack, RobotEntity.SLOT_PICKAXE, RobotEntity.SLOT_PICKAXE + 1, false)) {
                    if (!moveItemStackTo(stack, storageStart, storageEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (stack.getItem() instanceof EnergyCellBlockItem) {
                if (!moveItemStackTo(stack, cellStart, cellEnd, false)) {
                    if (!moveItemStackTo(stack, storageStart, storageEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!moveItemStackTo(stack, storageStart, storageEnd, false)) {
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

    public int getRobotEntityId() {
        return robot != null ? robot.getId() : -1;
    }

    public ItemStack getToolStack() {
        return robot != null ? robot.getInventory().getStackInSlot(RobotEntity.SLOT_PICKAXE) : ItemStack.EMPTY;
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

    public int getSlotUpgradeTier() {
        return robot != null ? robot.getSlotUpgradeTier() : 0;
    }

    public int getStorageCapacity() {
        return robot != null ? robot.getStorageCapacity() : 9;
    }

    public int getMiningTargetY() {
        return robot != null ? robot.getMiningTargetY() : 0;
    }

    public boolean isDigChunks() {
        return robot != null && robot.isDigChunks();
    }

    public int getChunkRadius() {
        return robot != null ? robot.getChunkRadius() : 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return robot != null && robot.isAlive() && robot.distanceToSqr(player) <= 64.0D && robot.isOwner(player);
    }

    /**
     * SlotItemHandler that hides itself from client rendering/hovering when moved offscreen.
     */
    private static class DynamicSlotItemHandler extends SlotItemHandler {
        private final Predicate<ItemStack> mayPlacePredicate;
        private final boolean active;

        DynamicSlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition,
                Predicate<ItemStack> mayPlacePredicate) {
            this(itemHandler, index, xPosition, yPosition, xPosition > -500 && yPosition > -500, mayPlacePredicate);
        }

        DynamicSlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition,
                boolean active, Predicate<ItemStack> mayPlacePredicate) {
            super(itemHandler, index, xPosition, yPosition);
            this.active = active;
            this.mayPlacePredicate = mayPlacePredicate;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return mayPlacePredicate.test(stack);
        }

        @Override
        public boolean isActive() {
            return active && this.x > -500 && this.y > -500;
        }

        DynamicSlotItemHandler withPosition(int newX, int newY, boolean newActive) {
            DynamicSlotItemHandler moved = new DynamicSlotItemHandler(
                    getItemHandler(), getSlotIndex(), newX, newY, newActive, mayPlacePredicate);
            ((Slot) moved).index = ((Slot) this).index;
            return moved;
        }
    }
}
