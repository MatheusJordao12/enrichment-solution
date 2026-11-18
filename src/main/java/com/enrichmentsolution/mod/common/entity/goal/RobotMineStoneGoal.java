package com.enrichmentsolution.mod.common.entity.goal;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RobotMineStoneGoal extends Goal {

    private static final int SEARCH_RADIUS = 12;
    private static final double MINE_RANGE_SQR = 9.0D; // 3.0 blocks from robot body center
    private static final int COLUMN_HEIGHT = 3;
    private static final int MAX_PATH_CANDIDATES = 16;
    private static final int STUCK_REROUTE_TICKS = 20;
    private static final int STUCK_SCAFFOLD_TICKS = 32;
    private static final int STUCK_ABANDON_TICKS = 55;
    private static final long BLACKLIST_TICKS = 120L;

    private final RobotEntity robot;
    private final Map<BlockPos, Long> blacklistedColumns = new HashMap<>();
    @Nullable
    private BlockPos targetPos;
    @Nullable
    private BlockPos standPos;
    private int breakTicks;
    private int searchCooldown;
    private int stuckTicks;
    private Vec3 lastProgressPos = Vec3.ZERO;

    public RobotMineStoneGoal(RobotEntity robot) {
        this.robot = robot;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!robot.isPowered() || robot.isTransforming() || !robot.hasPickaxe() || !robot.hasAnyStorageSlotFree()) {
            return false;
        }
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }
        searchCooldown = 15;
        return selectNextTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (!robot.isPowered() || robot.isTransforming() || targetPos == null || !robot.hasPickaxe() || !robot.hasAnyStorageSlotFree()) {
            return false;
        }
        return hasTargetStoneInColumn(targetPos);
    }

    @Override
    public void start() {
        breakTicks = 0;
        stuckTicks = 0;
        lastProgressPos = robot.position();
        moveToCurrentTarget();
    }

    @Override
    public void stop() {
        targetPos = null;
        standPos = null;
        breakTicks = 0;
        stuckTicks = 0;
        robot.setMining(false);
        robot.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPos == null || !(robot.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        List<BlockPos> columnTargets = getColumnTargets(targetPos);
        if (columnTargets.isEmpty()) {
            selectNextTarget();
            moveToCurrentTarget();
            return;
        }

        Vec3 lookTarget = Vec3.atCenterOf(columnTargets.get(0));
        robot.getLookControl().setLookAt(lookTarget.x, lookTarget.y, lookTarget.z, 30.0F, 30.0F);
        double bodyCenterY = robot.getY() + (robot.getBbHeight() * 0.5D);
        double dx = lookTarget.x - robot.getX();
        double dy = lookTarget.y - bodyCenterY;
        double dz = lookTarget.z - robot.getZ();
        double distSqr = dx * dx + dy * dy + dz * dz;

        if (distSqr <= MINE_RANGE_SQR) {
            robot.getNavigation().stop();
            robot.setMining(true);
            stuckTicks = 0;
            lastProgressPos = robot.position();
            breakTicks++;

            ItemStack pickaxe = robot.getPickaxe();
            int requiredTicks = 0;
            for (BlockPos pos : columnTargets) {
                BlockState blockState = serverLevel.getBlockState(pos);
                float speed = Math.max(1.0F, pickaxe.getDestroySpeed(blockState));
                requiredTicks += Math.max(10, Math.round(60.0F / speed));
            }

            if (breakTicks >= requiredTicks) {
                for (BlockPos pos : columnTargets) {
                    if (!robot.hasPickaxe()) {
                        break;
                    }
                    BlockState blockState = serverLevel.getBlockState(pos);
                    if (!isTargetStone(blockState)) {
                        continue;
                    }
                    ItemStack currentPickaxe = robot.getPickaxe();
                    List<ItemStack> drops = Block.getDrops(blockState, serverLevel, pos, null, robot, currentPickaxe);
                    if (serverLevel.destroyBlock(pos, false, robot)) {
                        for (ItemStack drop : drops) {
                            ItemStack leftover = robot.insertIntoStorage(drop);
                            if (!leftover.isEmpty()) {
                                Block.popResource(serverLevel, pos, leftover);
                            }
                        }
                        currentPickaxe.hurtAndBreak(1, serverLevel, null, item ->
                                robot.getInventory().setStackInSlot(RobotEntity.SLOT_PICKAXE, ItemStack.EMPTY));
                    }
                }
                breakTicks = 0;
                robot.setMining(false);
                if (selectNextTarget()) {
                    moveToCurrentTarget();
                }
            }
        } else {
            robot.setMining(false);
            breakTicks = 0;

            if (robot.position().distanceToSqr(lastProgressPos) > 0.04D) {
                lastProgressPos = robot.position();
                stuckTicks = 0;
            } else {
                stuckTicks++;
            }

            if (stuckTicks >= STUCK_ABANDON_TICKS) {
                blacklistTarget(targetPos);
                if (selectNextTarget()) {
                    moveToCurrentTarget();
                }
                return;
            }

            if (stuckTicks == STUCK_SCAFFOLD_TICKS) {
                if (tryRecoverWithScaffold()) {
                    stuckTicks = STUCK_REROUTE_TICKS;
                    moveToCurrentTarget();
                    return;
                }
            }

            if (robot.getNavigation().isDone() || stuckTicks == STUCK_REROUTE_TICKS) {
                BlockPos newStand = findBestStandPos(targetPos, true);
                if (newStand != null) {
                    standPos = newStand;
                    moveToCurrentTarget();
                } else if (robot.getNavigation().isDone() && stuckTicks >= STUCK_REROUTE_TICKS) {
                    if (!tryRecoverWithScaffold()) {
                        blacklistTarget(targetPos);
                        if (selectNextTarget()) {
                            moveToCurrentTarget();
                        }
                    }
                } else if (robot.getNavigation().isDone()) {
                    moveToCurrentTarget();
                }
            }
        }
    }

    private void moveToCurrentTarget() {
        if (targetPos == null) {
            return;
        }
        BlockPos dest = standPos != null ? standPos : targetPos;
        robot.getNavigation().moveTo(dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D, 1.0D);
    }

    private void blacklistTarget(BlockPos pos) {
        blacklistedColumns.put(pos, robot.level().getGameTime() + BLACKLIST_TICKS);
        targetPos = null;
        standPos = null;
        stuckTicks = 0;
    }

    private boolean selectNextTarget() {
        long now = robot.level().getGameTime();
        blacklistedColumns.entrySet().removeIf(entry -> entry.getValue() <= now);

        BlockPos origin = robot.blockPosition();
        Set<BlockPos> seenColumns = new HashSet<>();
        List<BlockPos> candidates = new ArrayList<>();

        for (int dy = -1; dy <= 3; dy++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (isTargetStone(robot.level().getBlockState(pos))) {
                        BlockPos base = toColumnBase(pos, origin.getY());
                        if (!blacklistedColumns.containsKey(base) && seenColumns.add(base)) {
                            candidates.add(base);
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            targetPos = null;
            standPos = null;
            return false;
        }

        candidates.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));

        int checked = 0;
        for (BlockPos candidate : candidates) {
            if (checked >= MAX_PATH_CANDIDATES) {
                break;
            }
            checked++;
            BlockPos reachableStand = findBestStandPos(candidate, true);
            if (reachableStand != null) {
                targetPos = candidate;
                standPos = reachableStand;
                stuckTicks = 0;
                return true;
            }
        }

        // Fallback: pick closest candidate with an adjacent standable spot so scaffold recovery can try
        for (BlockPos candidate : candidates) {
            BlockPos fallbackStand = findBestStandPos(candidate, false);
            if (fallbackStand != null) {
                targetPos = candidate;
                standPos = fallbackStand;
                stuckTicks = 0;
                return true;
            }
        }

        targetPos = candidates.get(0);
        standPos = null;
        stuckTicks = 0;
        return true;
    }

    @Nullable
    private BlockPos findBestStandPos(BlockPos columnBase, boolean requirePath) {
        Level level = robot.level();
        BlockPos robotPos = robot.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos candidate = columnBase.offset(dx, dy, dz);
                    if (!canRobotStandAt(level, candidate)) {
                        continue;
                    }
                    double dist = candidate.distSqr(robotPos);
                    if (dist >= bestDist) {
                        continue;
                    }
                    if (requirePath) {
                        Path path = robot.getNavigation().createPath(candidate, 0);
                        if (path == null || !path.canReach()) {
                            continue;
                        }
                    }
                    bestDist = dist;
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static boolean canRobotStandAt(Level level, BlockPos feetPos) {
        BlockState floor = level.getBlockState(feetPos.below());
        if (!floor.blocksMotion()) {
            return false;
        }
        for (int h = 0; h < COLUMN_HEIGHT; h++) {
            if (level.getBlockState(feetPos.above(h)).blocksMotion()) {
                return false;
            }
        }
        return true;
    }

    private boolean tryRecoverWithScaffold() {
        if (targetPos == null) {
            return false;
        }
        Level level = robot.level();
        BlockPos feet = robot.blockPosition();

        // 1. Pillar up if target is higher and headroom above robot is clear
        if (targetPos.getY() > feet.getY() && !level.getBlockState(feet.above(COLUMN_HEIGHT)).blocksMotion()) {
            robot.getJumpControl().jump();
            if (robot.tryPlaceScaffoldBlock(feet)) {
                return true;
            }
        }

        // 2. Bridge forward over a 1-block gap toward the target
        int stepX = Integer.compare(targetPos.getX(), feet.getX());
        int stepZ = Integer.compare(targetPos.getZ(), feet.getZ());
        if (stepX != 0 || stepZ != 0) {
            Direction primary = Math.abs(targetPos.getX() - feet.getX()) >= Math.abs(targetPos.getZ() - feet.getZ())
                    ? (stepX > 0 ? Direction.EAST : Direction.WEST)
                    : (stepZ > 0 ? Direction.SOUTH : Direction.NORTH);
            BlockPos front = feet.relative(primary);
            if (!level.getBlockState(front.below()).blocksMotion()
                    && !level.getBlockState(front).blocksMotion()
                    && !level.getBlockState(front.above(1)).blocksMotion()
                    && !level.getBlockState(front.above(2)).blocksMotion()) {
                if (robot.tryPlaceScaffoldBlock(front.below())) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<BlockPos> getColumnTargets(BlockPos basePos) {
        List<BlockPos> targets = new ArrayList<>(COLUMN_HEIGHT);
        for (int dy = 0; dy < COLUMN_HEIGHT; dy++) {
            BlockPos pos = basePos.above(dy);
            if (isTargetStone(robot.level().getBlockState(pos))) {
                targets.add(pos);
            }
        }
        return targets;
    }

    private boolean hasTargetStoneInColumn(BlockPos basePos) {
        for (int dy = 0; dy < COLUMN_HEIGHT; dy++) {
            if (isTargetStone(robot.level().getBlockState(basePos.above(dy)))) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos toColumnBase(BlockPos stonePos, int robotY) {
        int y = stonePos.getY();
        int baseY;
        if (y >= robotY && y < robotY + COLUMN_HEIGHT) {
            baseY = robotY;
        } else if (y >= robotY + COLUMN_HEIGHT) {
            baseY = y - (COLUMN_HEIGHT - 1);
        } else {
            baseY = y;
        }
        return new BlockPos(stonePos.getX(), baseY, stonePos.getZ());
    }

    private static boolean isTargetStone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLED_DEEPSLATE);
    }
}
