package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.EnrichmentChamberBlock;
import com.enrichmentsolution.mod.common.block.EnrichmentChamberBlock.QuadRole;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.enrichmentsolution.mod.common.menu.EnrichmentChamberMenu;
import com.enrichmentsolution.mod.common.menu.QuadEnrichmentMenu;
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
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Converts Raw Uranium into Enriched Uranium using Forge Energy (FE). The conversion itself is a datapack recipe
 * ({@code enrichment_solution:enrichment}).
 * Single mode: Slot 0 = Input, Slot 1 = Output.
 * Quad mode (2x2x1 multiblock Master): Slots 0..3 = Inputs, Slots 4..7 = Outputs (2x faster, 4 parallel channels).
 */
public class EnrichmentChamberBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private final MachineRecipeCache recipes = new MachineRecipeCache(MachineRecipeKind.ENRICHMENT);

    private int progress = 0;
    private final int[] quadProgress = new int[4];
    private boolean autoSort = true;
    private long quadFormAnimStartTick = -1L;
    private final SideConfigHolder sideConfig = SideConfigHolder.allInput();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= EnrichmentChamberMenu.DATA_SIDE_BASE && index < EnrichmentChamberMenu.DATA_COUNT) {
                return sideConfig.getMode(RelativeSide.fromOrdinal(index - EnrichmentChamberMenu.DATA_SIDE_BASE)).ordinal();
            }
            return switch (index) {
                case EnrichmentChamberMenu.DATA_PROGRESS -> progress;
                case EnrichmentChamberMenu.DATA_MAX_PROGRESS -> getMaxProgress();
                case EnrichmentChamberMenu.DATA_ENERGY_LOW -> energyStorage.getEnergyStored() & 0xFFFF;
                case EnrichmentChamberMenu.DATA_ENERGY_HIGH -> (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case EnrichmentChamberMenu.DATA_MAX_ENERGY_LOW -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case EnrichmentChamberMenu.DATA_MAX_ENERGY_HIGH -> (energyStorage.getMaxEnergyStored() >>> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == EnrichmentChamberMenu.DATA_PROGRESS) {
                progress = value;
            } else if (index >= EnrichmentChamberMenu.DATA_SIDE_BASE && index < EnrichmentChamberMenu.DATA_COUNT) {
                sideConfig.setMode(RelativeSide.fromOrdinal(index - EnrichmentChamberMenu.DATA_SIDE_BASE), SideConfigMode.fromOrdinal(value));
            }
        }

        @Override
        public int getCount() {
            return EnrichmentChamberMenu.DATA_COUNT;
        }
    };

    private final ContainerData quadData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= QuadEnrichmentMenu.DATA_SIDE_BASE && index < QuadEnrichmentMenu.DATA_COUNT) {
                return sideConfig.getMode(RelativeSide.fromOrdinal(index - QuadEnrichmentMenu.DATA_SIDE_BASE)).ordinal();
            }
            return switch (index) {
                case QuadEnrichmentMenu.DATA_PROGRESS_0 -> quadProgress[0];
                case QuadEnrichmentMenu.DATA_PROGRESS_1 -> quadProgress[1];
                case QuadEnrichmentMenu.DATA_PROGRESS_2 -> quadProgress[2];
                case QuadEnrichmentMenu.DATA_PROGRESS_3 -> quadProgress[3];
                case QuadEnrichmentMenu.DATA_MAX_PROGRESS -> getQuadMaxProgress();
                case QuadEnrichmentMenu.DATA_ENERGY_LOW -> energyStorage.getEnergyStored() & 0xFFFF;
                case QuadEnrichmentMenu.DATA_ENERGY_HIGH -> (energyStorage.getEnergyStored() >>> 16) & 0xFFFF;
                case QuadEnrichmentMenu.DATA_MAX_ENERGY_LOW -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case QuadEnrichmentMenu.DATA_MAX_ENERGY_HIGH -> (energyStorage.getMaxEnergyStored() >>> 16) & 0xFFFF;
                case QuadEnrichmentMenu.DATA_AUTO_SORT -> autoSort ? 1 : 0;
                case QuadEnrichmentMenu.DATA_LIT -> getBlockState().getValue(EnrichmentChamberBlock.LIT) ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= QuadEnrichmentMenu.DATA_PROGRESS_0 && index <= QuadEnrichmentMenu.DATA_PROGRESS_3) {
                quadProgress[index] = value;
            } else if (index == QuadEnrichmentMenu.DATA_AUTO_SORT) {
                autoSort = value != 0;
            } else if (index >= QuadEnrichmentMenu.DATA_SIDE_BASE && index < QuadEnrichmentMenu.DATA_COUNT) {
                sideConfig.setMode(RelativeSide.fromOrdinal(index - QuadEnrichmentMenu.DATA_SIDE_BASE), SideConfigMode.fromOrdinal(value));
            }
        }

        @Override
        public int getCount() {
            return QuadEnrichmentMenu.DATA_COUNT;
        }
    };

    public EnrichmentChamberBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.ENRICHMENT_CHAMBER.get(),
                pos,
                blockState,
                ModServerConfig.get(ModServerConfig.ENRICHMENT_ENERGY_CAPACITY),
                ModServerConfig.get(ModServerConfig.ENRICHMENT_MAX_RECEIVE),
                0,
                8
        );
    }

    @Override
    public Component getDisplayName() {
        if (getBlockState().getValue(EnrichmentChamberBlock.QUAD_ROLE) != QuadRole.NONE) {
            return Component.translatable("container.enrichment_solution.quad_enrichment_chamber");
        }
        return Component.translatable("block.enrichment_solution.enrichment_chamber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (getBlockState().getValue(EnrichmentChamberBlock.QUAD_ROLE) == QuadRole.MASTER) {
            return new QuadEnrichmentMenu(containerId, playerInventory, this, quadData);
        }
        return new EnrichmentChamberMenu(containerId, playerInventory, this, data);
    }

    @Override
    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        EnrichmentChamberBlockEntity master = resolveMaster();
        if (master != null && master != this) {
            return master.getEnergyStorage(side);
        }
        Direction facing = getBlockState().hasProperty(EnrichmentChamberBlock.FACING)
                ? getBlockState().getValue(EnrichmentChamberBlock.FACING)
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
    public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
        EnrichmentChamberBlockEntity master = resolveMaster();
        return master != null && master != this ? master.getItemHandler(side) : super.getItemHandler(side);
    }

    @Nullable
    private EnrichmentChamberBlockEntity resolveMaster() {
        BlockState state = getBlockState();
        QuadRole role = state.getValue(EnrichmentChamberBlock.QUAD_ROLE);
        if (role == QuadRole.NONE || role == QuadRole.MASTER || level == null) {
            return this;
        }
        BlockPos masterPos = EnrichmentChamberBlock.getMasterPos(worldPosition, state);
        if (level.getBlockEntity(masterPos) instanceof EnrichmentChamberBlockEntity master) {
            return master;
        }
        return this;
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        QuadRole role = state.getValue(EnrichmentChamberBlock.QUAD_ROLE);
        if (role == QuadRole.MASTER) {
            tickQuadMaster(level, pos, state);
            return;
        }
        if (role != QuadRole.NONE) {
            return;
        }

        boolean isWorking = false;
        RecipeHolder<MachineRecipe> holder = findSingleRecipe(itemHandler.getStackInSlot(SLOT_INPUT));
        MachineRecipe recipe = holder == null ? null : holder.value();
        int energyCost = recipe == null ? 0 : ModServerConfig.scaleEnergy(recipe.energyPerTick(), ModServerConfig.ENRICHMENT_ENERGY_MULTIPLIER);
        if (canProcessSingle(recipe) && energyStorage.getEnergyStored() >= energyCost) {
            energyStorage.consumeInternal(energyCost, false);
            progress++;
            isWorking = true;

            int targetTicks = ModServerConfig.scaleTicks(recipe.ticks(), ModServerConfig.ENRICHMENT_TIME_MULTIPLIER);
            if (progress >= targetTicks) {
                processSingle(recipe);
                progress = 0;
            }
            setChanged();
        } else if (progress > 0 && !canProcessSingle(recipe)) {
            progress = 0;
            setChanged();
        }

        if (state.getValue(EnrichmentChamberBlock.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(EnrichmentChamberBlock.LIT, isWorking), Block.UPDATE_ALL);
        }
    }

    private void tickQuadMaster(Level level, BlockPos pos, BlockState state) {
        if (autoSort) {
            distributeInputsEvenly();
        }

        boolean anyWorking = false;
        boolean dirty = false;

        for (int ch = 0; ch < 4; ch++) {
            RecipeHolder<MachineRecipe> holder = findChannelRecipe(ch);
            MachineRecipe recipe = holder == null ? null : holder.value();
            boolean canProcess = canProcessChannel(ch, recipe);
            int energyCost = recipe == null ? 0 : ModServerConfig.scaleEnergy(recipe.energyPerTick(), ModServerConfig.ENRICHMENT_ENERGY_MULTIPLIER);
            if (canProcess && energyStorage.getEnergyStored() >= energyCost) {
                energyStorage.consumeInternal(energyCost, false);
                quadProgress[ch]++;
                anyWorking = true;
                dirty = true;

                if (quadProgress[ch] >= quadTicks(recipe)) {
                    processChannel(ch, recipe);
                    quadProgress[ch] = 0;
                }
            } else if (quadProgress[ch] > 0 && !canProcess) {
                quadProgress[ch] = 0;
                dirty = true;
            }
        }

        if (dirty) {
            setChanged();
        }

        if (state.getValue(EnrichmentChamberBlock.LIT) != anyWorking) {
            level.setBlock(pos, state.setValue(EnrichmentChamberBlock.LIT, anyWorking), Block.UPDATE_ALL);
        }
    }

    /** The quad multiblock runs every channel twice as fast as a single chamber. */
    private static int quadTicks(MachineRecipe recipe) {
        int scaled = ModServerConfig.scaleTicks(recipe.ticks(), ModServerConfig.ENRICHMENT_TIME_MULTIPLIER);
        return Math.max(1, scaled / 2);
    }

    private static boolean isSingleInputRecipe(MachineRecipe recipe) {
        return recipe.hasItemInputs() && recipe.itemInputs().size() == 1 && recipe.hasItemOutput();
    }

    @Nullable
    private RecipeHolder<MachineRecipe> findSingleRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return null;
        }
        return recipes.find(level, recipe -> isSingleInputRecipe(recipe) && recipe.itemInputs().get(0).matches(input));
    }

    @Nullable
    private RecipeHolder<MachineRecipe> findChannelRecipe(int channel) {
        return findSingleRecipe(itemHandler.getStackInSlot(channel));
    }

    private boolean canProcessSingle(@Nullable MachineRecipe recipe) {
        return recipe != null
                && MachineRecipeMatcher.canAcceptOutput(itemHandler.getStackInSlot(SLOT_OUTPUT), recipe.itemOutput());
    }

    private void processSingle(MachineRecipe recipe) {
        itemHandler.extractItem(SLOT_INPUT, recipe.itemInputs().get(0).count(), false);
        itemHandler.insertItem(SLOT_OUTPUT, recipe.itemOutput().copy(), false);
    }

    private boolean canProcessChannel(int channel, @Nullable MachineRecipe recipe) {
        return recipe != null
                && MachineRecipeMatcher.canAcceptOutput(itemHandler.getStackInSlot(4 + channel), recipe.itemOutput());
    }

    private void processChannel(int channel, MachineRecipe recipe) {
        itemHandler.extractItem(channel, recipe.itemInputs().get(0).count(), false);
        itemHandler.insertItem(4 + channel, recipe.itemOutput().copy(), false);
    }

    public boolean isAutoSort() {
        return autoSort;
    }

    public void toggleAutoSort() {
        autoSort = !autoSort;
        if (autoSort) {
            distributeInputsEvenly();
        }
        setChanged();
    }

    /**
     * Spreads the four quad input slots evenly. Only runs when every non-empty slot holds the same item and that item is
     * a valid single input of the enrichment recipe type.
     */
    public void distributeInputsEvenly() {
        Item targetItem = null;
        int total = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (targetItem == null) {
                targetItem = stack.getItem();
            } else if (!stack.is(targetItem)) {
                return;
            }
            total += stack.getCount();
        }
        if (targetItem == null || total == 0 || !isEnrichmentInput(targetItem)) {
            return;
        }

        int base = total / 4;
        int rem = total % 4;
        boolean changed = false;
        for (int i = 0; i < 4; i++) {
            int targetCount = base + (i < rem ? 1 : 0);
            ItemStack current = itemHandler.getStackInSlot(i);
            if (current.getCount() != targetCount) {
                itemHandler.setStackInSlot(i, targetCount > 0
                        ? new ItemStack(targetItem, targetCount)
                        : ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
    }

    private boolean isEnrichmentInput(Item item) {
        if (level == null) {
            return false;
        }
        ItemStack probe = new ItemStack(item);
        return recipes.find(level, recipe -> isSingleInputRecipe(recipe)
                && recipe.itemInputs().get(0).ingredient().test(probe)) != null;
    }

    public void absorbFromSlave(EnrichmentChamberBlockEntity slave) {
        int stored = slave.energyStorage.getEnergyStored();
        if (stored > 0) {
            energyStorage.receiveEnergy(stored, false);
            slave.energyStorage.consumeInternal(stored, false);
        }
        for (int i = 0; i < slave.itemHandler.getSlots(); i++) {
            ItemStack stack = slave.itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                slave.itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                ItemStack rem = stack;
                for (int t = 0; t < 4 && !rem.isEmpty(); t++) {
                    rem = itemHandler.insertItem(t, rem, false);
                }
                if (!rem.isEmpty() && level != null) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, rem);
                }
            }
        }
        setChanged();
    }

    public void onFormQuad(Level level) {
        // Move single-mode output (slot 1) to quad output (slot 4) so slot 1 is free for quad input
        ItemStack singleOut = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (!singleOut.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            itemHandler.setStackInSlot(4, singleOut);
        }
        quadFormAnimStartTick = level.getGameTime();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public void onUnformQuad(Level level, BlockPos dropPos) {
        // Drop quad-only slots (1, 2, 3, 5, 6, 7) and move slot 4 back to single output (slot 1)
        for (int i : new int[]{1, 2, 3, 5, 6, 7}) {
            ItemStack s = itemHandler.getStackInSlot(i);
            if (!s.isEmpty()) {
                Containers.dropItemStack(level, dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, s);
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        ItemStack quadOut = itemHandler.getStackInSlot(4);
        if (!quadOut.isEmpty()) {
            itemHandler.setStackInSlot(4, ItemStack.EMPTY);
            itemHandler.setStackInSlot(SLOT_OUTPUT, quadOut);
        }
        for (int i = 0; i < 4; i++) {
            quadProgress[i] = 0;
        }
        quadFormAnimStartTick = -1L;
        setChanged();
    }

    public long getQuadFormAnimStartTick() {
        return quadFormAnimStartTick;
    }

    public int getProgress() {
        return progress;
    }

    /** Operation time of the recipe currently matching the single input slot, for the GUI. */
    public int getMaxProgress() {
        RecipeHolder<MachineRecipe> holder = findSingleRecipe(itemHandler.getStackInSlot(SLOT_INPUT));
        return holder == null
                ? ModServerConfig.get(ModServerConfig.ENRICHMENT_DEFAULT_PROCESS_TIME)
                : ModServerConfig.scaleTicks(holder.value().ticks(), ModServerConfig.ENRICHMENT_TIME_MULTIPLIER);
    }

    /** Operation time of the quad channels, for the GUI. */
    public int getQuadMaxProgress() {
        if (level != null) {
            for (int ch = 0; ch < 4; ch++) {
                RecipeHolder<MachineRecipe> holder = findChannelRecipe(ch);
                if (holder != null) {
                    return quadTicks(holder.value());
                }
            }
        }
        return ModServerConfig.get(ModServerConfig.ENRICHMENT_DEFAULT_QUAD_PROCESS_TIME);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
        tag.putIntArray("QuadProgress", quadProgress);
        tag.putBoolean("AutoSort", autoSort);
        tag.putLong("QuadFormAnimStart", quadFormAnimStartTick);
        sideConfig.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        if (tag.contains("QuadProgress")) {
            int[] arr = tag.getIntArray("QuadProgress");
            System.arraycopy(arr, 0, quadProgress, 0, Math.min(arr.length, 4));
        }
        if (tag.contains("AutoSort")) {
            autoSort = tag.getBoolean("AutoSort");
        }
        if (tag.contains("QuadFormAnimStart")) {
            quadFormAnimStartTick = tag.getLong("QuadFormAnimStart");
        }
        sideConfig.loadAdditional(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putLong("QuadFormAnimStart", quadFormAnimStartTick);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
