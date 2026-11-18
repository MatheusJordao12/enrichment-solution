package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.TankPortBlock;
import com.enrichmentsolution.mod.common.block.TankPortBlock.PortMode;
import com.enrichmentsolution.mod.common.fluid.ThermalFluidHelper;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Port BlockEntity that exposes FluidHandler capability for pipes and buckets,
 * forwarding fill (INPUT) or drain (OUTPUT) to the adjacent TankBlockEntity master.
 */
public class TankPortBlockEntity extends BlockEntity {

    private final PortMode mode;
    private final IFluidHandler fluidHandler = new PortFluidHandler();

    public TankPortBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TANK_PORT.get(), pos, blockState);
        this.mode = blockState.getBlock() instanceof TankPortBlock port ? port.getMode() : PortMode.INPUT;
    }

    @Nullable
    public TankBlockEntity findConnectedMaster() {
        if (level == null) {
            return null;
        }
        for (Direction dir : Direction.values()) {
            BlockPos adj = worldPosition.relative(dir);
            if (level.getBlockEntity(adj) instanceof TankBlockEntity tank) {
                return tank.getMasterEntity();
            }
        }
        return null;
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return fluidHandler;
    }

    private class PortFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            TankBlockEntity master = findConnectedMaster();
            if (master == null) {
                return FluidStack.EMPTY;
            }
            FluidStack stack = master.getMasterTank().getFluid();
            if (stack.isEmpty() || mode != PortMode.OUTPUT) {
                return stack;
            }
            return ThermalFluidHelper.withThermalData(stack.copy(), master.getTemperature(), master.getMultiplier());
        }

        @Override
        public int getTankCapacity(int tank) {
            TankBlockEntity master = findConnectedMaster();
            return master != null ? master.getMasterTank().getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (mode != PortMode.INPUT) {
                return false;
            }
            TankBlockEntity master = findConnectedMaster();
            return master != null && master.getMasterTank().isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (mode != PortMode.INPUT || resource.isEmpty()) {
                return 0;
            }
            TankBlockEntity master = findConnectedMaster();
            return master != null ? master.getMasterTank().fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (mode != PortMode.OUTPUT || resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            TankBlockEntity master = findConnectedMaster();
            if (master == null) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = master.getMasterTank().getFluid();
            if (stored.isEmpty() || stored.getFluid() != resource.getFluid()) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = master.getMasterTank().drain(resource.getAmount(), action);
            return ThermalFluidHelper.withThermalData(drained, master.getTemperature(), master.getMultiplier());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (mode != PortMode.OUTPUT || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            TankBlockEntity master = findConnectedMaster();
            if (master == null) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = master.getMasterTank().drain(maxDrain, action);
            return ThermalFluidHelper.withThermalData(drained, master.getTemperature(), master.getMultiplier());
        }
    }
}
