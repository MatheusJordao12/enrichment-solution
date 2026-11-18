package com.enrichmentsolution.mod.common.entity.goal;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Patrols within the 3x3 chunk area anchored when the robot is set to Stay mode.
 * Also pulls the robot back inside the area if it ever leaves its bounds.
 */
public class RobotStayPatrolGoal extends Goal {

    private final RobotEntity robot;
    private int patrolCooldown;
    @Nullable
    private Vec3 targetPos;

    public RobotStayPatrolGoal(RobotEntity robot) {
        this.robot = robot;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!robot.isPowered() || robot.isTransforming() || !robot.hasSword()
                || robot.isFollowOwner() || robot.isFlying() || robot.getTarget() != null) {
            return false;
        }

        if (!robot.isWithinPatrolArea(robot.blockPosition())) {
            BlockPos center = robot.getPatrolAnchorCenterPos();
            targetPos = Vec3.atBottomCenterOf(center);
            return true;
        }

        if (patrolCooldown > 0) {
            patrolCooldown--;
            return false;
        }
        patrolCooldown = 40 + robot.getRandom().nextInt(60);

        for (int attempt = 0; attempt < 6; attempt++) {
            Vec3 candidate = DefaultRandomPos.getPos(robot, 12, 5);
            if (candidate != null && robot.isWithinPatrolArea(candidate.x, candidate.z)) {
                targetPos = candidate;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return robot.isPowered()
                && !robot.isTransforming()
                && robot.hasSword()
                && !robot.isFollowOwner()
                && !robot.isFlying()
                && robot.getTarget() == null
                && !robot.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (targetPos != null) {
            robot.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 0.9D);
        }
    }

    @Override
    public void stop() {
        targetPos = null;
        robot.getNavigation().stop();
    }
}
