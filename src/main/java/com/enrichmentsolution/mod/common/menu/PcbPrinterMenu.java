package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.PcbPrinterBlockEntity;
import com.enrichmentsolution.mod.registry.ModBlocks;
import com.enrichmentsolution.mod.registry.ModItems;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class PcbPrinterMenu extends AbstractContainerMenu implements ISideConfigurableMenu {

    public static final int DATA_ENERGY = 0;
    public static final int DATA_MAX_ENERGY = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_MAX_PROGRESS = 3;
    public static final int DATA_PRINTING = 4;
    public static final int DATA_CAN_START = 5;
    public static final int DATA_SIDE_BASE = 6;
    public static final int DATA_COUNT = 12;

    public static final int BUTTON_START = 0;
    public static final int BUTTON_SIDE_BASE = 1;

    private final PcbPrinterBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess levelAccess;

    public PcbPrinterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public PcbPrinterMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.PCB_PRINTER.get(), containerId);
        this.blockEntity = blockEntity instanceof PcbPrinterBlockEntity printer ? printer : null;
        this.data = data;
        this.levelAccess = blockEntity != null && blockEntity.getLevel() != null
                ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                : ContainerLevelAccess.NULL;

        addDataSlots(data);

        IItemHandler handler = this.blockEntity != null
                ? this.blockEntity.getItemHandler(null)
                : new ItemStackHandler(PcbPrinterBlockEntity.TOTAL_SLOTS);

        // Input slots (unordered 3-ingredient recipes)
        addSlot(new SlotItemHandler(handler, PcbPrinterBlockEntity.SLOT_INPUT_0, 26, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidInput(stack);
            }
        });

        addSlot(new SlotItemHandler(handler, PcbPrinterBlockEntity.SLOT_INPUT_1, 48, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidInput(stack);
            }
        });

        addSlot(new SlotItemHandler(handler, PcbPrinterBlockEntity.SLOT_INPUT_2, 70, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidInput(stack);
            }
        });

        // Slot 3: Output Robot Circuit
        addSlot(new SlotItemHandler(handler, PcbPrinterBlockEntity.SLOT_OUTPUT, 118, 32) {
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
        if (id == BUTTON_START && blockEntity != null) {
            return blockEntity.startPrinting();
        }
        if (id >= BUTTON_SIDE_BASE && id < BUTTON_SIDE_BASE + RelativeSide.values().length && blockEntity != null) {
            blockEntity.cycleRelativeSideMode(id - BUTTON_SIDE_BASE);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int machineSlots = PcbPrinterBlockEntity.TOTAL_SLOTS;
        int playerStart = machineSlots;
        int playerEnd = playerStart + 36;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (isValidInput(stack)) {
                if (!moveItemStackTo(stack, 0, PcbPrinterBlockEntity.INPUT_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
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
        return stillValid(levelAccess, player, ModBlocks.PCB_PRINTER.get());
    }

    public int getEnergyStored() {
        return data.get(DATA_ENERGY);
    }

    public int getMaxEnergyStored() {
        return data.get(DATA_MAX_ENERGY);
    }

    public int getProgress() {
        return data.get(DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return data.get(DATA_MAX_PROGRESS);
    }

    public boolean isPrinting() {
        return data.get(DATA_PRINTING) != 0;
    }

    private static boolean isValidInput(ItemStack stack) {
        return stack.is(ModItems.STEEL_INGOT.get())
                || stack.is(ModItems.ALUMINIUM_INGOT.get())
                || stack.is(Items.REDSTONE)
                || stack.is(Items.IRON_INGOT)
                || stack.is(Items.GOLD_INGOT)
                || stack.is(Items.DIAMOND)
                || stack.is(Items.NETHERITE_INGOT)
                || stack.is(ModItems.BASIC_CIRCUIT.get())
                || stack.is(ModItems.ADVANCED_CIRCUIT.get())
                || stack.is(ModItems.ULTIMATE_CIRCUIT.get());
    }

    public boolean canStart() {
        return data.get(DATA_CAN_START) != 0;
    }
}
