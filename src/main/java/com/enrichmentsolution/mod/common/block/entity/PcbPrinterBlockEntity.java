package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.PcbPrinterBlock;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.menu.PcbPrinterMenu;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

import java.util.List;
import java.util.function.Supplier;

/**
 * PCB Printer BlockEntity: 100 FE buffer, consumes 20 FE/tick over 100 ticks (5s = 2,000 FE)
 * to print Robot Circuits and tiered Circuits from 3 unordered input slots.
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
    public static final int FE_PER_TICK = 20;
    public static final int PRINT_TICKS = 100;

    private record IngredientReq(Supplier<? extends Item> item, int count) {}

    private record PcbRecipe(String id, Supplier<? extends Item> output, List<IngredientReq> ingredients) {}

    private static final List<PcbRecipe> RECIPES = List.of(
            new PcbRecipe("robot_circuit", ModItems.ROBOT_CIRCUIT, List.of(
                    new IngredientReq(ModItems.STEEL_INGOT, 2),
                    new IngredientReq(ModItems.ALUMINIUM_INGOT, 2),
                    new IngredientReq(() -> Items.REDSTONE, 2)
            )),
            new PcbRecipe("basic_circuit", ModItems.BASIC_CIRCUIT, List.of(
                    new IngredientReq(() -> Items.REDSTONE, 1),
                    new IngredientReq(() -> Items.IRON_INGOT, 1),
                    new IngredientReq(ModItems.ALUMINIUM_INGOT, 1)
            )),
            new PcbRecipe("advanced_circuit", ModItems.ADVANCED_CIRCUIT, List.of(
                    new IngredientReq(() -> Items.GOLD_INGOT, 1),
                    new IngredientReq(ModItems.ALUMINIUM_INGOT, 1),
                    new IngredientReq(ModItems.BASIC_CIRCUIT, 1)
            )),
            new PcbRecipe("ultimate_circuit", ModItems.ULTIMATE_CIRCUIT, List.of(
                    new IngredientReq(() -> Items.DIAMOND, 1),
                    new IngredientReq(ModItems.ADVANCED_CIRCUIT, 1),
                    new IngredientReq(() -> Items.GOLD_INGOT, 1)
            )),
            new PcbRecipe("ultra_circuit", ModItems.ULTRA_CIRCUIT, List.of(
                    new IngredientReq(() -> Items.DIAMOND, 1),
                    new IngredientReq(ModItems.ULTIMATE_CIRCUIT, 1),
                    new IngredientReq(() -> Items.NETHERITE_INGOT, 1)
            ))
    );

    /** Recipe snapshots for the JEI plugin. */
    public static List<com.enrichmentsolution.mod.common.compat.PcbPrintingRecipe> getPrintingRecipes() {
        List<com.enrichmentsolution.mod.common.compat.PcbPrintingRecipe> recipes = new java.util.ArrayList<>();
        for (PcbRecipe recipe : RECIPES) {
            List<ItemStack> inputs = new java.util.ArrayList<>();
            for (IngredientReq requirement : recipe.ingredients()) {
                inputs.add(new ItemStack(requirement.item().get(), requirement.count()));
            }
            recipes.add(new com.enrichmentsolution.mod.common.compat.PcbPrintingRecipe(
                    new ItemStack(recipe.output().get()), inputs));
        }
        return recipes;
    }

    private boolean printing = false;
    private int progress = 0;
    private String pendingOutputKey = "";
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
        if (energyStorage.getEnergyStored() < FE_PER_TICK) {
            return false;
        }
        PcbRecipe recipe = findMatchingRecipe();
        return recipe != null && canAcceptOutput(recipe.output().get());
    }

    @Nullable
    private PcbRecipe findMatchingRecipe() {
        for (PcbRecipe recipe : RECIPES) {
            if (matchRecipeSlots(recipe) != null) {
                return recipe;
            }
        }
        return null;
    }

    private int @Nullable [] matchRecipeSlots(PcbRecipe recipe) {
        List<IngredientReq> ingredients = recipe.ingredients();
        int[] matchedSlots = new int[ingredients.size()];
        boolean[] slotUsed = new boolean[INPUT_SLOTS];

        for (int i = 0; i < ingredients.size(); i++) {
            IngredientReq req = ingredients.get(i);
            int foundSlot = -1;
            for (int slot = 0; slot < INPUT_SLOTS; slot++) {
                if (slotUsed[slot]) {
                    continue;
                }
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (stack.is(req.item().get()) && stack.getCount() >= req.count()) {
                    foundSlot = slot;
                    break;
                }
            }
            if (foundSlot < 0) {
                return null;
            }
            slotUsed[foundSlot] = true;
            matchedSlots[i] = foundSlot;
        }
        return matchedSlots;
    }

    private boolean canAcceptOutput(Item outputItem) {
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        return output.is(outputItem) && output.getCount() < output.getMaxStackSize();
    }

    private Item getPendingOutputItem() {
        for (PcbRecipe recipe : RECIPES) {
            if (recipe.id().equals(pendingOutputKey)) {
                return recipe.output().get();
            }
        }
        return ModItems.ROBOT_CIRCUIT.get();
    }

    public boolean startPrinting() {
        if (level == null || level.isClientSide || printing || energyStorage.getEnergyStored() < FE_PER_TICK) {
            return false;
        }
        PcbRecipe recipe = findMatchingRecipe();
        if (recipe == null || !canAcceptOutput(recipe.output().get())) {
            return false;
        }
        int[] matchedSlots = matchRecipeSlots(recipe);
        if (matchedSlots == null) {
            return false;
        }
        List<IngredientReq> ingredients = recipe.ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            itemHandler.extractItem(matchedSlots[i], ingredients.get(i).count(), false);
        }
        pendingOutputKey = recipe.id();
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
        Item outputItem = getPendingOutputItem();
        if (!canAcceptOutput(outputItem)) {
            return;
        }
        if (energyStorage.getEnergyStored() < FE_PER_TICK) {
            return;
        }
        energyStorage.consumeInternal(FE_PER_TICK, false);
        progress++;
        if (progress >= PRINT_TICKS) {
            itemHandler.insertItem(SLOT_OUTPUT, new ItemStack(outputItem, 1), false);
            printing = false;
            progress = 0;
            pendingOutputKey = "";
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
        tag.putString("PendingOutput", pendingOutputKey);
        sideConfig.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        printing = tag.getBoolean("Printing");
        progress = tag.getInt("Progress");
        if (tag.contains("PendingOutput")) {
            pendingOutputKey = tag.getString("PendingOutput");
        } else if (printing) {
            pendingOutputKey = "robot_circuit";
        } else {
            pendingOutputKey = "";
        }
        sideConfig.loadAdditional(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Printing", printing);
        tag.putInt("Progress", progress);
        tag.putString("PendingOutput", pendingOutputKey);
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
                case PcbPrinterMenu.DATA_MAX_PROGRESS -> PRINT_TICKS;
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
