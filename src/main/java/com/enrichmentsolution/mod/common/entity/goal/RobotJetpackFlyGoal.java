package com.enrichmentsolution.mod.common.entity.goal;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Flies horizontally at 5 blocks/s (0.25 blocks/tick) with the jetpack when:
 * - In Stay mode and a hostile target inside the 3x3 chunk area is > 10 blocks away, or
 * - In Follow mode and the owner (or combat target) is > 10 blocks away.
 * Consumes 1,200 FE per 20 blocks flown (60 FE/block).
 */
public class RobotJetpackFlyGoal extends Goal {

    private static final double START_FLY_DIST_SQR = 100.0D; // > 10 blocks
    private static final double LAND_HORIZ_DIST_SQR = 12.25D; // 3.5 blocks
    private static final double FLY_SPEED = 0.35D; // 7 blocks per second (20 ticks/s)
    private static final int FE_PER_BLOCK = 60; // 1200 FE per 20 blocks

    private final RobotEntity robot;
    @Nullable
    private LivingEntity flyTarget;
    private boolean stayBounded;
    private Vec3 lastFlyPos = Vec3.ZERO;
    private double distanceAccum;

    public RobotJetpackFlyGoal(RobotEntity robot) {
        this.robot = robot;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!robot.isPowered() || robot.isTransforming() || !robot.hasSword()) {
            return false;
        }
        LivingEntity candidate = resolveFlyTarget();
        if (candidate == null || robot.distanceToSqr(candidate) <= START_FLY_DIST_SQR) {
            return false;
        }
        this.flyTarget = candidate;
        this.stayBounded = !robot.isFollowOwner();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!robot.isPowered() || robot.isTransforming() || !robot.hasSword()) {
            return false;
        }
        if (flyTarget == null || !flyTarget.isAlive()) {
            return false;
        }
        if (flyTarget instanceof Player player && player.isSpectator()) {
            return false;
        }
        if (stayBounded) {
            if (robot.isFollowOwner() || !robot.isWithinPatrolArea(flyTarget.blockPosition())) {
                return false;
            }
        } else if (!robot.isFollowOwner()) {
            return false;
        }

        double dx = flyTarget.getX() - robot.getX();
        double dz = flyTarget.getZ() - robot.getZ();
        double horizDistSqr = dx * dx + dz * dz;
        return !(horizDistSqr <= LAND_HORIZ_DIST_SQR && Math.abs(flyTarget.getY() - robot.getY()) <= 2.5D);
    }

    @Override
    public void start() {
        robot.setFlying(true);
        robot.setNoGravity(true);
        robot.getNavigation().stop();
        lastFlyPos = robot.position();
        distanceAccum = 0.0D;
    }

    @Override
    public void stop() {
        flyTarget = null;
        robot.setFlying(false);
        robot.setNoGravity(false);
        robot.setDeltaMovement(Vec3.ZERO);
        distanceAccum = 0.0D;
    }

    @Override
    public void tick() {
        if (flyTarget == null) {
            return;
        }

        robot.resetFallDistance();
        robot.getLookControl().setLookAt(flyTarget, 30.0F, 30.0F);

        double dx = flyTarget.getX() - robot.getX();
        double dz = flyTarget.getZ() - robot.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        int groundHere = robot.level().getHeight(Heightmap.Types.MOTION_BLOCKING,
                Mth.floor(robot.getX()), Mth.floor(robot.getZ()));
        double aheadX = horizDist > 0.1D ? robot.getX() + (dx / horizDist) * 1.5D : robot.getX();
        double aheadZ = horizDist > 0.1D ? robot.getZ() + (dz / horizDist) * 1.5D : robot.getZ();
        int groundAhead = robot.level().getHeight(Heightmap.Types.MOTION_BLOCKING,
                Mth.floor(aheadX), Mth.floor(aheadZ));

        double cruiseY = Math.max(Math.max(groundHere, groundAhead) + 2.2D, flyTarget.getY() + 1.5D);
        double desiredY = horizDist > 5.0D ? cruiseY : flyTarget.getY() + 0.3D;
        double dy = desiredY - robot.getY();

        Vec3 dir = new Vec3(dx, dy, dz);
        double len = dir.length();
        if (len > 0.001D) {
            Vec3 vel = dir.scale(FLY_SPEED / len);
            if (stayBounded) {
                BlockPos nextPos = BlockPos.containing(robot.getX() + vel.x, robot.getY(), robot.getZ() + vel.z);
                if (!robot.isWithinPatrolArea(nextPos)) {
                    vel = Vec3.ZERO;
                }
            }
            robot.setDeltaMovement(vel);
            if (vel.lengthSqr() > 0.0001D) {
                float yaw = (float) (Mth.atan2(vel.z, vel.x) * Mth.RAD_TO_DEG) - 90.0F;
                robot.setYRot(yaw);
                robot.yBodyRot = yaw;
                robot.yHeadRot = yaw;
            }
        }

        double moved = robot.position().distanceTo(lastFlyPos);
        lastFlyPos = robot.position();
        distanceAccum += moved;
        if (distanceAccum >= 1.0D) {
            int blocksFlown = (int) Math.floor(distanceAccum);
            distanceAccum -= blocksFlown;
            robot.consumeEnergy(blocksFlown * FE_PER_BLOCK);
        }
    }

    @Nullable
    private LivingEntity resolveFlyTarget() {
        LivingEntity combatTarget = robot.getTarget();
        if (combatTarget != null && combatTarget.isAlive()) {
            if (robot.isFollowOwner() || robot.isWithinPatrolArea(combatTarget.blockPosition())) {
                return combatTarget;
            }
        }
        if (robot.isFollowOwner()) {
            Player owner = robot.getOwnerPlayer();
            if (owner != null && owner.isAlive() && !owner.isSpectator()) {
                return owner;
            }
        }
        return null;
    }
}
