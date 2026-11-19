package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.BeltDriveBlock;
import com.enrichmentsolution.mod.common.block.ConveyorBeltBlock;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.menu.BeltDriveMenu;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Stores up to 50,000 FE (max 2,000 FE/t receive) and consumes 10 FE/t while
 * actively driving an adjacent {@link ConveyorBeltBlock} line.
 */
public class BeltDriveBlockEntity extends AbstractMachineBlockEntity implements MenuProvider {

    public static final int ENERGY_CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_000;
    public static final int MIN_ENERGY_TO_RUN = 20;
    public static final int ENERGY_COST_PER_TICK = 10;

    private static final String TAG_REVERSE = "Reverse";

    private int lastSyncedEnergy = -1;
    private boolean reverse = false;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case BeltDriveMenu.DATA_ENERGY_LOW -> energyStorage.getEnergyStored() & 0xFFFF;
                case BeltDriveMenu.DATA_ENERGY_HIGH -> (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case BeltDriveMenu.DATA_MAX_ENERGY_LOW -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case BeltDriveMenu.DATA_MAX_ENERGY_HIGH -> (energyStorage.getMaxEnergyStored() >>> 16) & 0xFFFF;
                case BeltDriveMenu.DATA_ACTIVE -> {
                    BlockState state = getBlockState();
                    yield (state.hasProperty(BeltDriveBlock.ACTIVE) && state.getValue(BeltDriveBlock.ACTIVE)) ? 1 : 0;
                }
                case BeltDriveMenu.DATA_REVERSE -> reverse ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == BeltDriveMenu.DATA_REVERSE) {
                reverse = value != 0;
            }
        }

        @Override
        public int getCount() {
            return BeltDriveMenu.DATA_COUNT;
        }
    };

    public BeltDriveBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.BELT_DRIVE.get(),
                pos,
                blockState,
                ENERGY_CAPACITY,
                MAX_RECEIVE,
                0,
                0
        );
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        if (this.reverse == reverse) {
            return;
        }
        this.reverse = reverse;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = worldPosition.relative(dir);
                if (level.getBlockState(neighborPos).getBlock() instanceof ConveyorBeltBlock) {
                    ConveyorBeltBlock.updateConnectedLine(level, neighborPos);
                }
            }
        }
    }

    public void toggleReverse() {
        setReverse(!this.reverse);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BeltDriveMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        boolean hasAdjacentBelt = false;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(dir)).getBlock() instanceof ConveyorBeltBlock) {
                hasAdjacentBelt = true;
                break;
            }
        }

        boolean shouldBeActive = hasAdjacentBelt && energyStorage.getEnergyStored() >= MIN_ENERGY_TO_RUN;
        if (shouldBeActive) {
            energyStorage.consumeInternal(ENERGY_COST_PER_TICK, false);
        }

        boolean wasActive = state.getValue(BeltDriveBlock.ACTIVE);
        if (wasActive != shouldBeActive) {
            level.setBlock(pos, state.setValue(BeltDriveBlock.ACTIVE, shouldBeActive), Block.UPDATE_ALL);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = pos.relative(dir);
                if (level.getBlockState(neighborPos).getBlock() instanceof ConveyorBeltBlock) {
                    ConveyorBeltBlock.updateConnectedLine(level, neighborPos);
                }
            }
        }

        int currentEnergy = energyStorage.getEnergyStored();
        if (currentEnergy != lastSyncedEnergy) {
            lastSyncedEnergy = currentEnergy;
            level.sendBlockUpdated(pos, state, level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(TAG_REVERSE, reverse);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        reverse = tag.getBoolean(TAG_REVERSE);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putBoolean(TAG_REVERSE, reverse);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("Energy")) {
            energyStorage.setEnergy(tag.getInt("Energy"));
        }
        reverse = tag.getBoolean(TAG_REVERSE);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
