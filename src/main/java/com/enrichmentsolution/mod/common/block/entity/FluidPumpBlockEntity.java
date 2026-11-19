package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.FluidPumpBlock;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.pump.FluidPumpScanner;
import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.enrichmentsolution.mod.common.menu.FluidPumpMenu;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Fluid Pump block entity that extracts Water and finite Crude Oil from the world.
 * Scans downward up to 64 blocks (with an 8-block horizontal pool scan) and traces
 * flowing Crude Oil upstream to its source block so oil deposits are finite.
 */
public class FluidPumpBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    private int progress = 0;
    private boolean isWorking = false;

    private final SideConfigHolder sideConfig = SideConfigHolder.createDefault(
            SideConfigMode.NONE,
            SideConfigMode.INPUT,
            SideConfigMode.INPUT,
            SideConfigMode.OUTPUT,
            SideConfigMode.OUTPUT,
            SideConfigMode.NONE
    );

    private final FluidTank outputTank = new FluidTank(ModServerConfig.get(ModServerConfig.FLUID_PUMP_TANK_CAPACITY),
            stack -> stack.getFluid() == Fluids.WATER || stack.getFluid() == ModFluids.CRUDE_OIL.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? outputTank.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? outputTank.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return outputTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return outputTank.drain(maxDrain, action);
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int energy = energyStorage.getEnergyStored();
            int maxEnergy = energyStorage.getMaxEnergyStored();
            int outFluid = outputTank.getFluidAmount();
            int outCap = outputTank.getCapacity();
            int fluidType = getSyncedFluidType();
            return switch (index) {
                case FluidPumpMenu.DATA_PROGRESS -> progress;
                case FluidPumpMenu.DATA_MAX_PROGRESS -> ModServerConfig.get(ModServerConfig.FLUID_PUMP_INTERVAL);
                case FluidPumpMenu.DATA_ENERGY_LOW -> energy & 0xFFFF;
                case FluidPumpMenu.DATA_ENERGY_HIGH -> (energy >>> 16) & 0xFFFF;
                case FluidPumpMenu.DATA_MAX_ENERGY_LOW -> maxEnergy & 0xFFFF;
                case FluidPumpMenu.DATA_MAX_ENERGY_HIGH -> (maxEnergy >>> 16) & 0xFFFF;
                case FluidPumpMenu.DATA_OUTPUT_FLUID_LOW -> outFluid & 0xFFFF;
                case FluidPumpMenu.DATA_OUTPUT_FLUID_HIGH -> (outFluid >>> 16) & 0xFFFF;
                case FluidPumpMenu.DATA_OUTPUT_CAP_LOW -> outCap & 0xFFFF;
                case FluidPumpMenu.DATA_OUTPUT_CAP_HIGH -> (outCap >>> 16) & 0xFFFF;
                case FluidPumpMenu.DATA_FLUID_TYPE -> fluidType;
                default -> {
                    if (index >= FluidPumpMenu.DATA_SIDE_BASE
                            && index < FluidPumpMenu.DATA_SIDE_BASE + RelativeSide.values().length) {
                        yield sideConfig.getMode(RelativeSide.fromOrdinal(index - FluidPumpMenu.DATA_SIDE_BASE)).ordinal();
                    }
                    yield 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == FluidPumpMenu.DATA_PROGRESS) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return FluidPumpMenu.DATA_COUNT;
        }
    };

    public FluidPumpBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.FLUID_PUMP.get(),
                pos,
                blockState,
                ModServerConfig.get(ModServerConfig.FLUID_PUMP_ENERGY_CAPACITY),
                ModServerConfig.get(ModServerConfig.FLUID_PUMP_MAX_RECEIVE),
                0,
                0
        );
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(FluidPumpBlock.FACING) ? state.getValue(FluidPumpBlock.FACING) : Direction.NORTH;
    }

    public SideConfigMode getRelativeSideMode(RelativeSide side) {
        return sideConfig.getMode(side);
    }

    public void cycleRelativeSideMode(int ordinal) {
        sideConfig.cycleMode(RelativeSide.fromOrdinal(ordinal));
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    @Nullable
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return sideConfig.wrapEnergyStorage(energyStorage, side, getFacing());
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return sideConfig.wrapFluidHandler(fluidHandler, side, getFacing());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enrichment_solution.fluid_pump");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidPumpMenu(containerId, playerInventory, this, data);
    }

    private int getSyncedFluidType() {
        if (outputTank.isEmpty()) {
            return FluidPumpMenu.FLUID_NONE;
        }
        Fluid fluid = outputTank.getFluid().getFluid();
        if (fluid == Fluids.WATER) {
            return FluidPumpMenu.FLUID_WATER;
        }
        if (fluid == ModFluids.CRUDE_OIL.get()) {
            return FluidPumpMenu.FLUID_CRUDE_OIL;
        }
        return FluidPumpMenu.FLUID_NONE;
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        int energyPerPump = ModServerConfig.get(ModServerConfig.FLUID_PUMP_ENERGY_PER_PUMP);
        int pumpInterval = ModServerConfig.get(ModServerConfig.FLUID_PUMP_INTERVAL);
        if (outputTank.getSpace() >= FluidType.BUCKET_VOLUME
                && energyStorage.getEnergyStored() >= energyPerPump) {
            progress++;
            if (progress >= pumpInterval) {
                progress = 0;
                if (FluidPumpScanner.tryPump(level, pos, outputTank)) {
                    energyStorage.consumeInternal(energyPerPump, false);
                    isWorking = true;
                } else {
                    isWorking = false;
                }
            }
            setChanged();
        } else {
            if (progress != 0 || isWorking) {
                progress = 0;
                isWorking = false;
                setChanged();
            }
        }

        if (state.getValue(FluidPumpBlock.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(FluidPumpBlock.LIT, isWorking), Block.UPDATE_ALL);
        }

        pushOutputFluid(level, pos);
    }

    private void pushOutputFluid(Level level, BlockPos pos) {
        if (outputTank.isEmpty()) {
            return;
        }
        Direction facing = getFacing();
        for (Direction dir : Direction.values()) {
            if (outputTank.isEmpty()) {
                break;
            }
            if (!sideConfig.canPushTo(dir, facing)) {
                continue;
            }
            BlockPos neighborPos = pos.relative(dir);
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
            if (target == null) {
                continue;
            }
            FluidStack toSend = outputTank.drain(
                    ModServerConfig.get(ModServerConfig.FLUID_PUMP_AUTO_PUSH_RATE),
                    IFluidHandler.FluidAction.SIMULATE
            );
            if (!toSend.isEmpty()) {
                int accepted = target.fill(toSend, IFluidHandler.FluidAction.EXECUTE);
                if (accepted > 0) {
                    outputTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
        tag.putBoolean("IsWorking", isWorking);
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        sideConfig.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Progress")) {
            progress = tag.getInt("Progress");
        }
        if (tag.contains("IsWorking")) {
            isWorking = tag.getBoolean("IsWorking");
        }
        if (tag.contains("OutputTank")) {
            outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
        sideConfig.loadAdditional(tag);
    }
}
