package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.FluidPumpBlockEntity;
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

/**
 * Container menu for the Fluid Pump.
 * Syncs pump progress, 32-bit FE energy, output tank level, and fluid type.
 */
public class FluidPumpMenu extends AbstractContainerMenu implements ISideConfigurableMenu {

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_ENERGY_LOW = 2;
    public static final int DATA_ENERGY_HIGH = 3;
    public static final int DATA_MAX_ENERGY_LOW = 4;
    public static final int DATA_MAX_ENERGY_HIGH = 5;
    public static final int DATA_OUTPUT_FLUID_LOW = 6;
    public static final int DATA_OUTPUT_FLUID_HIGH = 7;
    public static final int DATA_OUTPUT_CAP_LOW = 8;
    public static final int DATA_OUTPUT_CAP_HIGH = 9;
    public static final int DATA_FLUID_TYPE = 10;
    public static final int DATA_SIDE_BASE = 11;
    public static final int DATA_COUNT = 17;

    public static final int BUTTON_SIDE_BASE = 100;

    public static final int FLUID_NONE = 0;
    public static final int FLUID_WATER = 1;
    public static final int FLUID_CRUDE_OIL = 2;

    private static final int PLAYER_INV_START = 0;
    private static final int PLAYER_INV_END = 36;

    private final FluidPumpBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public FluidPumpMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(DATA_COUNT));
    }

    public FluidPumpMenu(
            int containerId,
            Inventory playerInventory,
            FluidPumpBlockEntity blockEntity,
            ContainerData data
    ) {
        super(ModMenuTypes.FLUID_PUMP.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    private static FluidPumpBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof FluidPumpBlockEntity pump) {
            return pump;
        }
        throw new IllegalStateException("Expected FluidPumpBlockEntity at menu position");
    }

    public boolean isPumping() {
        return data.get(DATA_PROGRESS) > 0;
    }

    public int getScaledProgress(int indicatorWidth) {
        int progress = data.get(DATA_PROGRESS);
        int maxProgress = data.get(DATA_MAX_PROGRESS);
        return maxProgress != 0 && progress != 0 ? progress * indicatorWidth / maxProgress : 0;
    }

    public int getEnergyStored() {
        return combine32(DATA_ENERGY_LOW, DATA_ENERGY_HIGH);
    }

    public int getMaxEnergyStored() {
        return combine32(DATA_MAX_ENERGY_LOW, DATA_MAX_ENERGY_HIGH);
    }

    public int getScaledEnergy(int barHeight) {
        int energy = getEnergyStored();
        int maxEnergy = getMaxEnergyStored();
        return maxEnergy > 0 && energy > 0 ? energy * barHeight / maxEnergy : 0;
    }

    public int getOutputFluidAmount() {
        return combine32(DATA_OUTPUT_FLUID_LOW, DATA_OUTPUT_FLUID_HIGH);
    }

    public int getOutputFluidCapacity() {
        return combine32(DATA_OUTPUT_CAP_LOW, DATA_OUTPUT_CAP_HIGH);
    }

    public int getFluidType() {
        return data.get(DATA_FLUID_TYPE);
    }

    private int combine32(int lowIdx, int highIdx) {
        return (data.get(lowIdx) & 0xFFFF) | ((data.get(highIdx) & 0xFFFF) << 16);
    }

    @Override
    public SideConfigMode getRelativeSideMode(RelativeSide side) {
        return SideConfigMode.fromOrdinal(data.get(DATA_SIDE_BASE + side.ordinal()));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_SIDE_BASE && id < BUTTON_SIDE_BASE + RelativeSide.values().length) {
            if (!level.isClientSide) {
                blockEntity.cycleRelativeSideMode(id - BUTTON_SIDE_BASE);
            }
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index < PLAYER_INV_START + 27) {
            if (!moveItemStackTo(sourceStack, PLAYER_INV_START + 27, PLAYER_INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(sourceStack, PLAYER_INV_START, PLAYER_INV_START + 27, false)) {
            return ItemStack.EMPTY;
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
                ModBlocks.FLUID_PUMP.get()
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
