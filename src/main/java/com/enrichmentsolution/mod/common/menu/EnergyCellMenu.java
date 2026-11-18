package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.EnergyCellBlockEntity;
import com.enrichmentsolution.mod.registry.ModMenuTypes;
import net.minecraft.core.Direction;
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

public class EnergyCellMenu extends AbstractContainerMenu {

    public static final int DATA_ENERGY_LOW = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_MAX_ENERGY_LOW = 2;
    public static final int DATA_MAX_ENERGY_HIGH = 3;
    public static final int DATA_SIDE_START = 4;
    public static final int DATA_COUNT = DATA_SIDE_START + 6;

    private final EnergyCellBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess levelAccess;

    public EnergyCellMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public EnergyCellMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.ENERGY_CELL.get(), containerId);
        this.blockEntity = blockEntity instanceof EnergyCellBlockEntity cell ? cell : null;
        this.data = data;
        this.levelAccess = blockEntity != null && blockEntity.getLevel() != null
                ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
                : ContainerLevelAccess.NULL;

        addDataSlots(data);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity != null && id >= 0 && id < Direction.values().length) {
            blockEntity.cycleSideMode(id);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved()
                && levelAccess.evaluate((level, pos) ->
                        player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
                        true);
    }

    public int getEnergyStored() {
        return (data.get(DATA_ENERGY_HIGH) << 16) | (data.get(DATA_ENERGY_LOW) & 0xFFFF);
    }

    public int getMaxEnergyStored() {
        return (data.get(DATA_MAX_ENERGY_HIGH) << 16) | (data.get(DATA_MAX_ENERGY_LOW) & 0xFFFF);
    }

    public SideConfigMode getSideMode(Direction direction) {
        return SideConfigMode.fromOrdinal(data.get(DATA_SIDE_START + direction.ordinal()));
    }
}
