package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.GeothermalGeneratorBlock;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.enrichmentsolution.mod.common.menu.GeothermalGeneratorMenu;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Geothermal Generator / Turbine BlockEntity (Stage 2 of the Geothermal Binary Cycle).
 * Consumes Gaseous Isobutane from its input tank, generates FE (20 FE/mB, up to 50 mB/t = 1,000 FE/t),
 * pushes FE to adjacent receivers, and passes 100% of the expanded exhaust Gaseous Isobutane
 * to its output tank for closed-loop condensation.
 */
public class GeothermalGeneratorBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    private final FluidTank inputTank = new FluidTank(ModServerConfig.get(ModServerConfig.GEOTHERMAL_TANK_CAPACITY),
            stack -> stack.getFluid() == ModFluids.GASEOUS_ISOBUTANE.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final FluidTank outputTank = new FluidTank(ModServerConfig.get(ModServerConfig.GEOTHERMAL_TANK_CAPACITY),
            stack -> stack.getFluid() == ModFluids.GASEOUS_ISOBUTANE.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    /**
     * Unified fluid handler: external fills always enter inputTank,
     * external drains always extract exhaust gas from outputTank.
     */
    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? inputTank.getFluid() : outputTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return ModServerConfig.get(ModServerConfig.GEOTHERMAL_TANK_CAPACITY);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && stack.getFluid() == ModFluids.GASEOUS_ISOBUTANE.get();
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

    private int lastGeneration = 0;
    private int lastFlowRate = 0;

    private final SideConfigHolder sideConfig = SideConfigHolder.createDefault(
            SideConfigMode.NONE,
            SideConfigMode.OUTPUT,
            SideConfigMode.INPUT,
            SideConfigMode.OUTPUT,
            SideConfigMode.OUTPUT,
            SideConfigMode.OUTPUT
    );

    public GeothermalGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.GEOTHERMAL_GENERATOR.get(),
                pos,
                blockState,
                ModServerConfig.get(ModServerConfig.GEOTHERMAL_ENERGY_CAPACITY),
                0,
                ModServerConfig.get(ModServerConfig.GEOTHERMAL_MAX_EXTRACT),
                0
        );
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

    private Direction getFacing() {
        return getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    @Nullable
    @Override
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return sideConfig.wrapEnergyStorage(energyStorage, side, getFacing());
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return sideConfig.wrapFluidHandler(fluidHandler, side, getFacing());
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        int availableGas = inputTank.getFluidAmount();
        int outputSpace = outputTank.getSpace();
        int energySpace = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        int fePerMb = ModServerConfig.get(ModServerConfig.GEOTHERMAL_FE_PER_MB);
        int maxProcessMb = ModServerConfig.get(ModServerConfig.GEOTHERMAL_MAX_PROCESS_MB_PER_TICK);
        int maxByEnergy = energySpace / fePerMb;

        int toProcess = Math.min(maxProcessMb, Math.min(availableGas, Math.min(outputSpace, maxByEnergy)));

        if (toProcess > 0) {
            FluidStack drained = inputTank.drain(toProcess, IFluidHandler.FluidAction.EXECUTE);
            int actual = drained.getAmount();
            if (actual > 0) {
                outputTank.fill(new FluidStack(ModFluids.GASEOUS_ISOBUTANE.get(), actual), IFluidHandler.FluidAction.EXECUTE);
                int generated = actual * fePerMb;
                energyStorage.generateInternal(generated, false);
                lastGeneration = generated;
                lastFlowRate = actual;
            } else {
                lastGeneration = 0;
                lastFlowRate = 0;
            }
        } else {
            lastGeneration = 0;
            lastFlowRate = 0;
        }

        boolean shouldBeLit = lastFlowRate > 0;
        if (state.hasProperty(GeothermalGeneratorBlock.LIT) && state.getValue(GeothermalGeneratorBlock.LIT) != shouldBeLit) {
            level.setBlock(pos, state.setValue(GeothermalGeneratorBlock.LIT, shouldBeLit), Block.UPDATE_ALL);
        }

        pushEnergyToNeighbors(level, pos);
        pushExhaustGasToNeighbors(level, pos);
    }

    private void pushEnergyToNeighbors(Level level, BlockPos pos) {
        if (energyStorage.getEnergyStored() <= 0) {
            return;
        }
        Direction facing = getFacing();
        for (Direction direction : Direction.values()) {
            if (!sideConfig.canPushTo(direction, facing)) {
                continue;
            }
            BlockPos neighborPos = pos.relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null) {
                continue;
            }
            IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());
            if (receiver != null && receiver.canReceive()) {
                int extractable = energyStorage.extractEnergy(ModServerConfig.get(ModServerConfig.GEOTHERMAL_MAX_EXTRACT), true);
                if (extractable <= 0) {
                    return;
                }
                int accepted = receiver.receiveEnergy(extractable, false);
                if (accepted > 0) {
                    energyStorage.extractEnergy(accepted, false);
                }
            }
        }
    }

    private void pushExhaustGasToNeighbors(Level level, BlockPos pos) {
        if (outputTank.isEmpty()) {
            return;
        }
        Direction facing = getFacing();
        for (Direction direction : Direction.values()) {
            if (outputTank.isEmpty()) {
                return;
            }
            if (!sideConfig.canPushTo(direction, facing)) {
                continue;
            }
            BlockPos neighborPos = pos.relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null || neighbor instanceof GeothermalGeneratorBlockEntity) {
                continue;
            }
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, direction.getOpposite());
            if (target != null) {
                int toSend = Math.min(ModServerConfig.get(ModServerConfig.GEOTHERMAL_MAX_PROCESS_MB_PER_TICK), outputTank.getFluidAmount());
                FluidStack simulatedDrain = outputTank.drain(toSend, IFluidHandler.FluidAction.SIMULATE);
                if (!simulatedDrain.isEmpty()) {
                    int accepted = target.fill(simulatedDrain, IFluidHandler.FluidAction.EXECUTE);
                    if (accepted > 0) {
                        outputTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        sideConfig.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("InputTank")) {
            inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        }
        if (tag.contains("OutputTank")) {
            outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
        sideConfig.loadAdditional(tag);
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int stored = energyStorage.getEnergyStored();
            int max = energyStorage.getMaxEnergyStored();
            return switch (index) {
                case GeothermalGeneratorMenu.DATA_ENERGY_LOW -> stored & 0xFFFF;
                case GeothermalGeneratorMenu.DATA_ENERGY_HIGH -> (stored >>> 16) & 0xFFFF;
                case GeothermalGeneratorMenu.DATA_MAX_ENERGY_LOW -> max & 0xFFFF;
                case GeothermalGeneratorMenu.DATA_MAX_ENERGY_HIGH -> (max >>> 16) & 0xFFFF;
                case GeothermalGeneratorMenu.DATA_INPUT_GAS -> inputTank.getFluidAmount();
                case GeothermalGeneratorMenu.DATA_OUTPUT_GAS -> outputTank.getFluidAmount();
                case GeothermalGeneratorMenu.DATA_MAX_TANK -> ModServerConfig.get(ModServerConfig.GEOTHERMAL_TANK_CAPACITY);
                case GeothermalGeneratorMenu.DATA_GENERATION -> lastGeneration;
                case GeothermalGeneratorMenu.DATA_FLOW_RATE -> lastFlowRate;
                default -> {
                    if (index >= GeothermalGeneratorMenu.DATA_SIDE_START && index < GeothermalGeneratorMenu.DATA_SIDE_START + 6) {
                        yield sideConfig.getMode(RelativeSide.fromOrdinal(index - GeothermalGeneratorMenu.DATA_SIDE_START)).ordinal();
                    }
                    yield 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return GeothermalGeneratorMenu.DATA_COUNT;
        }
    };

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enrichment_solution.geothermal_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GeothermalGeneratorMenu(containerId, playerInventory, this, data);
    }
}
