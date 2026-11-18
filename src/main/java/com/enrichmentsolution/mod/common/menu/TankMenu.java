package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.block.entity.TankBlockEntity;
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

public class TankMenu extends AbstractContainerMenu {

    public static final int DATA_FLUID_LOW = 0;
    public static final int DATA_FLUID_HIGH = 1;
    public static final int DATA_CAP_LOW = 2;
    public static final int DATA_CAP_HIGH = 3;
    public static final int DATA_TEMP_TENTHS = 4;
    public static final int DATA_MULT_HUNDREDTHS = 5;
    public static final int DATA_BLOCK_COUNT = 6;
    public static final int DATA_BONUS_FLAGS = 7;
    public static final int DATA_COUNT = 8;

    private final TankBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess levelAccess;

    public TankMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public TankMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.TANK.get(), containerId);
        this.blockEntity = blockEntity instanceof TankBlockEntity tank ? tank : null;
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
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved()
                && levelAccess.evaluate((level, pos) ->
                        player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 100.0D,
                        true);
    }

    public int getFluidAmount() {
        return (data.get(DATA_FLUID_HIGH) << 16) | (data.get(DATA_FLUID_LOW) & 0xFFFF);
    }

    public int getFluidCapacity() {
        return (data.get(DATA_CAP_HIGH) << 16) | (data.get(DATA_CAP_LOW) & 0xFFFF);
    }

    public double getTemperature() {
        return data.get(DATA_TEMP_TENTHS) / 10.0D;
    }

    public double getMultiplier() {
        return data.get(DATA_MULT_HUNDREDTHS) / 100.0D;
    }

    public int getBlockCount() {
        return data.get(DATA_BLOCK_COUNT);
    }

    public int getBonusFlags() {
        return data.get(DATA_BONUS_FLAGS);
    }
}
