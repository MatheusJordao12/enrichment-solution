package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.entity.RobotEntity;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Stores up to 500,000 FE from cables/generators and transfers up to 5,000 FE/t
 * to any RobotEntity positioned on or adjacent to the charging pad.
 */
public class RobotChargerBlockEntity extends AbstractMachineBlockEntity {

    public static final int ENERGY_CAPACITY = 500_000;
    public static final int MAX_RECEIVE = 5_000;
    public static final int CHARGE_RATE_PER_TICK = 5_000;

    public RobotChargerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ROBOT_CHARGER.get(), pos, blockState, ENERGY_CAPACITY, MAX_RECEIVE, 0, 0);
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        if (energyStorage.getEnergyStored() <= 0) {
            return;
        }

        AABB padArea = new AABB(pos).inflate(0.75D, 1.5D, 0.75D);
        List<RobotEntity> robots = level.getEntitiesOfClass(RobotEntity.class, padArea,
                robot -> robot.isAlive() && robot.getEnergyStored() < robot.getMaxEnergyStored());

        for (RobotEntity robot : robots) {
            int available = energyStorage.getEnergyStored();
            if (available <= 0) {
                break;
            }
            int needed = robot.getMaxEnergyStored() - robot.getEnergyStored();
            int toTransfer = Math.min(CHARGE_RATE_PER_TICK, Math.min(available, needed));
            if (toTransfer > 0) {
                int accepted = robot.chargeEnergy(toTransfer);
                if (accepted > 0) {
                    energyStorage.consumeInternal(accepted, false);
                }
            }
        }
    }
}
