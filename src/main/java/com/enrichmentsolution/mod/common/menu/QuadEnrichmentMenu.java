package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.EnrichmentChamberBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class QuadEnrichmentMenu extends AbstractContainerMenu implements ISideConfigurableMenu {

    public static final int DATA_PROGRESS_0 = 0;
    public static final int DATA_PROGRESS_1 = 1;
    public static final int DATA_PROGRESS_2 = 2;
    public static final int DATA_PROGRESS_3 = 3;
    public static final int DATA_MAX_PROGRESS = 4;
    public static final int DATA_ENERGY_LOW = 5;
    public static final int DATA_ENERGY_HIGH = 6;
    public static final int DATA_MAX_ENERGY_LOW = 7;
    public static final int DATA_MAX_ENERGY_HIGH = 8;
    public static final int DATA_AUTO_SORT = 9;
    public static final int DATA_LIT = 10;
    public static final int DATA_SIDE_BASE = 11;
    public static final int DATA_COUNT = 17;

    public static final int BUTTON_TOGGLE_AUTO_SORT = 0;
    public static final int BUTTON_SIDE_BASE = 1;

    private final EnrichmentChamberBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess levelAccess;

    public QuadEnrichmentMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public QuadEnrichmentMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.QUAD_ENRICHMENT_CHAMBER.get(), containerId);
        this.blockEntity = blockEntity instanceof EnrichmentChamberBlockEntity chamber ? chamber : null;
        this.data = data;
        this.levelAccess = blockEntity != null
                ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                : ContainerLevelAccess.NULL;

        addDataSlots(data);

        IItemHandler itemHandler = this.blockEntity != null
                ? this.blockEntity.getItemHandler(null)
                : new ItemStackHandler(8);

        // 4 input slots (indices 0..3)
        for (int i = 0; i < 4; i++) {
            final int slotIdx = i;
            addSlot(new SlotItemHandler(itemHandler, slotIdx, 35 + i * 22, 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModItems.RAW_URANIUM.get());
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    if (QuadEnrichmentMenu.this.blockEntity != null && QuadEnrichmentMenu.this.blockEntity.isAutoSort()) {
                        QuadEnrichmentMenu.this.blockEntity.distributeInputsEvenly();
                    }
                }
            });
        }

        // 4 output slots (indices 4..7)
        for (int i = 0; i < 4; i++) {
            addSlot(new SlotItemHandler(itemHandler, 4 + i, 35 + i * 22, 52) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // Player inventory (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Hotbar (1x9)
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
        if (id == BUTTON_TOGGLE_AUTO_SORT && blockEntity != null) {
            blockEntity.toggleAutoSort();
            return true;
        }
        if (id >= BUTTON_SIDE_BASE && id < BUTTON_SIDE_BASE + RelativeSide.values().length && blockEntity != null) {
            blockEntity.cycleRelativeSideMode(id - BUTTON_SIDE_BASE);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < 8) {
            if (!moveItemStackTo(stack, 8, 44, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stack.is(ModItems.RAW_URANIUM.get())) {
                if (!moveItemStackTo(stack, 0, 4, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 35) {
                if (!moveItemStackTo(stack, 35, 44, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 8, 35, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);

        if (blockEntity != null && blockEntity.isAutoSort()) {
            blockEntity.distributeInputsEvenly();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.ENRICHMENT_CHAMBER.get());
    }

    public int getProgress(int channel) {
        return data.get(DATA_PROGRESS_0 + channel);
    }

    public int getMaxProgress() {
        return Math.max(1, data.get(DATA_MAX_PROGRESS));
    }

    public int getEnergyStored() {
        return (data.get(DATA_ENERGY_HIGH) << 16) | (data.get(DATA_ENERGY_LOW) & 0xFFFF);
    }

    public int getMaxEnergyStored() {
        return (data.get(DATA_MAX_ENERGY_HIGH) << 16) | (data.get(DATA_MAX_ENERGY_LOW) & 0xFFFF);
    }

    public boolean isAutoSort() {
        return data.get(DATA_AUTO_SORT) != 0;
    }

    public boolean isLit() {
        return data.get(DATA_LIT) != 0;
    }
}
