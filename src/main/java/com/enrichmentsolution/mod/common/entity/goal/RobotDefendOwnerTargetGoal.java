package com.enrichmentsolution.mod.common.entity.goal;

import com.enrichmentsolution.mod.common.entity.RobotEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Targets mobs or players that attacked the robot's owner (or that the owner attacked),
 * active only while the robot is equipped with a sword.
 */
public class RobotDefendOwnerTargetGoal extends TargetGoal {

    private final RobotEntity robot;
    @Nullable
    private LivingEntity target;
    private int timestampHurtBy;
    private int timestampHurt;

    public RobotDefendOwnerTargetGoal(RobotEntity robot) {
        super(robot, false);
        this.robot = robot;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!robot.isPowered() || !robot.hasSword() || robot.isTransforming()) {
            return false;
        }
        Player owner = robot.getOwnerPlayer();
        if (owner == null) {
            return false;
        }

        LivingEntity hurtBy = owner.getLastHurtByMob();
        int hurtByTime = owner.getLastHurtByMobTimestamp();
        if (hurtByTime != this.timestampHurtBy && isValidOwnerTarget(owner, hurtBy)) {
            this.target = hurtBy;
            return true;
        }

        LivingEntity hurt = owner.getLastHurtMob();
        int hurtTime = owner.getLastHurtMobTimestamp();
        if (hurtTime != this.timestampHurt && isValidOwnerTarget(owner, hurt)) {
            this.target = hurt;
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return robot.isPowered() && robot.hasSword() && !robot.isTransforming() && super.canContinueToUse();
    }

    @Override
    public void start() {
        this.mob.setTarget(this.target);
        Player owner = robot.getOwnerPlayer();
        if (owner != null) {
            this.timestampHurtBy = owner.getLastHurtByMobTimestamp();
            this.timestampHurt = owner.getLastHurtMobTimestamp();
        }
        super.start();
    }

    private boolean isValidOwnerTarget(Player owner, @Nullable LivingEntity candidate) {
        if (candidate == null || candidate == owner || candidate == robot || !candidate.isAlive()) {
            return false;
        }
        if (candidate instanceof RobotEntity otherRobot && owner.getUUID().equals(otherRobot.getOwnerUUID())) {
            return false;
        }
        return canAttack(candidate, TargetingConditions.DEFAULT);
    }
}
