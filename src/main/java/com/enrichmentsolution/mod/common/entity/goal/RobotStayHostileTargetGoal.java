package com.enrichmentsolution.mod.common.entity.goal;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Proactively detects hostile mobs inside the robot's 3x3 chunk Stay area.
 * Drops the target if it leaves the 3x3 chunk bounds.
 */
public class RobotStayHostileTargetGoal extends TargetGoal {

    private final RobotEntity robot;
    @Nullable
    private LivingEntity target;

    public RobotStayHostileTargetGoal(RobotEntity robot) {
        super(robot, false);
        this.robot = robot;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!robot.isPowered() || robot.isTransforming() || !robot.hasSword() || robot.isFollowOwner()) {
            return false;
        }
        if (!robot.isWithinPatrolArea(robot.blockPosition())) {
            return false;
        }

        AABB searchBox = robot.getPatrolAreaBox();
        List<LivingEntity> hostiles = robot.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> entity instanceof Enemy
                        && entity.isAlive()
                        && robot.isWithinPatrolArea(entity.blockPosition())
                        && canAttack(entity, TargetingConditions.DEFAULT)
        );

        if (hostiles.isEmpty()) {
            return false;
        }

        this.target = hostiles.stream()
                .min(Comparator.comparingDouble(robot::distanceToSqr))
                .orElse(null);
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!robot.isPowered() || robot.isTransforming() || !robot.hasSword() || robot.isFollowOwner()) {
            return false;
        }
        LivingEntity current = this.mob.getTarget();
        if (current == null || !current.isAlive() || !robot.isWithinPatrolArea(current.blockPosition())) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.target);
        super.start();
    }

    @Override
    public void stop() {
        this.target = null;
        super.stop();
    }
}
