package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.EnergyCellBlock;
import com.enrichmentsolution.mod.common.block.EnergyCellTier;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.menu.EnergyCellMenu;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * BlockEntity for tiered Energy Cells with per-face I/O configuration,
 * automatic output pushing, and energy preservation on break.
 */
public class EnergyCellBlockEntity extends AbstractMachineBlockEntity implements MenuProvider {

    private static final String TAG_SIDE_MODES = "SideModes";

    private final EnergyCellTier tier;
    private final SideConfigMode[] sideModes = new SideConfigMode[Direction.values().length];
    private final Map<Direction, IEnergyStorage> sidedStorages = new EnumMap<>(Direction.class);

    public EnergyCellBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, resolveTier(blockState));
    }

    private EnergyCellBlockEntity(BlockPos pos, BlockState blockState, EnergyCellTier tier) {
        super(ModBlockEntities.ENERGY_CELL.get(), pos, blockState,
                tier.getCapacity(), tier.getTransferRate(), tier.getTransferRate(), 0);
        this.tier = tier;
        for (Direction dir : Direction.values()) {
            sideModes[dir.ordinal()] = dir == Direction.DOWN ? SideConfigMode.OUTPUT : SideConfigMode.INPUT;
            sidedStorages.put(dir, new SidedEnergyStorage(dir));
        }
    }

    private static EnergyCellTier resolveTier(BlockState state) {
        if (state.getBlock() instanceof EnergyCellBlock cellBlock) {
            return cellBlock.getTier();
        }
        return EnergyCellTier.BASIC;
    }

    public EnergyCellTier getTier() {
        return tier;
    }

    public SideConfigMode getSideMode(Direction direction) {
        return sideModes[direction.ordinal()];
    }

    public void cycleSideMode(int directionOrdinal) {
        if (directionOrdinal < 0 || directionOrdinal >= sideModes.length) {
            return;
        }
        sideModes[directionOrdinal] = sideModes[directionOrdinal].next();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        if (energyStorage.getEnergyStored() <= 0) {
            return;
        }
        int maxPush = tier.getTransferRate();
        for (Direction dir : Direction.values()) {
            if (sideModes[dir.ordinal()] != SideConfigMode.OUTPUT) {
                continue;
            }
            if (energyStorage.getEnergyStored() <= 0) {
                break;
            }
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null) {
                continue;
            }
            IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
            if (receiver != null && receiver.canReceive()) {
                int extractable = energyStorage.extractEnergy(maxPush, true);
                if (extractable > 0) {
                    int accepted = receiver.receiveEnergy(extractable, false);
                    if (accepted > 0) {
                        energyStorage.extractEnergy(accepted, false);
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side == null) {
            return energyStorage;
        }
        if (sideModes[side.ordinal()] == SideConfigMode.NONE) {
            return null;
        }
        return sidedStorages.get(side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int[] rawModes = new int[sideModes.length];
        for (int i = 0; i < sideModes.length; i++) {
            rawModes[i] = sideModes[i].ordinal();
        }
        tag.putIntArray(TAG_SIDE_MODES, rawModes);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_SIDE_MODES)) {
            int[] rawModes = tag.getIntArray(TAG_SIDE_MODES);
            for (int i = 0; i < Math.min(rawModes.length, sideModes.length); i++) {
                sideModes[i] = SideConfigMode.fromOrdinal(rawModes[i]);
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        int stored = energyStorage.getEnergyStored();
        if (stored > 0) {
            components.set(ModDataComponents.STORED_ENERGY.get(), stored);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        Integer stored = componentInput.get(ModDataComponents.STORED_ENERGY.get());
        if (stored != null && stored > 0) {
            energyStorage.setEnergy(stored);
        }
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case EnergyCellMenu.DATA_ENERGY_LOW -> energyStorage.getEnergyStored() & 0xFFFF;
                case EnergyCellMenu.DATA_ENERGY_HIGH -> (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case EnergyCellMenu.DATA_MAX_ENERGY_LOW -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case EnergyCellMenu.DATA_MAX_ENERGY_HIGH -> (energyStorage.getMaxEnergyStored() >>> 16) & 0xFFFF;
                default -> {
                    int sideIdx = index - EnergyCellMenu.DATA_SIDE_START;
                    if (sideIdx >= 0 && sideIdx < sideModes.length) {
                        yield sideModes[sideIdx].ordinal();
                    }
                    yield 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {
            int sideIdx = index - EnergyCellMenu.DATA_SIDE_START;
            if (sideIdx >= 0 && sideIdx < sideModes.length) {
                sideModes[sideIdx] = SideConfigMode.fromOrdinal(value);
            }
        }

        @Override
        public int getCount() {
            return EnergyCellMenu.DATA_COUNT;
        }
    };

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EnergyCellMenu(containerId, playerInventory, this, data);
    }

    private class SidedEnergyStorage implements IEnergyStorage {

        private final Direction side;

        private SidedEnergyStorage(Direction side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (!canReceive()) {
                return 0;
            }
            return energyStorage.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (!canExtract()) {
                return 0;
            }
            return energyStorage.extractEnergy(toExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return sideModes[side.ordinal()].canExtract();
        }

        @Override
        public boolean canReceive() {
            return sideModes[side.ordinal()].canReceive();
        }
    }
}
