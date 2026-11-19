package com.enrichmentsolution.mod.common.block.entity.heatexchanger;

import com.enrichmentsolution.mod.common.block.entity.TankBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.TankPortBlockEntity;
import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Manages the Heat Exchanger's water tank (including weighted thermal data merging)
 * and computes effective temperature and thermal multiplier from adjacent Tank multiblocks
 * and piped water.
 */
public class HeatExchangerThermalHandler {

    private final FluidTank waterTank;
    private double currentTemperature = 20.0D;
    private double currentMultiplier = 1.0D;

    public HeatExchangerThermalHandler(Runnable onChanged) {
        this.waterTank = new FluidTank(
                ModServerConfig.get(ModServerConfig.HEAT_EXCHANGER_WATER_TANK_CAPACITY),
                stack -> stack.getFluid() == Fluids.WATER
        ) {
            @Override
            protected void onContentsChanged() {
                onChanged.run();
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || resource.getFluid() != Fluids.WATER) {
                    return 0;
                }
                if (fluid.isEmpty()) {
                    int filled = Math.min(capacity, resource.getAmount());
                    if (action.execute() && filled > 0) {
                        fluid = resource.copyWithAmount(filled);
                        onContentsChanged();
                    }
                    return filled;
                }
                if (fluid.getFluid() != Fluids.WATER) {
                    return 0;
                }
                int space = capacity - fluid.getAmount();
                int filled = Math.min(space, resource.getAmount());
                if (action.execute() && filled > 0) {
                    int oldAmt = fluid.getAmount();
                    int newAmt = oldAmt + filled;
                    double existingTemp = fluid.getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), 20.0D);
                    double existingMult = fluid.getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), 1.0D);
                    double incomingTemp = resource.getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), 20.0D);
                    double incomingMult = resource.getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), 1.0D);
                    fluid.setAmount(newAmt);
                    fluid.set(ModDataComponents.FLUID_TEMPERATURE.get(), (existingTemp * oldAmt + incomingTemp * filled) / newAmt);
                    fluid.set(ModDataComponents.FLUID_MULTIPLIER.get(), (existingMult * oldAmt + incomingMult * filled) / newAmt);
                    onContentsChanged();
                }
                return filled;
            }
        };
    }

    public FluidTank getWaterTank() {
        return waterTank;
    }

    public double getCurrentTemperature() {
        return currentTemperature;
    }

    public double getCurrentMultiplier() {
        return currentMultiplier;
    }

    public record HeatSourceResult(boolean hasHeatWater, @Nullable TankBlockEntity adjacentTank, boolean preferTankWater) {
    }

    /**
     * Evaluates adjacent Tank masters and piped water to update {@code currentTemperature}
     * and {@code currentMultiplier}, returning which heat source should be drained on boil.
     */
    public HeatSourceResult evaluateHeatSources(Level level, List<BlockPos> scanList) {
        TankBlockEntity adjacentTank = findBestAdjacentTankMaster(level, scanList);
        boolean hasTankWater = adjacentTank != null && adjacentTank.getMasterTank().getFluidAmount() > 0;
        double tankTemp = adjacentTank != null ? adjacentTank.getTemperature() : 20.0D;
        double tankMult = adjacentTank != null ? adjacentTank.getMultiplier() : 1.0D;

        FluidStack pipedStack = waterTank.getFluid();
        boolean hasPipedWater = !pipedStack.isEmpty();
        double pipedTemp = hasPipedWater
                ? pipedStack.getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), 20.0D)
                : 20.0D;
        double pipedMult = hasPipedWater
                ? pipedStack.getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), 1.0D)
                : 1.0D;

        boolean hasHeatWater = hasTankWater || hasPipedWater;
        if (hasTankWater && hasPipedWater) {
            currentTemperature = Math.max(tankTemp, pipedTemp);
            currentMultiplier = Math.max(tankMult, pipedMult);
        } else if (hasTankWater) {
            currentTemperature = tankTemp;
            currentMultiplier = tankMult;
        } else if (hasPipedWater) {
            currentTemperature = pipedTemp;
            currentMultiplier = pipedMult;
        } else {
            currentTemperature = adjacentTank != null ? tankTemp : 20.0D;
            currentMultiplier = adjacentTank != null ? tankMult : 1.0D;
        }

        boolean preferTank = hasTankWater && (!hasPipedWater || tankMult >= pipedMult);
        return new HeatSourceResult(hasHeatWater, adjacentTank, preferTank);
    }

    public void consumeHeatWater(HeatSourceResult source) {
        if (source.preferTankWater() && source.adjacentTank() != null) {
            source.adjacentTank().getMasterTank().drain(1, IFluidHandler.FluidAction.EXECUTE);
        } else {
            waterTank.drain(1, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Nullable
    private TankBlockEntity findBestAdjacentTankMaster(Level level, List<BlockPos> scanList) {
        TankBlockEntity best = null;
        boolean bestHasWater = false;
        for (BlockPos member : scanList) {
            for (Direction dir : Direction.values()) {
                BlockEntity be = level.getBlockEntity(member.relative(dir));
                TankBlockEntity candidate = null;
                if (be instanceof TankBlockEntity tank) {
                    candidate = tank.getMasterEntity();
                } else if (be instanceof TankPortBlockEntity port) {
                    candidate = port.findConnectedMaster();
                }
                if (candidate == null) {
                    continue;
                }
                boolean hasWater = candidate.getMasterTank().getFluidAmount() > 0;
                if (best == null
                        || (hasWater && !bestHasWater)
                        || (hasWater == bestHasWater && candidate.getMultiplier() > best.getMultiplier())) {
                    best = candidate;
                    bestHasWater = hasWater;
                }
            }
        }
        return best;
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putDouble("CurrentTemperature", currentTemperature);
        tag.putDouble("CurrentMultiplier", currentMultiplier);
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries, int blockCount) {
        if (tag.contains("CurrentTemperature")) {
            this.currentTemperature = tag.getDouble("CurrentTemperature");
        }
        if (tag.contains("CurrentMultiplier")) {
            this.currentMultiplier = tag.getDouble("CurrentMultiplier");
        }
        int waterCap = ModServerConfig.get(ModServerConfig.HEAT_EXCHANGER_WATER_TANK_CAPACITY);
        this.waterTank.setCapacity(blockCount * waterCap);
        if (tag.contains("WaterTank")) {
            waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        }
    }
}
