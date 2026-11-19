package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.BeltDriveBlock;
import com.enrichmentsolution.mod.common.block.ConveyorBeltBlock;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Stores up to 50,000 FE (max 2,000 FE/t receive) and consumes 10 FE/t while
 * actively driving an adjacent {@link ConveyorBeltBlock} line.
 */
public class BeltDriveBlockEntity extends AbstractMachineBlockEntity {

    public static final int ENERGY_CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 2_000;
    public static final int MIN_ENERGY_TO_RUN = 20;
    public static final int ENERGY_COST_PER_TICK = 10;

    private int lastSyncedEnergy = -1;

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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
