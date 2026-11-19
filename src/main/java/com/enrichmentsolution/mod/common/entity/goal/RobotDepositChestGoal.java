package com.enrichmentsolution.mod.common.entity.goal;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * When the robot's storage has no empty slot and it is linked to a chest (IItemHandler),
 * navigates to the chest, deposits its storage items, and then allows mining or recharging to resume.
 */
public class RobotDepositChestGoal extends Goal {

    private static final double DEPOSIT_DIST_SQR = 9.0D; // 3.0 blocks
    private static final int RETRY_COOLDOWN_TICKS = 60;

    private final RobotEntity robot;
    private int repathCooldown;
    private int retryCooldown;
    private int stuckTicks;
    private double lastX;
    private double lastY;
    private double lastZ;

    public RobotDepositChestGoal(RobotEntity robot) {
        this.robot = robot;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (retryCooldown > 0) {
            retryCooldown--;
            return false;
        }
        if (!robot.isPowered() || robot.isTransforming() || robot.isRecharging()) {
            return false;
        }
        if (!robot.hasLinkedChest() || robot.hasEmptyStorageSlot()) {
            return false;
        }
        BlockPos chestPos = robot.getLinkedChestPos();
        if (chestPos == null) {
            return false;
        }
        return getChestHandler(robot.level(), chestPos) != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!robot.isPowered() || robot.isTransforming() || robot.isRecharging()) {
            return false;
        }
        if (!robot.hasLinkedChest() || robot.hasEmptyStorageSlot()) {
            return false;
        }
        if (retryCooldown > 0) {
            return false;
        }
        BlockPos chestPos = robot.getLinkedChestPos();
        return chestPos != null && getChestHandler(robot.level(), chestPos) != null;
    }

    @Override
    public void start() {
        robot.setMining(false);
        repathCooldown = 0;
        stuckTicks = 0;
        lastX = robot.getX();
        lastY = robot.getY();
        lastZ = robot.getZ();
    }

    @Override
    public void stop() {
        robot.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(robot.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos chestPos = robot.getLinkedChestPos();
        if (chestPos == null) {
            return;
        }

        double cx = chestPos.getX() + 0.5D;
        double cy = chestPos.getY() + 0.5D;
        double cz = chestPos.getZ() + 0.5D;
        robot.getLookControl().setLookAt(cx, cy, cz, 30.0F, 30.0F);

        double distSqr = robot.distanceToSqr(cx, chestPos.getY(), cz);
        if (distSqr <= DEPOSIT_DIST_SQR) {
            robot.getNavigation().stop();
            IItemHandler chestHandler = getChestHandler(serverLevel, chestPos);
            if (chestHandler != null) {
                depositItems(chestHandler);
                robot.triggerPickupAnimation();
            }
            if (!robot.hasEmptyStorageSlot()) {
                retryCooldown = RETRY_COOLDOWN_TICKS;
            }
            return;
        }

        double movedSqr = (robot.getX() - lastX) * (robot.getX() - lastX)
                + (robot.getY() - lastY) * (robot.getY() - lastY)
                + (robot.getZ() - lastZ) * (robot.getZ() - lastZ);
        if (movedSqr > 0.04D) {
            lastX = robot.getX();
            lastY = robot.getY();
            lastZ = robot.getZ();
            stuckTicks = 0;
        } else {
            stuckTicks++;
            if (stuckTicks >= 60) {
                retryCooldown = RETRY_COOLDOWN_TICKS;
                return;
            }
        }

        if (--repathCooldown <= 0 || robot.getNavigation().isDone()) {
            repathCooldown = 15;
            robot.getNavigation().moveTo(cx, chestPos.getY(), cz, 1.1D);
        }
    }

    private void depositItems(IItemHandler chestHandler) {
        int storageEnd = RobotEntity.SLOT_STORAGE_START + robot.getStorageCapacity();
        for (int i = RobotEntity.SLOT_STORAGE_START; i < storageEnd; i++) {
            ItemStack stack = robot.getInventory().getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(chestHandler, stack.copy(), false);
            robot.getInventory().setStackInSlot(i, remainder);
        }
    }

    @Nullable
    private static IItemHandler getChestHandler(Level level, BlockPos pos) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler != null) {
            return handler;
        }
        for (Direction dir : Direction.values()) {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }
}
