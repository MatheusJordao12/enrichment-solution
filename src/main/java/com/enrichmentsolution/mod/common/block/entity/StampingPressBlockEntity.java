package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.StampingPressBlock;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.menu.StampingPressMenu;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModItems;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Stamping Press BlockEntity:
 * 100,000 FE buffer, consumes 25 FE/t over 80 ticks (4s = 2,000 FE) per operation.
 * Automatically crafts Mechanical Eyes (Pressed Aluminium + Glowstone Dust + Robot Circuit + Glass Pane)
 * or Pressed Aluminium (Aluminium Ingot).
 */
public class StampingPressBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    public static final int INPUT_SLOTS = 4;
    public static final int SLOT_OUTPUT = 4;
    public static final int TOTAL_SLOTS = INPUT_SLOTS + 1;

    public static final int ENERGY_CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 2_000;
    public static final int FE_PER_TICK = 25;
    public static final int PROCESS_TICKS = 80;

    /** Recipe snapshots for the JEI plugin (checked in this order in-game). */
    public static java.util.List<com.enrichmentsolution.mod.common.compat.PressingRecipe> getJeiRecipes() {
        return java.util.List.of(
                new com.enrichmentsolution.mod.common.compat.PressingRecipe(
                        java.util.List.of(
                                new ItemStack(ModItems.PRESSED_ALUMINIUM.get()),
                                new ItemStack(Items.GLOWSTONE_DUST),
                                new ItemStack(ModItems.ROBOT_CIRCUIT.get()),
                                new ItemStack(Items.GLASS_PANE)),
                        new ItemStack(ModItems.MECHANICAL_EYES.get()),
                        FE_PER_TICK, PROCESS_TICKS),
                new com.enrichmentsolution.mod.common.compat.PressingRecipe(
                        java.util.List.of(new ItemStack(ModItems.ALUMINIUM_INGOT.get())),
                        new ItemStack(ModItems.PRESSED_ALUMINIUM.get()),
                        FE_PER_TICK, PROCESS_TICKS));
    }

    private enum RecipeType {
        NONE,
        MECHANICAL_EYES,
        PRESSED_ALUMINIUM
    }

    private int progress = 0;
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
        RecipeType recipe = matchRecipe();
        boolean wasLit = state.getValue(StampingPressBlock.LIT);
        boolean isWorking = false;

        if (recipe != RecipeType.NONE && canAcceptOutput(getResultFor(recipe))
                && energyStorage.getEnergyStored() >= FE_PER_TICK) {
            energyStorage.consumeInternal(FE_PER_TICK, false);
            progress++;
            isWorking = true;

            if (progress >= PROCESS_TICKS) {
                consumeInputs(recipe);
                itemHandler.insertItem(SLOT_OUTPUT, getResultFor(recipe), false);
                progress = 0;
            }
            setChanged();
        } else if (recipe == RecipeType.NONE && progress > 0) {
            progress = 0;
            setChanged();
        }

        if (wasLit != isWorking) {
            level.setBlock(pos, state.setValue(StampingPressBlock.LIT, isWorking), Block.UPDATE_ALL);
        }
    }

    private RecipeType matchRecipe() {
        if (hasItem(ModItems.PRESSED_ALUMINIUM.get())
                && hasItem(Items.GLOWSTONE_DUST)
                && hasItem(ModItems.ROBOT_CIRCUIT.get())
                && hasItem(Items.GLASS_PANE)) {
            return RecipeType.MECHANICAL_EYES;
        }
        if (hasItem(ModItems.ALUMINIUM_INGOT.get())) {
            return RecipeType.PRESSED_ALUMINIUM;
        }
        return RecipeType.NONE;
    }

    private boolean hasItem(Item item) {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private void consumeOne(Item item) {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(item)) {
                itemHandler.extractItem(i, 1, false);
                return;
            }
        }
    }

    private void consumeInputs(RecipeType recipe) {
        if (recipe == RecipeType.MECHANICAL_EYES) {
            consumeOne(ModItems.PRESSED_ALUMINIUM.get());
            consumeOne(Items.GLOWSTONE_DUST);
            consumeOne(ModItems.ROBOT_CIRCUIT.get());
            consumeOne(Items.GLASS_PANE);
        } else if (recipe == RecipeType.PRESSED_ALUMINIUM) {
            consumeOne(ModItems.ALUMINIUM_INGOT.get());
        }
    }

    private ItemStack getResultFor(RecipeType recipe) {
        return switch (recipe) {
            case MECHANICAL_EYES -> new ItemStack(ModItems.MECHANICAL_EYES.get(), 1);
            case PRESSED_ALUMINIUM -> new ItemStack(ModItems.PRESSED_ALUMINIUM.get(), 1);
            case NONE -> ItemStack.EMPTY;
        };
    }

    private boolean canAcceptOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
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
                case StampingPressMenu.DATA_MAX_PROGRESS -> PROCESS_TICKS;
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
