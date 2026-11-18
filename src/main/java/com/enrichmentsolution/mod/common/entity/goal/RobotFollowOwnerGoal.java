package com.enrichmentsolution.mod.common.entity.goal;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Follows the robot's owner when the robot is in combat mode (holding a sword)
 * and the follow-owner toggle is enabled in its GUI.
 */
public class RobotFollowOwnerGoal extends Goal {

    private final RobotEntity robot;
    private final double speedModifier;
    private final float startDistanceSqr;
    private final float stopDistanceSqr;
    @Nullable
    private Player owner;
    private int timeToRecalcPath;

    public RobotFollowOwnerGoal(RobotEntity robot, double speedModifier, float startDistance, float stopDistance) {
        this.robot = robot;
        this.speedModifier = speedModifier;
        this.startDistanceSqr = startDistance * startDistance;
        this.stopDistanceSqr = stopDistance * stopDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!robot.isPowered() || !robot.hasSword() || robot.isTransforming() || !robot.isFollowOwner() || robot.getTarget() != null) {
            return false;
        }
        Player ownerPlayer = robot.getOwnerPlayer();
        if (ownerPlayer == null || ownerPlayer.isSpectator()) {
            return false;
        }
        if (robot.distanceToSqr(ownerPlayer) < startDistanceSqr) {
            return false;
        }
        this.owner = ownerPlayer;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!robot.isPowered() || !robot.hasSword() || robot.isTransforming() || !robot.isFollowOwner() || robot.getTarget() != null) {
            return false;
        }
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            return false;
        }
        return robot.distanceToSqr(owner) > stopDistanceSqr;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.robot.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        robot.getLookControl().setLookAt(owner, 10.0F, robot.getMaxHeadXRot());
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;
            robot.getNavigation().moveTo(owner, speedModifier);
        }
    }
}
