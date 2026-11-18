package com.enrichmentsolution.mod.common.entity.goal;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RobotPickupItemGoal extends Goal {

    private static final double SEARCH_RADIUS = 10.0D;
    private static final double PICKUP_DIST_SQR = 2.6D;
    private static final int STUCK_TIMEOUT_TICKS = 40;
    private static final long BLACKLIST_TICKS = 100L;

    private final RobotEntity robot;
    private final Map<UUID, Long> blacklistedItems = new HashMap<>();
    @Nullable
    private ItemEntity targetItem;
    private Vec3 lastProgressPos = Vec3.ZERO;
    private int stuckTicks;
    private int searchCooldown;

    public RobotPickupItemGoal(RobotEntity robot) {
        this.robot = robot;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!robot.isPowered() || robot.isTransforming() || robot.hasSword()) {
            return false;
        }
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }
        searchCooldown = 10;
        targetItem = findNearestItem();
        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        return robot.isPowered()
                && !robot.isTransforming()
                && !robot.hasSword()
                && targetItem != null
                && targetItem.isAlive()
                && !targetItem.getItem().isEmpty()
                && robot.hasStorageSpaceFor(targetItem.getItem())
                && robot.distanceToSqr(targetItem) <= (SEARCH_RADIUS + 2.0D) * (SEARCH_RADIUS + 2.0D);
    }

    @Override
    public void start() {
        robot.setMining(false);
        stuckTicks = 0;
        lastProgressPos = robot.position();
        if (targetItem != null) {
            robot.getNavigation().moveTo(targetItem, 1.05D);
        }
    }

    @Override
    public void stop() {
        targetItem = null;
        stuckTicks = 0;
        robot.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetItem == null || !targetItem.isAlive()) {
            return;
        }

        robot.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);
        double distSqr = robot.distanceToSqr(targetItem);

        if (distSqr <= PICKUP_DIST_SQR) {
            robot.getNavigation().stop();
            ItemStack stack = targetItem.getItem();
            ItemStack leftover = robot.insertIntoStorage(stack);
            if (leftover.getCount() < stack.getCount()) {
                robot.triggerPickupAnimation();
                if (leftover.isEmpty()) {
                    targetItem.discard();
                } else {
                    targetItem.setItem(leftover);
                }
            }
            targetItem = null;
            return;
        }

        if (robot.position().distanceToSqr(lastProgressPos) > 0.04D) {
            lastProgressPos = robot.position();
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }

        if (stuckTicks >= STUCK_TIMEOUT_TICKS) {
            blacklistedItems.put(targetItem.getUUID(), robot.level().getGameTime() + BLACKLIST_TICKS);
            targetItem = null;
            return;
        }

        if (robot.getNavigation().isDone()) {
            Path path = robot.getNavigation().createPath(targetItem, 0);
            if (path == null || !path.canReach()) {
                blacklistedItems.put(targetItem.getUUID(), robot.level().getGameTime() + BLACKLIST_TICKS);
                targetItem = null;
                return;
            }
            robot.getNavigation().moveTo(path, 1.05D);
        }
    }

    @Nullable
    private ItemEntity findNearestItem() {
        long now = robot.level().getGameTime();
        blacklistedItems.entrySet().removeIf(entry -> entry.getValue() <= now);

        AABB box = robot.getBoundingBox().inflate(SEARCH_RADIUS, 4.0D, SEARCH_RADIUS);
        List<ItemEntity> items = robot.level().getEntitiesOfClass(
                ItemEntity.class,
                box,
                item -> item.isAlive()
                        && !item.getItem().isEmpty()
                        && !blacklistedItems.containsKey(item.getUUID())
                        && robot.hasStorageSpaceFor(item.getItem())
        );
        items.sort(Comparator.comparingDouble(robot::distanceToSqr));

        for (ItemEntity item : items) {
            Path path = robot.getNavigation().createPath(item, 0);
            if (path != null && path.canReach()) {
                return item;
            }
        }
        return null;
    }
}
