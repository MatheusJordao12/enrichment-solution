package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.PelletizerControllerBlock;
import com.enrichmentsolution.mod.common.block.SinteringHeaterBlock;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.menu.PelletizerMenu;
import com.enrichmentsolution.mod.common.recipe.MachineRecipe;
import com.enrichmentsolution.mod.common.recipe.MachineRecipeCache;
import com.enrichmentsolution.mod.common.recipe.MachineRecipeKind;
import com.enrichmentsolution.mod.common.recipe.MachineRecipeMatcher;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModBlocks;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for the 3x3x4 Pelletizer Multiblock Controller.
 * Processes {@link MachineRecipeKind#PELLETIZING} recipes when the 3x3x4 structure is valid.
 */
public class PelletizerControllerBlockEntity extends AbstractMachineBlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    public static final int ENERGY_CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int DEFAULT_PROCESS_TIME = 100;

    private final MachineRecipeCache recipes = new MachineRecipeCache(MachineRecipeKind.PELLETIZING);

    private int progress = 0;
    private boolean structureValid = false;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PelletizerMenu.DATA_PROGRESS -> progress;
                case PelletizerMenu.DATA_MAX_PROGRESS -> getMaxProgress();
                case PelletizerMenu.DATA_ENERGY_LOW -> energyStorage.getEnergyStored() & 0xFFFF;
                case PelletizerMenu.DATA_ENERGY_HIGH -> (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case PelletizerMenu.DATA_MAX_ENERGY_LOW -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case PelletizerMenu.DATA_MAX_ENERGY_HIGH -> (energyStorage.getMaxEnergyStored() >>> 16) & 0xFFFF;
                case PelletizerMenu.DATA_STRUCTURE_VALID -> isStructureValid() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == PelletizerMenu.DATA_PROGRESS) {
                progress = value;
            } else if (index == PelletizerMenu.DATA_STRUCTURE_VALID) {
                structureValid = value != 0;
            }
        }

        @Override
        public int getCount() {
            return PelletizerMenu.DATA_COUNT;
        }
    };

    public PelletizerControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.PELLETIZER_CONTROLLER.get(),
                pos,
                blockState,
                ENERGY_CAPACITY,
                MAX_RECEIVE,
                0,
                2
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.enrichment_solution.pelletizer_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PelletizerMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        boolean valid = validateStructure(level, pos, state);
        this.structureValid = valid;

        boolean isWorking = false;
        if (valid) {
            RecipeHolder<MachineRecipe> holder = findRecipe(itemHandler.getStackInSlot(SLOT_INPUT));
            MachineRecipe recipe = holder == null ? null : holder.value();
            int energyCost = recipe == null ? 0 : recipe.energyPerTick();

            if (canProcess(recipe) && energyStorage.getEnergyStored() >= energyCost) {
                energyStorage.consumeInternal(energyCost, false);
                progress++;
                isWorking = true;

                if (progress >= recipe.ticks()) {
                    process(recipe);
                    progress = 0;
                }
                setChanged();
            } else if (progress > 0 && !canProcess(recipe)) {
                progress = 0;
                setChanged();
            }
        } else if (progress > 0) {
            progress = 0;
            setChanged();
        }

        if (state.getValue(PelletizerControllerBlock.ACTIVE) != isWorking) {
            level.setBlock(pos, state.setValue(PelletizerControllerBlock.ACTIVE, isWorking), Block.UPDATE_ALL);
        }
        setHeaterActive(level, pos, state, isWorking);
    }

    public void setHeaterActive(Level level, BlockPos controllerPos, BlockState controllerState, boolean active) {
        Direction facing = controllerState.hasProperty(PelletizerControllerBlock.FACING)
                ? controllerState.getValue(PelletizerControllerBlock.FACING)
                : Direction.NORTH;
        BlockPos heaterPos = controllerPos.relative(facing.getOpposite(), 3).above(2);
        BlockState heaterState = level.getBlockState(heaterPos);
        if (heaterState.getBlock() instanceof SinteringHeaterBlock
                && heaterState.getValue(SinteringHeaterBlock.ACTIVE) != active) {
            level.setBlock(heaterPos, heaterState.setValue(SinteringHeaterBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    public boolean isStructureValid() {
        if (level == null) {
            return structureValid;
        }
        return validateStructure(level, worldPosition, getBlockState());
    }

    @Nullable
    public Component getMissingBlockMessage() {
        if (level == null) {
            return null;
        }
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(PelletizerControllerBlock.FACING)
                ? state.getValue(PelletizerControllerBlock.FACING)
                : Direction.NORTH;
        Direction backDir = facing.getOpposite();
        Direction rightDir = facing.getClockWise();

        for (int back = 0; back <= 3; back++) {
            for (int y = 0; y <= 2; y++) {
                for (int x = -1; x <= 1; x++) {
                    Block expected = expectedBlockAt(x, y, back);
                    BlockPos checkPos = worldPosition.relative(rightDir, x).relative(backDir, back).above(y);
                    if (!level.getBlockState(checkPos).is(expected)) {
                        return Component.literal("Missing " + expected.getName().getString()
                                + " at (" + checkPos.getX() + ", " + checkPos.getY() + ", " + checkPos.getZ() + ")");
                    }
                }
            }
        }
        return null;
    }

    private boolean validateStructure(Level level, BlockPos controllerPos, BlockState controllerState) {
        Direction facing = controllerState.hasProperty(PelletizerControllerBlock.FACING)
                ? controllerState.getValue(PelletizerControllerBlock.FACING)
                : Direction.NORTH;
        Direction backDir = facing.getOpposite();
        Direction rightDir = facing.getClockWise();

        for (int back = 0; back <= 3; back++) {
            for (int y = 0; y <= 2; y++) {
                for (int x = -1; x <= 1; x++) {
                    Block expected = expectedBlockAt(x, y, back);
                    BlockPos checkPos = controllerPos.relative(rightDir, x).relative(backDir, back).above(y);
                    if (!level.getBlockState(checkPos).is(expected)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static Block expectedBlockAt(int x, int y, int back) {
        if (x == 0 && y == 0 && back == 0) {
            return ModBlocks.PELLETIZER_CONTROLLER.get();
        }
        if (x == 0 && y == 2 && back == 1) {
            return ModBlocks.CRUSHER_MODULE.get();
        }
        if (x == 0 && y == 2 && back == 2) {
            return ModBlocks.HYDRAULIC_PRESS_HEAD.get();
        }
        if (x == 0 && y == 2 && back == 3) {
            return ModBlocks.SINTERING_HEATER.get();
        }
        return ModBlocks.MACHINE_CASING.get();
    }

    @Nullable
    private RecipeHolder<MachineRecipe> findRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return null;
        }
        return recipes.find(level, recipe ->
                recipe.hasItemInputs()
                        && recipe.itemInputs().size() == 1
                        && recipe.hasItemOutput()
                        && recipe.itemInputs().get(0).matches(input));
    }

    private boolean canProcess(@Nullable MachineRecipe recipe) {
        return recipe != null
                && MachineRecipeMatcher.canAcceptOutput(itemHandler.getStackInSlot(SLOT_OUTPUT), recipe.itemOutput());
    }

    private void process(MachineRecipe recipe) {
        itemHandler.extractItem(SLOT_INPUT, recipe.itemInputs().get(0).count(), false);
        itemHandler.insertItem(SLOT_OUTPUT, recipe.itemOutput().copy(), false);
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        RecipeHolder<MachineRecipe> holder = findRecipe(itemHandler.getStackInSlot(SLOT_INPUT));
        return holder == null ? DEFAULT_PROCESS_TIME : holder.value().ticks();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
    }
}
