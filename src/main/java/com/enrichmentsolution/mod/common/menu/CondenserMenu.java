package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.CondenserBlockEntity;
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
 * Container menu for the Condenser machine.
 * Syncs gas input, cooling water, liquid output, and active condense rate via ContainerData.
 */
public class CondenserMenu extends AbstractContainerMenu implements ISideConfigurableMenu {

    public static final int DATA_GAS_AMOUNT = 0;
    public static final int DATA_GAS_CAPACITY = 1;
    public static final int DATA_WATER_AMOUNT = 2;
    public static final int DATA_WATER_CAPACITY = 3;
    public static final int DATA_LIQUID_AMOUNT = 4;
    public static final int DATA_LIQUID_CAPACITY = 5;
    public static final int DATA_CONDENSE_RATE = 6;
    public static final int DATA_SIDE_START = 7;
    public static final int DATA_COUNT = DATA_SIDE_START + 6;
    public static final int BUTTON_SIDE_BASE = 0;

    private final CondenserBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public CondenserMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(DATA_COUNT));
    }

    public CondenserMenu(
            int containerId,
            Inventory playerInventory,
            CondenserBlockEntity blockEntity,
            ContainerData data
    ) {
        super(ModMenuTypes.CONDENSER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    private static CondenserBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof CondenserBlockEntity condenser) {
            return condenser;
        }
        throw new IllegalStateException("Expected CondenserBlockEntity at menu position");
    }

    public int getGasAmount() {
        return data.get(DATA_GAS_AMOUNT);
    }

    public int getGasCapacity() {
        return data.get(DATA_GAS_CAPACITY);
    }

    public int getWaterAmount() {
        return data.get(DATA_WATER_AMOUNT);
    }

    public int getWaterCapacity() {
        return data.get(DATA_WATER_CAPACITY);
    }

    public int getLiquidAmount() {
        return data.get(DATA_LIQUID_AMOUNT);
    }

    public int getLiquidCapacity() {
        return data.get(DATA_LIQUID_CAPACITY);
    }

    public int getCondenseRate() {
        return data.get(DATA_CONDENSE_RATE);
    }

    public boolean isCondensing() {
        return data.get(DATA_CONDENSE_RATE) > 0;
    }

    @Override
    public SideConfigMode getRelativeSideMode(RelativeSide side) {
        return SideConfigMode.fromOrdinal(data.get(DATA_SIDE_START + side.ordinal()));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_SIDE_BASE && id < BUTTON_SIDE_BASE + 6) {
            blockEntity.cycleRelativeSideMode(id - BUTTON_SIDE_BASE);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.CONDENSER.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }
}
