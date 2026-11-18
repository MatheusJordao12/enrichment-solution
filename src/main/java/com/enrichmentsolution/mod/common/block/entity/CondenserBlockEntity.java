package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.CondenserBlock;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.menu.CondenserMenu;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Condenser BlockEntity — Stage 3 of the Geothermal Binary Cycle (closed-loop Option A).
 * Condenses exhaust Gaseous Isobutane back into Liquid Isobutane (1:1 mB, zero loss)
 * using cooling Water (1 mB water per 5 mB gas), and pushes Liquid Isobutane to adjacent handlers.
 */
public class CondenserBlockEntity extends BlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    public static final int TANK_CAPACITY = 16_000;
    public static final int MAX_CONDENSE_PER_TICK = 50;
    public static final int MAX_PUSH_PER_TICK = 200;

    private final FluidTank gasInputTank = new FluidTank(TANK_CAPACITY,
            stack -> stack.getFluid().isSame(ModFluids.GASEOUS_ISOBUTANE.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final FluidTank waterTank = new FluidTank(TANK_CAPACITY,
            stack -> stack.getFluid().isSame(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final FluidTank liquidOutputTank = new FluidTank(TANK_CAPACITY,
            stack -> stack.getFluid().isSame(ModFluids.LIQUID_ISOBUTANE.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int condenseRate = 0;

    private final SideConfigHolder sideConfig = SideConfigHolder.createDefault(
            SideConfigMode.NONE,
            SideConfigMode.INPUT,
            SideConfigMode.INPUT,
            SideConfigMode.OUTPUT,
            SideConfigMode.INPUT,
            SideConfigMode.INPUT
    );

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> gasInputTank.getFluid();
                case 1 -> waterTank.getFluid();
                case 2 -> liquidOutputTank.getFluid();
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return TANK_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return switch (tank) {
                case 0 -> gasInputTank.isFluidValid(stack);
                case 1 -> waterTank.isFluidValid(stack);
                case 2 -> liquidOutputTank.isFluidValid(stack);
                default -> false;
            };
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            if (resource.getFluid().isSame(ModFluids.GASEOUS_ISOBUTANE.get())) {
                return gasInputTank.fill(resource, action);
            }
            if (resource.getFluid().isSame(Fluids.WATER)) {
                return waterTank.fill(resource, action);
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !resource.getFluid().isSame(ModFluids.LIQUID_ISOBUTANE.get())) {
                return FluidStack.EMPTY;
            }
            return liquidOutputTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return liquidOutputTank.drain(maxDrain, action);
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case CondenserMenu.DATA_GAS_AMOUNT -> gasInputTank.getFluidAmount();
                case CondenserMenu.DATA_GAS_CAPACITY -> gasInputTank.getCapacity();
                case CondenserMenu.DATA_WATER_AMOUNT -> waterTank.getFluidAmount();
                case CondenserMenu.DATA_WATER_CAPACITY -> waterTank.getCapacity();
                case CondenserMenu.DATA_LIQUID_AMOUNT -> liquidOutputTank.getFluidAmount();
                case CondenserMenu.DATA_LIQUID_CAPACITY -> liquidOutputTank.getCapacity();
                case CondenserMenu.DATA_CONDENSE_RATE -> condenseRate;
                default -> {
                    if (index >= CondenserMenu.DATA_SIDE_START && index < CondenserMenu.DATA_SIDE_START + 6) {
                        yield sideConfig.getMode(RelativeSide.fromOrdinal(index - CondenserMenu.DATA_SIDE_START)).ordinal();
                    }
                    yield 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {
            // Synced server -> client only
        }

        @Override
        public int getCount() {
            return CondenserMenu.DATA_COUNT;
        }
    };

    public CondenserBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CONDENSER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CondenserBlockEntity be) {
        if (level.isClientSide) {
            return;
        }

        int gasAvail = be.gasInputTank.getFluidAmount();
        int liquidSpace = be.liquidOutputTank.getSpace();
        int waterAvail = be.waterTank.getFluidAmount();

        int canCondense = Math.min(MAX_CONDENSE_PER_TICK, Math.min(gasAvail, liquidSpace));
        int maxByWater = waterAvail * 5;
        int toCondense = Math.min(canCondense, maxByWater);

        if (toCondense > 0) {
            int waterToConsume = Math.max(1, (toCondense + 4) / 5);
            be.gasInputTank.drain(toCondense, IFluidHandler.FluidAction.EXECUTE);
            be.waterTank.drain(waterToConsume, IFluidHandler.FluidAction.EXECUTE);
            be.liquidOutputTank.fill(
                    new FluidStack(ModFluids.LIQUID_ISOBUTANE.get(), toCondense),
                    IFluidHandler.FluidAction.EXECUTE
            );
            be.condenseRate = toCondense;
        } else {
            be.condenseRate = 0;
        }

        // Push condensed Liquid Isobutane to adjacent fluid handlers (Heat Exchanger or Fluid Pipes)
        if (!be.liquidOutputTank.isEmpty()) {
            Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : Direction.NORTH;
            for (Direction dir : Direction.values()) {
                if (be.liquidOutputTank.isEmpty()) {
                    break;
                }
                if (!be.sideConfig.canPushTo(dir, facing)) {
                    continue;
                }
                BlockPos neighborPos = pos.relative(dir);
                IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
                if (target != null) {
                    int toPush = Math.min(MAX_PUSH_PER_TICK, be.liquidOutputTank.getFluidAmount());
                    FluidStack pushStack = new FluidStack(ModFluids.LIQUID_ISOBUTANE.get(), toPush);
                    int accepted = target.fill(pushStack, IFluidHandler.FluidAction.EXECUTE);
                    if (accepted > 0) {
                        be.liquidOutputTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }

        boolean active = be.condenseRate > 0;
        if (state.getValue(CondenserBlock.LIT) != active) {
            level.setBlock(pos, state.setValue(CondenserBlock.LIT, active), Block.UPDATE_ALL);
        }
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

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        Direction facing = getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        return sideConfig.wrapFluidHandler(fluidHandler, side, facing);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enrichment_solution.condenser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CondenserMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("GasInputTank", gasInputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("LiquidOutputTank", liquidOutputTank.writeToNBT(registries, new CompoundTag()));
        sideConfig.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("GasInputTank")) {
            gasInputTank.readFromNBT(registries, tag.getCompound("GasInputTank"));
        }
        if (tag.contains("WaterTank")) {
            waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        }
        if (tag.contains("LiquidOutputTank")) {
            liquidOutputTank.readFromNBT(registries, tag.getCompound("LiquidOutputTank"));
        }
        sideConfig.loadAdditional(tag);
    }
}
