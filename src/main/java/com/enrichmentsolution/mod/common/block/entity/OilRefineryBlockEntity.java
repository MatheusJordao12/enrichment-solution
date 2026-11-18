package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.OilRefineryBlock;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.menu.OilRefineryMenu;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Refines Crude Oil into Liquid Isobutane using Forge Energy (FE).
 * Recipe: 100 mB Crude Oil + 1,000 FE (25 ticks @ 40 FE/t) -> 50 mB Liquid Isobutane.
 */
public class OilRefineryBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    public static final int TANK_CAPACITY = 16_000;
    public static final int CRUDE_OIL_PER_OP = 100;
    public static final int ISOBUTANE_PER_OP = 50;

    private static final int ENERGY_CAPACITY = 50_000;
    private static final int MAX_RECEIVE = 2_000;
    private static final int ENERGY_PER_TICK = 40;
    private static final int PROCESS_TIME = 25;
    private static final int AUTO_PUSH_RATE = 100;

    /** Recipe snapshot for the JEI plugin. */
    public static java.util.List<com.enrichmentsolution.mod.common.compat.RefiningRecipe> getJeiRecipes() {
        return java.util.List.of(new com.enrichmentsolution.mod.common.compat.RefiningRecipe(
                new FluidStack(ModFluids.CRUDE_OIL.get(), CRUDE_OIL_PER_OP),
                new FluidStack(ModFluids.LIQUID_ISOBUTANE.get(), ISOBUTANE_PER_OP),
                ENERGY_PER_TICK,
                PROCESS_TIME));
    }

    private int progress = 0;

    private final SideConfigHolder sideConfig = SideConfigHolder.createDefault(
            SideConfigMode.NONE,
            SideConfigMode.INPUT,
            SideConfigMode.INPUT,
            SideConfigMode.OUTPUT,
            SideConfigMode.INPUT,
            SideConfigMode.INPUT
    );

    private final FluidTank inputTank = new FluidTank(TANK_CAPACITY,
            stack -> stack.getFluid() == ModFluids.CRUDE_OIL.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY,
            stack -> stack.getFluid() == ModFluids.LIQUID_ISOBUTANE.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> inputTank.getFluid();
                case 1 -> outputTank.getFluid();
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> inputTank.getCapacity();
                case 1 -> outputTank.getCapacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return switch (tank) {
                case 0 -> inputTank.isFluidValid(stack);
                case 1 -> outputTank.isFluidValid(stack);
                default -> false;
            };
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return inputTank.fill(resource, action);
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
            int inFluid = inputTank.getFluidAmount();
            int inCap = inputTank.getCapacity();
            int outFluid = outputTank.getFluidAmount();
            int outCap = outputTank.getCapacity();
            return switch (index) {
                case OilRefineryMenu.DATA_PROGRESS -> progress;
                case OilRefineryMenu.DATA_MAX_PROGRESS -> PROCESS_TIME;
                case OilRefineryMenu.DATA_ENERGY_LOW -> energy & 0xFFFF;
                case OilRefineryMenu.DATA_ENERGY_HIGH -> (energy >>> 16) & 0xFFFF;
                case OilRefineryMenu.DATA_MAX_ENERGY_LOW -> maxEnergy & 0xFFFF;
                case OilRefineryMenu.DATA_MAX_ENERGY_HIGH -> (maxEnergy >>> 16) & 0xFFFF;
                case OilRefineryMenu.DATA_INPUT_FLUID_LOW -> inFluid & 0xFFFF;
                case OilRefineryMenu.DATA_INPUT_FLUID_HIGH -> (inFluid >>> 16) & 0xFFFF;
                case OilRefineryMenu.DATA_INPUT_CAP_LOW -> inCap & 0xFFFF;
                case OilRefineryMenu.DATA_INPUT_CAP_HIGH -> (inCap >>> 16) & 0xFFFF;
                case OilRefineryMenu.DATA_OUTPUT_FLUID_LOW -> outFluid & 0xFFFF;
                case OilRefineryMenu.DATA_OUTPUT_FLUID_HIGH -> (outFluid >>> 16) & 0xFFFF;
                case OilRefineryMenu.DATA_OUTPUT_CAP_LOW -> outCap & 0xFFFF;
                case OilRefineryMenu.DATA_OUTPUT_CAP_HIGH -> (outCap >>> 16) & 0xFFFF;
                default -> {
                    if (index >= OilRefineryMenu.DATA_SIDE_BASE
                            && index < OilRefineryMenu.DATA_SIDE_BASE + RelativeSide.values().length) {
                        yield sideConfig.getMode(RelativeSide.fromOrdinal(index - OilRefineryMenu.DATA_SIDE_BASE)).ordinal();
                    }
                    yield 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == OilRefineryMenu.DATA_PROGRESS) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return OilRefineryMenu.DATA_COUNT;
        }
    };

    public OilRefineryBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.OIL_REFINERY.get(), pos, blockState, ENERGY_CAPACITY, MAX_RECEIVE, 0, 0);
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(OilRefineryBlock.FACING) ? state.getValue(OilRefineryBlock.FACING) : Direction.NORTH;
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
        return Component.translatable("block.enrichment_solution.oil_refinery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OilRefineryMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        boolean isWorking = false;
        if (canProcess() && energyStorage.getEnergyStored() >= ENERGY_PER_TICK) {
            energyStorage.consumeInternal(ENERGY_PER_TICK, false);
            progress++;
            isWorking = true;

            if (progress >= PROCESS_TIME) {
                process();
                progress = 0;
            }
            setChanged();
        } else if (progress > 0 && !canProcess()) {
            progress = 0;
            setChanged();
        }

        if (state.getValue(OilRefineryBlock.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(OilRefineryBlock.LIT, isWorking), Block.UPDATE_ALL);
        }

        pushOutputFluid(level, pos);
    }

    private boolean canProcess() {
        return inputTank.getFluidAmount() >= CRUDE_OIL_PER_OP
                && outputTank.getSpace() >= ISOBUTANE_PER_OP;
    }

    private void process() {
        inputTank.drain(CRUDE_OIL_PER_OP, IFluidHandler.FluidAction.EXECUTE);
        outputTank.fill(new FluidStack(ModFluids.LIQUID_ISOBUTANE.get(), ISOBUTANE_PER_OP), IFluidHandler.FluidAction.EXECUTE);
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
            FluidStack toSend = outputTank.drain(AUTO_PUSH_RATE, IFluidHandler.FluidAction.SIMULATE);
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
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        sideConfig.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Progress")) {
            progress = tag.getInt("Progress");
        }
        if (tag.contains("InputTank")) {
            inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        }
        if (tag.contains("OutputTank")) {
            outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
        sideConfig.loadAdditional(tag);
    }
}
