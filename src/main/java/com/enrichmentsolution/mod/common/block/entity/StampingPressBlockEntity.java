package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.StampingPressBlock;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.menu.StampingPressMenu;
import com.enrichmentsolution.mod.common.recipe.MachineRecipe;
import com.enrichmentsolution.mod.common.recipe.MachineRecipeCache;
import com.enrichmentsolution.mod.common.recipe.MachineRecipeKind;
import com.enrichmentsolution.mod.common.recipe.MachineRecipeMatcher;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
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
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Stamping Press BlockEntity.
 * 100,000 FE buffer; the operation cost and duration come from the datapack recipes
 * ({@code enrichment_solution:stamping}). Inputs are matched unordered across the four input slots and the most
 * specific recipe (more item inputs) wins, so Mechanical Eyes are preferred over single-ingot Pressed Aluminium.
 */
public class StampingPressBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    public static final int INPUT_SLOTS = 4;
    public static final int SLOT_OUTPUT = 4;
    public static final int TOTAL_SLOTS = INPUT_SLOTS + 1;

    public static final int ENERGY_CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 2_000;

    /** GUI fallback used while no stamping recipe matches the input slots. */
    private static final int DEFAULT_PROCESS_TICKS = 80;

    private final MachineRecipeCache recipes = new MachineRecipeCache(MachineRecipeKind.STAMPING);

    private int progress = 0;
    @Nullable
    private MachineRecipe activeRecipe;
    private final SideConfigHolder sideConfig = SideConfigHolder.allInput();

    public StampingPressBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.STAMPING_PRESS.get(), pos, blockState, ENERGY_CAPACITY, MAX_RECEIVE, 0, TOTAL_SLOTS);
    }

    @Override
    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        Direction facing = getBlockState().hasProperty(StampingPressBlock.FACING)
                ? getBlockState().getValue(StampingPressBlock.FACING)
                : Direction.NORTH;
        return sideConfig.wrapEnergyStorage(energyStorage, side, facing);
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
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        RecipeHolder<MachineRecipe> holder = findRecipe();
        MachineRecipe recipe = holder == null ? null : holder.value();
        this.activeRecipe = recipe;

        boolean wasLit = state.getValue(StampingPressBlock.LIT);
        boolean isWorking = false;

        if (recipe != null
                && MachineRecipeMatcher.canAcceptOutput(itemHandler.getStackInSlot(SLOT_OUTPUT), recipe.itemOutput())
                && energyStorage.getEnergyStored() >= recipe.energyPerTick()) {
            energyStorage.consumeInternal(recipe.energyPerTick(), false);
            progress++;
            isWorking = true;

            if (progress >= recipe.ticks()) {
                completeOperation(recipe);
                progress = 0;
            }
            setChanged();
        } else if (recipe == null && progress > 0) {
            progress = 0;
            setChanged();
        }

        if (wasLit != isWorking) {
            level.setBlock(pos, state.setValue(StampingPressBlock.LIT, isWorking), Block.UPDATE_ALL);
        }
    }

    private void completeOperation(MachineRecipe recipe) {
        int[] slots = MachineRecipeMatcher.matchSlots(recipe, itemHandler::getStackInSlot, INPUT_SLOTS);
        if (slots == null) {
            return;
        }
        for (int i = 0; i < slots.length; i++) {
            itemHandler.extractItem(slots[i], recipe.itemInputs().get(i).count(), false);
        }
        itemHandler.insertItem(SLOT_OUTPUT, recipe.itemOutput().copy(), false);
    }

    @Nullable
    private RecipeHolder<MachineRecipe> findRecipe() {
        if (level == null) {
            return null;
        }
        return recipes.find(level, recipe -> recipe.hasItemInputs()
                && recipe.hasItemOutput()
                && recipe.itemInputs().size() <= INPUT_SLOTS
                && MachineRecipeMatcher.matchSlots(recipe, itemHandler::getStackInSlot, INPUT_SLOTS) != null);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
        sideConfig.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        sideConfig.loadAdditional(tag);
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= StampingPressMenu.DATA_SIDE_BASE && index < StampingPressMenu.DATA_COUNT) {
                return sideConfig.getMode(RelativeSide.fromOrdinal(index - StampingPressMenu.DATA_SIDE_BASE)).ordinal();
            }
            return switch (index) {
                case StampingPressMenu.DATA_ENERGY_LOW -> energyStorage.getEnergyStored() & 0xFFFF;
                case StampingPressMenu.DATA_ENERGY_HIGH -> (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case StampingPressMenu.DATA_MAX_ENERGY_LOW -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case StampingPressMenu.DATA_MAX_ENERGY_HIGH -> (energyStorage.getMaxEnergyStored() >>> 16) & 0xFFFF;
                case StampingPressMenu.DATA_PROGRESS -> progress;
                case StampingPressMenu.DATA_MAX_PROGRESS ->
                        activeRecipe == null ? DEFAULT_PROCESS_TICKS : activeRecipe.ticks();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == StampingPressMenu.DATA_PROGRESS) {
                progress = value;
            } else if (index >= StampingPressMenu.DATA_SIDE_BASE && index < StampingPressMenu.DATA_COUNT) {
                sideConfig.setMode(RelativeSide.fromOrdinal(index - StampingPressMenu.DATA_SIDE_BASE), SideConfigMode.fromOrdinal(value));
            }
        }

        @Override
        public int getCount() {
            return StampingPressMenu.DATA_COUNT;
        }
    };

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StampingPressMenu(containerId, playerInventory, this, data);
    }
}
