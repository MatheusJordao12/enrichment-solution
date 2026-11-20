package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.BeltPart;
import com.enrichmentsolution.mod.common.block.ConveyorBeltBlock;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holds and smoothly transports a single {@link ItemStack} flat on top of the conveyor belt
 * (1/16 block per tick = 1 pixel/tick) with seamless handoff to the next belt or adjacent inventory.
 */
public class ConveyorBeltBlockEntity extends BlockEntity {

    /** 1/16 block per tick so item motion matches the 1px/tick belt animation 1:1. */
    public static final float PROGRESS_PER_TICK = 0.0625F;
    private static final String TAG_REVERSE = "Reverse";
    private static final String TAG_ITEM = "Item";
    private static final String TAG_PROGRESS = "Progress";

    private boolean reverse = false;
    private ItemStack storedItem = ItemStack.EMPTY;
    private float progress = 0.0F;
    private float prevProgress = 0.0F;

    public ConveyorBeltBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CONVEYOR_BELT.get(), pos, blockState);
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        if (this.reverse != reverse) {
            this.reverse = reverse;
            setChangedAndSync();
        }
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public float getProgress() {
        return progress;
    }

    public float getPrevProgress() {
        return prevProgress;
    }

    public boolean canAcceptItem() {
        return storedItem.isEmpty();
    }

    public boolean acceptItem(ItemStack stack) {
        if (!storedItem.isEmpty() || stack.isEmpty()) {
            return false;
        }
        this.storedItem = stack.copy();
        this.progress = 0.0F;
        this.prevProgress = 0.0F;
        setChangedAndSync();
        return true;
    }

    public void dropStoredItem(Level level, BlockPos pos) {
        if (!storedItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, storedItem);
            storedItem = ItemStack.EMPTY;
            progress = 0.0F;
            prevProgress = 0.0F;
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(TAG_REVERSE, reverse);
        tag.putFloat(TAG_PROGRESS, progress);
        if (!storedItem.isEmpty()) {
            tag.put(TAG_ITEM, storedItem.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        reverse = tag.getBoolean(TAG_REVERSE);
        progress = tag.getFloat(TAG_PROGRESS);
        prevProgress = progress;
        if (tag.contains(TAG_ITEM)) {
            storedItem = ItemStack.parseOptional(registries, tag.getCompound(TAG_ITEM));
        } else {
            storedItem = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ConveyorBeltBlockEntity blockEntity) {
        blockEntity.prevProgress = blockEntity.progress;
        if (state.getValue(ConveyorBeltBlock.ACTIVE) && !blockEntity.storedItem.isEmpty()) {
            blockEntity.progress = Math.min(1.0F, blockEntity.progress + PROGRESS_PER_TICK);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ConveyorBeltBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.tickServer(level, pos, state);
        }
    }

    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        if (storedItem.isEmpty()) {
            AABB topBox = new AABB(
                    pos.getX(), pos.getY() + 0.75D, pos.getZ(),
                    pos.getX() + 1.0D, pos.getY() + 1.35D, pos.getZ() + 1.0D
            );
            List<ItemEntity> items = level.getEntitiesOfClass(
                    ItemEntity.class,
                    topBox,
                    item -> item.isAlive() && !item.getItem().isEmpty()
            );
            if (!items.isEmpty()) {
                ItemEntity first = items.get(0);
                storedItem = first.getItem().copy();
                progress = 0.0F;
                prevProgress = 0.0F;
                first.discard();
                setChangedAndSync();
            }
        }

        if (!state.getValue(ConveyorBeltBlock.ACTIVE) || storedItem.isEmpty()) {
            return;
        }

        prevProgress = progress;
        progress = Math.min(1.0F, progress + PROGRESS_PER_TICK);
        if (progress < 1.0F) {
            return;
        }

        Direction facing = state.getValue(ConveyorBeltBlock.FACING);
        Direction moveDir = reverse ? facing.getOpposite() : facing;
        BlockPos nextPos = pos.relative(moveDir);

        // 1. Seamless handoff to the next conveyor belt
        if (level.getBlockEntity(nextPos) instanceof ConveyorBeltBlockEntity nextBelt) {
            if (nextBelt.acceptItem(storedItem)) {
                storedItem = ItemStack.EMPTY;
                progress = 0.0F;
                prevProgress = 0.0F;
                setChangedAndSync();
            }
            return;
        }

        // 2. Insert into adjacent inventory at the end of the line
        IItemHandler targetHandler = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                nextPos,
                moveDir.getOpposite()
        );
        if (targetHandler != null) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(targetHandler, storedItem, false);
            if (remainder.isEmpty()) {
                storedItem = ItemStack.EMPTY;
                progress = 0.0F;
                prevProgress = 0.0F;
                setChangedAndSync();
            } else if (remainder.getCount() != storedItem.getCount()) {
                storedItem = remainder;
                setChangedAndSync();
            }
            return;
        }

        // 3. No belt or inventory ahead: drop cleanly off the end
        BeltPart part = state.getValue(ConveyorBeltBlock.PART);
        boolean isOutputEnd = reverse
                ? (part == BeltPart.START || part == BeltPart.SINGLE)
                : (part == BeltPart.END || part == BeltPart.SINGLE);
        if (isOutputEnd) {
            double dropX = pos.getX() + 0.5D + moveDir.getStepX() * 0.65D;
            double dropY = pos.getY() + 0.85D;
            double dropZ = pos.getZ() + 0.5D + moveDir.getStepZ() * 0.65D;
            ItemEntity dropped = new ItemEntity(level, dropX, dropY, dropZ, storedItem);
            dropped.setDeltaMovement(moveDir.getStepX() * 0.08D, 0.02D, moveDir.getStepZ() * 0.08D);
            level.addFreshEntity(dropped);
            storedItem = ItemStack.EMPTY;
            progress = 0.0F;
            prevProgress = 0.0F;
            setChangedAndSync();
        }
    }
}
