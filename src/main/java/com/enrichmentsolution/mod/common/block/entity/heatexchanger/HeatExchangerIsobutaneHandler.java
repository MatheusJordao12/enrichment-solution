package com.enrichmentsolution.mod.common.block.entity.heatexchanger;

import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.entity.HeatExchangerBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.TankBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.TankPortBlockEntity;
import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.enrichmentsolution.mod.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

/**
 * Manages the Heat Exchanger's liquid/gaseous isobutane tanks, boiling step,
 * neighbor gas pushing, and combined IFluidHandler exposure.
 */
public class HeatExchangerIsobutaneHandler {

    private final FluidTank liquidIsobutaneTank;
    private final FluidTank gaseousIsobutaneTank;
    private final IFluidHandler fluidHandler;

    public HeatExchangerIsobutaneHandler(FluidTank waterTank, Runnable onChanged) {
        int isoCap = ModServerConfig.get(ModServerConfig.HEAT_EXCHANGER_ISOBUTANE_TANK_CAPACITY);
        this.liquidIsobutaneTank = new FluidTank(isoCap,
                stack -> stack.getFluid() == ModFluids.LIQUID_ISOBUTANE.get()) {
            @Override
            protected void onContentsChanged() {
                onChanged.run();
            }
        };
        this.gaseousIsobutaneTank = new FluidTank(isoCap,
                stack -> stack.getFluid() == ModFluids.GASEOUS_ISOBUTANE.get()) {
            @Override
            protected void onContentsChanged() {
                onChanged.run();
            }
        };
        this.fluidHandler = new ExchangerFluidHandler(waterTank);
    }

    public FluidTank getLiquidIsobutaneTank() {
        return liquidIsobutaneTank;
    }

    public FluidTank getGaseousIsobutaneTank() {
        return gaseousIsobutaneTank;
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    /**
     * Converts up to {@code scaledRate} mB of liquid isobutane into gaseous isobutane.
     * Returns the amount actually boiled (0 if either tank prevents conversion).
     */
    public int boil(int scaledRate) {
        if (liquidIsobutaneTank.isEmpty() || gaseousIsobutaneTank.getSpace() <= 0) {
            return 0;
        }
        int toBoil = Math.min(scaledRate, Math.min(liquidIsobutaneTank.getFluidAmount(), gaseousIsobutaneTank.getSpace()));
        if (toBoil <= 0) {
            return 0;
        }
        liquidIsobutaneTank.drain(toBoil, IFluidHandler.FluidAction.EXECUTE);
        gaseousIsobutaneTank.fill(new FluidStack(ModFluids.GASEOUS_ISOBUTANE.get(), toBoil), IFluidHandler.FluidAction.EXECUTE);
        return toBoil;
    }

    public void pushGasToNeighbors(
            Level level,
            List<BlockPos> scanList,
            SideConfigHolder sideConfig,
            int count,
            Runnable onChanged
    ) {
        if (gaseousIsobutaneTank.isEmpty()) {
            return;
        }
        int maxPush = ModServerConfig.get(ModServerConfig.HEAT_EXCHANGER_MAX_PUSH_RATE) * count;
        for (BlockPos memberPos : scanList) {
            if (gaseousIsobutaneTank.isEmpty()) {
                break;
            }
            BlockState memberState = level.getBlockState(memberPos);
            Direction memberFacing = memberState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? memberState.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : Direction.NORTH;
            for (Direction dir : Direction.values()) {
                if (gaseousIsobutaneTank.isEmpty()) {
                    break;
                }
                if (!sideConfig.canPushTo(dir, memberFacing)) {
                    continue;
                }
                BlockPos neighborPos = memberPos.relative(dir);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                if (neighbor == null
                        || neighbor instanceof HeatExchangerBlockEntity
                        || neighbor instanceof TankBlockEntity
                        || neighbor instanceof TankPortBlockEntity) {
                    continue;
                }
                IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
                if (target == null) {
                    continue;
                }
                int amountToPush = Math.min(maxPush, gaseousIsobutaneTank.getFluidAmount());
                FluidStack toSend = new FluidStack(ModFluids.GASEOUS_ISOBUTANE.get(), amountToPush);
                int accepted = target.fill(toSend, IFluidHandler.FluidAction.EXECUTE);
                if (accepted > 0) {
                    gaseousIsobutaneTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                    onChanged.run();
                }
            }
        }
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("LiquidIsobutaneTank", liquidIsobutaneTank.writeToNBT(registries, new CompoundTag()));
        tag.put("GaseousIsobutaneTank", gaseousIsobutaneTank.writeToNBT(registries, new CompoundTag()));
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries, int blockCount) {
        int isoCap = ModServerConfig.get(ModServerConfig.HEAT_EXCHANGER_ISOBUTANE_TANK_CAPACITY);
        this.liquidIsobutaneTank.setCapacity(blockCount * isoCap);
        this.gaseousIsobutaneTank.setCapacity(blockCount * isoCap);
        if (tag.contains("LiquidIsobutaneTank")) {
            liquidIsobutaneTank.readFromNBT(registries, tag.getCompound("LiquidIsobutaneTank"));
        }
        if (tag.contains("GaseousIsobutaneTank")) {
            gaseousIsobutaneTank.readFromNBT(registries, tag.getCompound("GaseousIsobutaneTank"));
        }
    }

    private class ExchangerFluidHandler implements IFluidHandler {

        private final FluidTank waterTank;

        private ExchangerFluidHandler(FluidTank waterTank) {
            this.waterTank = waterTank;
        }

        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> liquidIsobutaneTank.getFluid();
                case 1 -> gaseousIsobutaneTank.getFluid();
                case 2 -> waterTank.getFluid();
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> liquidIsobutaneTank.getCapacity();
                case 1 -> gaseousIsobutaneTank.getCapacity();
                case 2 -> waterTank.getCapacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return switch (tank) {
                case 0 -> liquidIsobutaneTank.isFluidValid(stack);
                case 1 -> gaseousIsobutaneTank.isFluidValid(stack);
                case 2 -> waterTank.isFluidValid(stack);
                default -> false;
            };
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            if (resource.getFluid() == ModFluids.LIQUID_ISOBUTANE.get()) {
                return liquidIsobutaneTank.fill(resource, action);
            }
            if (resource.getFluid() == Fluids.WATER) {
                return waterTank.fill(resource, action);
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != ModFluids.GASEOUS_ISOBUTANE.get()) {
                return FluidStack.EMPTY;
            }
            return gaseousIsobutaneTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            return gaseousIsobutaneTank.drain(maxDrain, action);
        }
    }
}
