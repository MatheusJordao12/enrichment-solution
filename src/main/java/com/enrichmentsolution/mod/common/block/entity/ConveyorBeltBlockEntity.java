package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.BeltPart;
import com.enrichmentsolution.mod.common.block.ConveyorBeltBlock;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

/**
 * Moves {@link ItemEntity} instances along {@link ConveyorBeltBlock#FACING} when active
 * and inserts items into an adjacent {@link IItemHandler} at {@link BeltPart#END} or {@link BeltPart#SINGLE}.
 */
public class ConveyorBeltBlockEntity extends BlockEntity {

    public static final double BELT_SPEED = 0.08D;
    private static final double CENTER_STRENGTH = 0.2D;
    private static final double MAX_CENTER_STEP = 0.05D;
    private static final String TAG_REVERSE = "Reverse";

    private boolean reverse = false;

    public ConveyorBeltBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CONVEYOR_BELT.get(), pos, blockState);
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        if (this.reverse != reverse) {
            this.reverse = reverse;
            setChanged();
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, ConveyorBeltBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.tickServer(level, pos, state);
        }
    }

    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(ConveyorBeltBlock.ACTIVE)) {
            return;
        }

        Direction facing = state.getValue(ConveyorBeltBlock.FACING);
        Direction moveDir = reverse ? facing.getOpposite() : facing;
        BeltPart part = state.getValue(ConveyorBeltBlock.PART);

        AABB topBox = new AABB(
                pos.getX(), pos.getY() + 0.75D, pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 1.35D, pos.getZ() + 1.0D
        );
        List<ItemEntity> items = level.getEntitiesOfClass(
                ItemEntity.class,
                topBox,
                item -> item.isAlive() && !item.getItem().isEmpty()
        );

        if (items.isEmpty()) {
            return;
        }

        IItemHandler targetHandler = null;
        boolean isOutputEnd = reverse
                ? (part == BeltPart.START || part == BeltPart.SINGLE)
                : (part == BeltPart.END || part == BeltPart.SINGLE);
        if (isOutputEnd) {
            targetHandler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    pos.relative(moveDir),
                    moveDir.getOpposite()
            );
        }

        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;

        for (ItemEntity item : items) {
            item.setExtendedLifetime();

            double dx = moveDir.getStepX() * BELT_SPEED;
            double dz = moveDir.getStepZ() * BELT_SPEED;

            if (moveDir.getAxis() == Direction.Axis.Z) {
                dx += Math.clamp((centerX - item.getX()) * CENTER_STRENGTH, -MAX_CENTER_STEP, MAX_CENTER_STEP);
            } else {
                dz += Math.clamp((centerZ - item.getZ()) * CENTER_STRENGTH, -MAX_CENTER_STEP, MAX_CENTER_STEP);
            }

            item.setDeltaMovement(dx, Math.min(item.getDeltaMovement().y, 0.0D), dz);
            item.hasImpulse = true;

            if (targetHandler != null) {
                double forwardProgress = (item.getX() - centerX) * moveDir.getStepX()
                        + (item.getZ() - centerZ) * moveDir.getStepZ();
                if (forwardProgress >= 0.0D) {
                    ItemStack remaining = ItemHandlerHelper.insertItemStacked(
                            targetHandler,
                            item.getItem().copy(),
                            false
                    );
                    if (remaining.isEmpty()) {
                        item.discard();
                    } else if (remaining.getCount() != item.getItem().getCount()) {
                        item.setItem(remaining);
                    }
                }
            }
        }
    }
}
