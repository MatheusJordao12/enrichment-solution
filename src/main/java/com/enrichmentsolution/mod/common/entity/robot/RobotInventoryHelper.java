package com.enrichmentsolution.mod.common.entity.robot;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import com.enrichmentsolution.mod.common.item.EnergyCellBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Handles Robot inventory validation, tool queries, storage insertion,
 * scaffold block placement, and death drops.
 */
public class RobotInventoryHelper {

    private boolean wasHoldingSword = false;

    public ItemStackHandler createInventory(RobotEntity robot) {
        return new ItemStackHandler(RobotEntity.TOTAL_SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == RobotEntity.SLOT_PICKAXE) {
                    return isValidTool(stack);
                }
                if (slot >= RobotEntity.SLOT_CELL_START && slot < RobotEntity.TOTAL_SLOTS) {
                    return stack.getItem() instanceof EnergyCellBlockItem;
                }
                return true;
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                if (!robot.level().isClientSide) {
                    if (slot == RobotEntity.SLOT_PICKAXE) {
                        boolean nowHasSword = getStackInSlot(RobotEntity.SLOT_PICKAXE).is(ItemTags.SWORDS);
                        if (robot.isPowered()) {
                            if (nowHasSword && !wasHoldingSword) {
                                robot.setDetransformTicks(0);
                                robot.setTransformTicks(RobotEntity.TRANSFORM_DURATION);
                            } else if (!nowHasSword && wasHoldingSword) {
                                robot.setTransformTicks(0);
                                robot.setDetransformTicks(RobotEntity.TRANSFORM_DURATION);
                            }
                        }
                        wasHoldingSword = nowHasSword;
                        robot.syncMainHandPickaxe();
                    } else if (slot >= RobotEntity.SLOT_CELL_START && slot < RobotEntity.TOTAL_SLOTS) {
                        robot.syncEnergyData();
                    }
                }
            }
        };
    }

    public boolean isWasHoldingSword() {
        return wasHoldingSword;
    }

    public void setWasHoldingSword(boolean wasHoldingSword) {
        this.wasHoldingSword = wasHoldingSword;
    }

    public static boolean isValidTool(ItemStack stack) {
        return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SWORDS);
    }

    public static ItemStack getTool(ItemStackHandler inventory) {
        ItemStack stack = inventory.getStackInSlot(RobotEntity.SLOT_PICKAXE);
        return isValidTool(stack) ? stack : ItemStack.EMPTY;
    }

    public static ItemStack getPickaxe(ItemStackHandler inventory) {
        ItemStack stack = inventory.getStackInSlot(RobotEntity.SLOT_PICKAXE);
        return stack.is(ItemTags.PICKAXES) ? stack : ItemStack.EMPTY;
    }

    public static boolean hasStorageSpaceFor(ItemStackHandler inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack remaining = stack.copy();
        for (int i = RobotEntity.SLOT_STORAGE_START; i < RobotEntity.TOTAL_SLOTS; i++) {
            remaining = inventory.insertItem(i, remaining, true);
            if (remaining.isEmpty()) {
                return true;
            }
        }
        return remaining.getCount() < stack.getCount();
    }

    public static boolean hasAnyStorageSlotFree(ItemStackHandler inventory) {
        for (int i = RobotEntity.SLOT_STORAGE_START; i < RobotEntity.TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack insertIntoStorage(ItemStackHandler inventory, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = RobotEntity.SLOT_STORAGE_START; i < RobotEntity.TOTAL_SLOTS; i++) {
            remaining = inventory.insertItem(i, remaining, false);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }

    public static boolean tryPlaceScaffoldBlock(RobotEntity robot, ItemStackHandler inventory, BlockPos pos) {
        if (!(robot.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockState existing = serverLevel.getBlockState(pos);
        if (!existing.canBeReplaced()) {
            return false;
        }
        int slot = findScaffoldSlot(robot, inventory);
        if (slot < 0) {
            return false;
        }
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        BlockState placeState = blockItem.getBlock().defaultBlockState();
        if (serverLevel.setBlock(pos, placeState, Block.UPDATE_ALL)) {
            inventory.extractItem(slot, 1, false);
            SoundType soundType = placeState.getSoundType(serverLevel, pos, robot);
            serverLevel.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
            return true;
        }
        return false;
    }

    private static int findScaffoldSlot(RobotEntity robot, ItemStackHandler inventory) {
        int fallback = -1;
        for (int i = RobotEntity.SLOT_STORAGE_START; i < RobotEntity.TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            Block block = blockItem.getBlock();
            if (block instanceof BaseEntityBlock || block instanceof FallingBlock) {
                continue;
            }
            BlockState state = block.defaultBlockState();
            if (!state.isCollisionShapeFullBlock(robot.level(), robot.blockPosition())) {
                continue;
            }
            if (block == Blocks.COBBLESTONE || block == Blocks.COBBLED_DEEPSLATE
                    || block == Blocks.DIRT || block == Blocks.STONE) {
                return i;
            }
            if (fallback < 0) {
                fallback = i;
            }
        }
        return fallback;
    }

    public static void dropAllItems(ServerLevel level, RobotEntity robot, ItemStackHandler inventory) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, robot.getX(), robot.getY(), robot.getZ(), stack.copy());
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public void deserializeInventory(RobotEntity robot, ItemStackHandler inventory, CompoundTag tag) {
        inventory.deserializeNBT(robot.registryAccess(), tag);
        if (inventory.getSlots() != RobotEntity.TOTAL_SLOTS) {
            ItemStack[] preserved = new ItemStack[Math.min(inventory.getSlots(), RobotEntity.TOTAL_SLOTS)];
            for (int i = 0; i < preserved.length; i++) {
                preserved[i] = inventory.getStackInSlot(i);
            }
            inventory.setSize(RobotEntity.TOTAL_SLOTS);
            for (int i = 0; i < preserved.length; i++) {
                inventory.setStackInSlot(i, preserved[i]);
            }
        }
        this.wasHoldingSword = inventory.getStackInSlot(RobotEntity.SLOT_PICKAXE).is(ItemTags.SWORDS);
    }
}
