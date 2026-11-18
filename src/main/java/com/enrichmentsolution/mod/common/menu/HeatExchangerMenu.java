package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.HeatExchangerBlockEntity;
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
 * Container menu for the Heat Exchanger machine.
 * Syncs water, liquid isobutane, gaseous isobutane levels, temperature, multiplier, and active boil rate.
 */
public class HeatExchangerMenu extends AbstractContainerMenu implements ISideConfigurableMenu {

    public static final int DATA_WATER_AMOUNT_LOW = 0;
    public static final int DATA_WATER_AMOUNT_HIGH = 1;
    public static final int DATA_WATER_CAP_LOW = 2;
    public static final int DATA_WATER_CAP_HIGH = 3;
    public static final int DATA_LIQUID_AMOUNT_LOW = 4;
    public static final int DATA_LIQUID_AMOUNT_HIGH = 5;
    public static final int DATA_LIQUID_CAP_LOW = 6;
    public static final int DATA_LIQUID_CAP_HIGH = 7;
    public static final int DATA_GAS_AMOUNT_LOW = 8;
    public static final int DATA_GAS_AMOUNT_HIGH = 9;
    public static final int DATA_GAS_CAP_LOW = 10;
    public static final int DATA_GAS_CAP_HIGH = 11;
    public static final int DATA_TEMP_TENTHS = 12;
    public static final int DATA_MULT_HUNDREDTHS = 13;
    public static final int DATA_BOIL_RATE_LOW = 14;
    public static final int DATA_BOIL_RATE_HIGH = 15;
    public static final int DATA_BLOCK_COUNT = 16;
    public static final int DATA_SIDE_START = 17;
    public static final int DATA_WATER_TEMP_TENTHS = DATA_SIDE_START + 6;
    public static final int DATA_WATER_MULT_HUNDREDTHS = DATA_SIDE_START + 7;
    public static final int DATA_COUNT = DATA_SIDE_START + 8;
    public static final int BUTTON_SIDE_BASE = 0;

    private final HeatExchangerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public HeatExchangerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData), new SimpleContainerData(DATA_COUNT));
    }

    public HeatExchangerMenu(
            int containerId,
            Inventory playerInventory,
            HeatExchangerBlockEntity blockEntity,
            ContainerData data
    ) {
        super(ModMenuTypes.HEAT_EXCHANGER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    private static HeatExchangerBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof HeatExchangerBlockEntity exchanger) {
            return exchanger;
        }
        throw new IllegalStateException("Expected HeatExchangerBlockEntity at menu position");
    }

    private int getInt32(int lowIndex, int highIndex) {
        return (data.get(highIndex) << 16) | (data.get(lowIndex) & 0xFFFF);
    }

    public int getWaterAmount() {
        return getInt32(DATA_WATER_AMOUNT_LOW, DATA_WATER_AMOUNT_HIGH);
    }

    public int getWaterCapacity() {
        return getInt32(DATA_WATER_CAP_LOW, DATA_WATER_CAP_HIGH);
    }

    public int getLiquidAmount() {
        return getInt32(DATA_LIQUID_AMOUNT_LOW, DATA_LIQUID_AMOUNT_HIGH);
    }

    public int getLiquidCapacity() {
        return getInt32(DATA_LIQUID_CAP_LOW, DATA_LIQUID_CAP_HIGH);
    }

    public int getGasAmount() {
        return getInt32(DATA_GAS_AMOUNT_LOW, DATA_GAS_AMOUNT_HIGH);
    }

    public int getGasCapacity() {
        return getInt32(DATA_GAS_CAP_LOW, DATA_GAS_CAP_HIGH);
    }

    public double getTemperature() {
        return (data.get(DATA_TEMP_TENTHS) & 0xFFFF) / 10.0D;
    }

    public double getMultiplier() {
        return (data.get(DATA_MULT_HUNDREDTHS) & 0xFFFF) / 100.0D;
    }

    /**
     * Temperature (Celsius) stored on the water currently inside the water tank.
     * Unlike {@link #getTemperature()} this is not affected by the adjacent tank or by
     * the incoming piped water, it reflects what the water in the tank actually reached.
     */
    public double getWaterTemperature() {
        return (data.get(DATA_WATER_TEMP_TENTHS) & 0xFFFF) / 10.0D;
    }

    public double getWaterMultiplier() {
        return (data.get(DATA_WATER_MULT_HUNDREDTHS) & 0xFFFF) / 100.0D;
    }

    public int getBoilRate() {
        return getInt32(DATA_BOIL_RATE_LOW, DATA_BOIL_RATE_HIGH);
    }

    public int getBlockCount() {
        return Math.max(1, data.get(DATA_BLOCK_COUNT));
    }

    public boolean isBoiling() {
        return getBoilRate() > 0;
    }

    @Override
    public SideConfigMode getRelativeSideMode(RelativeSide side) {
        return SideConfigMode.fromOrdinal(data.get(DATA_SIDE_START + side.ordinal()));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_SIDE_BASE && id < BUTTON_SIDE_BASE + 6) {
            HeatExchangerBlockEntity master = blockEntity.getMasterEntity();
            if (master != null) {
                master.cycleRelativeSideMode(id - BUTTON_SIDE_BASE);
            }
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
        return !blockEntity.isRemoved()
                && ContainerLevelAccess.create(level, blockEntity.getBlockPos()).evaluate((lvl, pos) ->
                        lvl.getBlockState(pos).is(ModBlocks.HEAT_EXCHANGER.get())
                                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 1024.0D,
                        true);
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
