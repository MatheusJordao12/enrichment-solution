package com.enrichmentsolution.mod.common.entity.goal;

import com.enrichmentsolution.mod.common.block.RobotChargerBlock;
import com.enrichmentsolution.mod.common.block.entity.RobotChargerBlockEntity;
import com.enrichmentsolution.mod.common.entity.RobotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * When the robot has the Auto Recharger upgrade, is linked to a charger,
 * and drops to <= 10% energy, it pauses its tasks and goes to its linked charger
 * until reaching 100% (or the charger runs out of energy, or the player cancels with Shift+Right-Click).
 */
public class RobotAutoRechargeGoal extends Goal {

    private static final double TRIGGER_RATIO = 0.10D;
    private static final double DOCK_HORIZ_DIST_SQR = 0.36D; // 0.6 blocks from center of charger

    private final RobotEntity robot;
    private int repathCooldown;
    private int emptyWaitTicks;

    public RobotAutoRechargeGoal(RobotEntity robot) {
        this.robot = robot;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!robot.isPowered() || robot.isTransforming() || !robot.hasAutoRecharger()
                || !robot.hasChargerPos() || robot.isRechargeCancelled()) {
            return false;
        }
        int max = robot.getMaxEnergyStored();
        if (max <= 0) {
            return false;
        }
        if (robot.getEnergyStored() > max * TRIGGER_RATIO) {
            return false;
        }
        BlockPos chargerPos = robot.getChargerPos();
        if (chargerPos == null) {
            return false;
        }
        Level level = robot.level();
        if (!(level.getBlockState(chargerPos).getBlock() instanceof RobotChargerBlock)) {
            return false;
        }
        if (level.getBlockEntity(chargerPos) instanceof RobotChargerBlockEntity charger && charger.getEnergyStored() <= 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (robot.isTransforming() || !robot.hasAutoRecharger()
                || !robot.hasChargerPos() || robot.isRechargeCancelled()) {
            return false;
        }
        if (robot.getMaxEnergyStored() > 0 && robot.getEnergyStored() >= robot.getMaxEnergyStored()) {
            return false;
        }
        BlockPos chargerPos = robot.getChargerPos();
        return chargerPos != null && robot.level().getBlockState(chargerPos).getBlock() instanceof RobotChargerBlock;
    }

    @Override
    public void start() {
        robot.setRecharging(true);
        robot.setMining(false);
        robot.setTarget(null);
        if (robot.isFlying()) {
            robot.setFlying(false);
            robot.setNoGravity(false);
        }
        repathCooldown = 0;
        emptyWaitTicks = 0;
    }

    @Override
    public void stop() {
        robot.setDocked(false);
        robot.setRecharging(false);
        robot.getNavigation().stop();
    }

    @Override
    public void tick() {
        BlockPos chargerPos = robot.getChargerPos();
        if (chargerPos == null) {
            return;
        }

        double cx = chargerPos.getX() + 0.5D;
        double cy = chargerPos.getY() + 0.25D;
        double cz = chargerPos.getZ() + 0.5D;

        double dx = cx - robot.getX();
        double dz = cz - robot.getZ();
        double horizDistSqr = dx * dx + dz * dz;

        if (horizDistSqr <= DOCK_HORIZ_DIST_SQR && Math.abs(robot.getY() - cy) <= 1.0D) {
            robot.getNavigation().stop();
            robot.setDocked(true);
            var state = robot.level().getBlockState(chargerPos);
            if (state.hasProperty(RobotChargerBlock.FACING)) {
                float targetYaw = state.getValue(RobotChargerBlock.FACING).toYRot();
                robot.setYRot(targetYaw);
                robot.yBodyRot = targetYaw;
                robot.yHeadRot = targetYaw;
            }
            if (robot.level().getBlockEntity(chargerPos) instanceof RobotChargerBlockEntity charger && charger.getEnergyStored() <= 0) {
                emptyWaitTicks++;
            } else {
                emptyWaitTicks = 0;
            }
        } else {
            robot.setDocked(false);
            robot.getLookControl().setLookAt(cx, cy + 0.5D, cz, 30.0F, 30.0F);
            emptyWaitTicks = 0;
            if (horizDistSqr <= 4.0D) {
                robot.getMoveControl().setWantedPosition(cx, cy, cz, 1.05D);
            } else if (--repathCooldown <= 0 || robot.getNavigation().isDone()) {
                repathCooldown = 15;
                robot.getNavigation().moveTo(cx, cy, cz, 1.15D);
            }
        }
    }
}
