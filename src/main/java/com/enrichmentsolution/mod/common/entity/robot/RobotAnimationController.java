package com.enrichmentsolution.mod.common.entity.robot;

import com.enrichmentsolution.mod.common.entity.RobotEntity;

/**
 * Updates RobotEntity server-side transition timers and client-side AnimationStates.
 */
public final class RobotAnimationController {

    private RobotAnimationController() {
    }

    public static void tickServerTimers(RobotEntity robot) {
        int transform = robot.getTransformTicks();
        if (transform > 0) {
            robot.setTransformTicks(transform - 1);
        }
        int detransform = robot.getDetransformTicks();
        if (detransform > 0) {
            robot.setDetransformTicks(detransform - 1);
        }
        int boot = robot.getBootTicks();
        if (boot > 0) {
            robot.setBootTicks(boot - 1);
        }
        int powerOff = robot.getPowerOffTicks();
        if (powerOff > 0) {
            robot.setPowerOffTicks(powerOff - 1);
        }
        if (transform > 0 || detransform > 0 || boot > 0 || !robot.isPowered()) {
            robot.getNavigation().stop();
            robot.setTarget(null);
            robot.setMining(false);
            if (robot.isFlying()) {
                robot.setFlying(false);
                robot.setNoGravity(false);
            }
        }
    }

    public static void tickClientAnimations(RobotEntity robot) {
        if (robot.isFlying()) {
            robot.flyAnimationState.startIfStopped(robot.tickCount);
        } else {
            robot.flyAnimationState.stop();
        }
        if (robot.isDocked()) {
            robot.dockAnimationState.startIfStopped(robot.tickCount);
        } else {
            robot.dockAnimationState.stop();
        }
        if (robot.getBootTicks() > 0 && !robot.summonBootAnimationState.isStarted()) {
            robot.powerOffAnimationState.stop();
            robot.summonBootAnimationState.start(robot.tickCount);
        } else if (robot.getBootTicks() <= 0 && robot.summonBootAnimationState.isStarted()) {
            robot.summonBootAnimationState.stop();
        }

        if (robot.getTransformTicks() > 0 && !robot.combatTransformAnimationState.isStarted()) {
            robot.powerOffAnimationState.stop();
            robot.detransformAnimationState.stop();
            robot.combatTransformAnimationState.start(robot.tickCount);
        } else if (robot.getTransformTicks() <= 0 && robot.combatTransformAnimationState.isStarted()) {
            robot.combatTransformAnimationState.stop();
        }

        if (robot.getDetransformTicks() > 0 && !robot.detransformAnimationState.isStarted()) {
            robot.powerOffAnimationState.stop();
            robot.combatTransformAnimationState.stop();
            robot.detransformAnimationState.start(robot.tickCount);
        } else if (robot.getDetransformTicks() <= 0 && robot.detransformAnimationState.isStarted()) {
            robot.detransformAnimationState.stop();
        }

        if (robot.getPowerOffTicks() > 0 && !robot.powerOffAnimationState.isStarted()) {
            robot.summonBootAnimationState.stop();
            robot.combatTransformAnimationState.stop();
            robot.detransformAnimationState.stop();
            robot.powerOffAnimationState.start(robot.tickCount);
        } else if (robot.isPowered() && robot.powerOffAnimationState.isStarted()) {
            robot.powerOffAnimationState.stop();
        }

        if (robot.isMining() && robot.isPowered() && !robot.isTransforming()) {
            robot.mineAnimationState.startIfStopped(robot.tickCount);
        } else {
            robot.mineAnimationState.stop();
        }
    }
}
