package com.enrichmentsolution.mod.common.menu;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.GeothermalGeneratorBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Container menu for the Geothermal Generator / Turbine.
 * Syncs 32-bit FE energy, input/output Gaseous Isobutane tank levels, FE/t generation, and mB/t flow rate.
 */
public class GeothermalGeneratorMenu extends AbstractContainerMenu implements ISideConfigurableMenu {

    public static final int DATA_ENERGY_LOW = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_MAX_ENERGY_LOW = 2;
    public static final int DATA_MAX_ENERGY_HIGH = 3;
    public static final int DATA_INPUT_GAS = 4;
    public static final int DATA_OUTPUT_GAS = 5;
    public static final int DATA_MAX_TANK = 6;
    public static final int DATA_GENERATION = 7;
    public static final int DATA_FLOW_RATE = 8;
    public static final int DATA_SIDE_START = 9;
    public static final int DATA_COUNT = DATA_SIDE_START + 6;
    public static final int BUTTON_SIDE_BASE = 0;

    private final GeothermalGeneratorBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess levelAccess;

    public GeothermalGeneratorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT));
    }

    public GeothermalGeneratorMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.GEOTHERMAL_GENERATOR.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.blockEntity = blockEntity instanceof GeothermalGeneratorBlockEntity gen ? gen : null;
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
    public SideConfigMode getRelativeSideMode(RelativeSide side) {
        return SideConfigMode.fromOrdinal(data.get(DATA_SIDE_START + side.ordinal()));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_SIDE_BASE && id < BUTTON_SIDE_BASE + 6) {
            if (blockEntity != null) {
                blockEntity.cycleRelativeSideMode(id - BUTTON_SIDE_BASE);
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
        return stillValid(levelAccess, player, ModBlocks.GEOTHERMAL_GENERATOR.get());
    }

    public GeothermalGeneratorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getEnergyStored() {
        return (data.get(DATA_ENERGY_HIGH) << 16) | (data.get(DATA_ENERGY_LOW) & 0xFFFF);
    }

    public int getMaxEnergyStored() {
        return (data.get(DATA_MAX_ENERGY_HIGH) << 16) | (data.get(DATA_MAX_ENERGY_LOW) & 0xFFFF);
    }

    public int getInputGas() {
        return data.get(DATA_INPUT_GAS);
    }

    public int getOutputGas() {
        return data.get(DATA_OUTPUT_GAS);
    }

    public int getMaxTank() {
        return data.get(DATA_MAX_TANK);
    }

    public int getGeneration() {
        return data.get(DATA_GENERATION);
    }

    public int getFlowRate() {
        return data.get(DATA_FLOW_RATE);
    }

    public boolean isGenerating() {
        return getFlowRate() > 0;
    }

    public int getScaledEnergy(int barHeight) {
        int energy = getEnergyStored();
        int maxEnergy = getMaxEnergyStored();
        return maxEnergy > 0 && energy > 0 ? Math.min(barHeight, (int) Math.ceil((double) energy / maxEnergy * barHeight)) : 0;
    }

    public int getScaledInputGas(int tankHeight) {
        int gas = getInputGas();
        int max = getMaxTank();
        return max > 0 && gas > 0 ? Math.min(tankHeight, (int) Math.ceil((double) gas / max * tankHeight)) : 0;
    }

    public int getScaledOutputGas(int tankHeight) {
        int gas = getOutputGas();
        int max = getMaxTank();
        return max > 0 && gas > 0 ? Math.min(tankHeight, (int) Math.ceil((double) gas / max * tankHeight)) : 0;
    }
}
