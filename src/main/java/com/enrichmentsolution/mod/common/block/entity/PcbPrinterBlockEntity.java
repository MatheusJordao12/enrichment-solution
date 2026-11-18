package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.block.PcbPrinterBlock;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.menu.PcbPrinterMenu;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * PCB Printer BlockEntity: 100 FE buffer, prints Robot Circuits and tiered Circuits from 3 unordered input slots.
 * The recipes come from the datapack ({@code enrichment_solution:pcb_printing}); the pending operation remembers the
 * recipe id so it survives save/load and datapack reloads.
 */
public class PcbPrinterBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    public static final int SLOT_INPUT_0 = 0;
    public static final int SLOT_INPUT_1 = 1;
    public static final int SLOT_INPUT_2 = 2;
    public static final int INPUT_SLOTS = 3;
    public static final int SLOT_OUTPUT = 3;
    public static final int TOTAL_SLOTS = 4;

    public static final int ENERGY_CAPACITY = 100;
    public static final int MAX_RECEIVE = 2_000;

    /** GUI fallback used while no printing recipe is pending. */
    private static final int DEFAULT_PRINT_TICKS = 100;

    /** Legacy saves stored the bare recipe key, without namespace or folder. */
    private static final String LEGACY_RECIPE_PREFIX = "pcb/";

    private final MachineRecipeCache recipes = new MachineRecipeCache(MachineRecipeKind.PCB_PRINTING);

    private boolean printing = false;
    private int progress = 0;
    @Nullable
    private ResourceLocation pendingRecipeId;
    private final SideConfigHolder sideConfig = SideConfigHolder.allInput();

    public PcbPrinterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.PCB_PRINTER.get(), pos, blockState, ENERGY_CAPACITY, MAX_RECEIVE, 0, TOTAL_SLOTS);
    }

    @Override
    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        Direction facing = getBlockState().hasProperty(PcbPrinterBlock.FACING)
                ? getBlockState().getValue(PcbPrinterBlock.FACING)
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

    public boolean isPrinting() {
        return printing;
    }

    public boolean hasOutputCircuit() {
        return !itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty();
    }

    public boolean canStartPrinting() {
        if (printing) {
            return false;
        }
        RecipeHolder<MachineRecipe> holder = findMatchingRecipe();
        if (holder == null) {
            return false;
        }
        MachineRecipe recipe = holder.value();
        if (energyStorage.getEnergyStored() < recipe.energyPerTick()) {
            return false;
        }
        return MachineRecipeMatcher.canAcceptOutput(itemHandler.getStackInSlot(SLOT_OUTPUT), recipe.itemOutput());
    }

    @Nullable
    private RecipeHolder<MachineRecipe> findMatchingRecipe() {
        if (level == null) {
            return null;
        }
        return recipes.find(level, recipe -> recipe.hasItemInputs()
                && recipe.hasItemOutput()
                && recipe.itemInputs().size() <= INPUT_SLOTS
                && MachineRecipeMatcher.matchSlots(recipe, itemHandler::getStackInSlot, INPUT_SLOTS) != null);
    }

    @Nullable
    private MachineRecipe getPendingRecipe() {
        if (level == null) {
            return null;
        }
        RecipeHolder<MachineRecipe> holder = recipes.byId(level, this.pendingRecipeId);
        return holder == null ? null : holder.value();
    }

    public boolean startPrinting() {
        if (level == null || level.isClientSide || printing) {
            return false;
        }
        RecipeHolder<MachineRecipe> holder = findMatchingRecipe();
        if (holder == null) {
            return false;
        }
        MachineRecipe recipe = holder.value();
        if (!MachineRecipeMatcher.canAcceptOutput(itemHandler.getStackInSlot(SLOT_OUTPUT), recipe.itemOutput())) {
            return false;
        }
        if (energyStorage.getEnergyStored() < recipe.energyPerTick()) {
            return false;
        }
        int[] matchedSlots = MachineRecipeMatcher.matchSlots(recipe, itemHandler::getStackInSlot, INPUT_SLOTS);
        if (matchedSlots == null) {
            return false;
        }
        for (int i = 0; i < matchedSlots.length; i++) {
            itemHandler.extractItem(matchedSlots[i], recipe.itemInputs().get(i).count(), false);
        }
        this.pendingRecipeId = holder.id();
        printing = true;
        progress = 0;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        if (!printing) {
            return;
        }
        MachineRecipe recipe = getPendingRecipe();
        if (recipe == null) {
            // The recipe disappeared (datapack change): abort instead of producing a wrong output.
            printing = false;
            progress = 0;
            pendingRecipeId = null;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            return;
        }
        if (!MachineRecipeMatcher.canAcceptOutput(itemHandler.getStackInSlot(SLOT_OUTPUT), recipe.itemOutput())) {
            return;
        }
        if (energyStorage.getEnergyStored() < recipe.energyPerTick()) {
            return;
        }
        energyStorage.consumeInternal(recipe.energyPerTick(), false);
        progress++;
        if (progress >= recipe.ticks()) {
            itemHandler.insertItem(SLOT_OUTPUT, recipe.itemOutput().copy(), false);
            printing = false;
            progress = 0;
            pendingRecipeId = null;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        } else {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Printing", printing);
        tag.putInt("Progress", progress);
        tag.putString("PendingOutput", pendingRecipeId == null ? "" : pendingRecipeId.toString());
        sideConfig.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        printing = tag.getBoolean("Printing");
        progress = tag.getInt("Progress");
        if (tag.contains("PendingOutput")) {
            pendingRecipeId = parsePendingRecipeId(tag.getString("PendingOutput"));
        } else if (printing) {
            pendingRecipeId = parsePendingRecipeId(LEGACY_RECIPE_PREFIX + "robot_circuit");
        } else {
            pendingRecipeId = null;
        }
        sideConfig.loadAdditional(tag);
    }

    /** Accepts the current {@code namespace:path} format and the legacy bare recipe key ("robot_circuit"). */
    @Nullable
    private static ResourceLocation parsePendingRecipeId(String value) {
        if (value.isEmpty()) {
            return null;
        }
        if (value.indexOf(':') < 0) {
            return ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, LEGACY_RECIPE_PREFIX + value);
        }
        return ResourceLocation.tryParse(value);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Printing", printing);
        tag.putInt("Progress", progress);
        tag.putString("PendingOutput", pendingRecipeId == null ? "" : pendingRecipeId.toString());
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= PcbPrinterMenu.DATA_SIDE_BASE && index < PcbPrinterMenu.DATA_COUNT) {
                return sideConfig.getMode(RelativeSide.fromOrdinal(index - PcbPrinterMenu.DATA_SIDE_BASE)).ordinal();
            }
            return switch (index) {
                case PcbPrinterMenu.DATA_ENERGY -> energyStorage.getEnergyStored();
                case PcbPrinterMenu.DATA_MAX_ENERGY -> energyStorage.getMaxEnergyStored();
                case PcbPrinterMenu.DATA_PROGRESS -> progress;
                case PcbPrinterMenu.DATA_MAX_PROGRESS -> {
                    MachineRecipe pending = getPendingRecipe();
                    yield pending == null ? DEFAULT_PRINT_TICKS : pending.ticks();
                }
                case PcbPrinterMenu.DATA_PRINTING -> printing ? 1 : 0;
                case PcbPrinterMenu.DATA_CAN_START -> canStartPrinting() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= PcbPrinterMenu.DATA_SIDE_BASE && index < PcbPrinterMenu.DATA_COUNT) {
                sideConfig.setMode(RelativeSide.fromOrdinal(index - PcbPrinterMenu.DATA_SIDE_BASE), SideConfigMode.fromOrdinal(value));
                return;
            }
            switch (index) {
                case PcbPrinterMenu.DATA_PROGRESS -> progress = value;
                case PcbPrinterMenu.DATA_PRINTING -> printing = value != 0;
            }
        }

        @Override
        public int getCount() {
            return PcbPrinterMenu.DATA_COUNT;
        }
    };

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PcbPrinterMenu(containerId, playerInventory, this, data);
    }
}
