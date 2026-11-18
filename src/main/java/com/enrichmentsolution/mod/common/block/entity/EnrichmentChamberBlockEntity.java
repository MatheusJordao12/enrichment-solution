package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.EnrichmentChamberBlock;
import com.enrichmentsolution.mod.common.block.EnrichmentChamberBlock.QuadRole;
import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.menu.EnrichmentChamberMenu;
import com.enrichmentsolution.mod.common.menu.QuadEnrichmentMenu;
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
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Converts Raw Uranium into Enriched Uranium using Forge Energy (FE).
 * Single mode: Slot 0 = Input, Slot 1 = Output (100 ticks).
 * Quad mode (2x2x1 multiblock Master): Slots 0..3 = Inputs, Slots 4..7 = Outputs (50 ticks, 2x faster, 4 parallel channels).
 */
public class EnrichmentChamberBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private static final int ENERGY_CAPACITY = 50_000;
    private static final int MAX_RECEIVE = 2_000;
    private static final int ENERGY_PER_TICK = 40;
    private static final int PROCESS_TIME = 100;
    private static final int QUAD_PROCESS_TIME = 50;

    /** Recipe snapshot for the JEI plugin. */
    public static java.util.List<com.enrichmentsolution.mod.common.compat.EnrichmentRecipe> getJeiRecipes() {
        return java.util.List.of(new com.enrichmentsolution.mod.common.compat.EnrichmentRecipe(
                new ItemStack(ModItems.RAW_URANIUM.get()),
                new ItemStack(ModItems.ENRICHED_URANIUM.get()),
                ENERGY_PER_TICK,
                PROCESS_TIME));
    }

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
                case EnrichmentChamberMenu.DATA_MAX_PROGRESS -> PROCESS_TIME;
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
                case QuadEnrichmentMenu.DATA_MAX_PROGRESS -> QUAD_PROCESS_TIME;
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
        super(ModBlockEntities.ENRICHMENT_CHAMBER.get(), pos, blockState, ENERGY_CAPACITY, MAX_RECEIVE, 0, 8);
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
        if (canProcessSingle() && energyStorage.getEnergyStored() >= ENERGY_PER_TICK) {
            energyStorage.consumeInternal(ENERGY_PER_TICK, false);
            progress++;
            isWorking = true;

            if (progress >= PROCESS_TIME) {
                processSingle();
                progress = 0;
            }
            setChanged();
        } else if (progress > 0 && !canProcessSingle()) {
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
            if (canProcessChannel(ch) && energyStorage.getEnergyStored() >= ENERGY_PER_TICK) {
                energyStorage.consumeInternal(ENERGY_PER_TICK, false);
                quadProgress[ch]++;
                anyWorking = true;
                dirty = true;

                if (quadProgress[ch] >= QUAD_PROCESS_TIME) {
                    processChannel(ch);
                    quadProgress[ch] = 0;
                }
            } else if (quadProgress[ch] > 0 && !canProcessChannel(ch)) {
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

    private boolean canProcessSingle() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        if (!input.is(ModItems.RAW_URANIUM.get())) {
            return false;
        }
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        return output.is(ModItems.ENRICHED_URANIUM.get()) && output.getCount() < output.getMaxStackSize();
    }

    private void processSingle() {
        itemHandler.extractItem(SLOT_INPUT, 1, false);
        itemHandler.insertItem(SLOT_OUTPUT, new ItemStack(ModItems.ENRICHED_URANIUM.get(), 1), false);
    }

    private boolean canProcessChannel(int ch) {
        ItemStack input = itemHandler.getStackInSlot(ch);
        if (!input.is(ModItems.RAW_URANIUM.get())) {
            return false;
        }
        ItemStack output = itemHandler.getStackInSlot(4 + ch);
        if (output.isEmpty()) {
            return true;
        }
        return output.is(ModItems.ENRICHED_URANIUM.get()) && output.getCount() < output.getMaxStackSize();
    }

    private void processChannel(int ch) {
        itemHandler.extractItem(ch, 1, false);
        itemHandler.insertItem(4 + ch, new ItemStack(ModItems.ENRICHED_URANIUM.get(), 1), false);
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

    public void distributeInputsEvenly() {
        int total = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.is(ModItems.RAW_URANIUM.get())) {
                total += stack.getCount();
            } else if (!stack.isEmpty()) {
                return;
            }
        }
        if (total == 0) {
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
                        ? new ItemStack(ModItems.RAW_URANIUM.get(), targetCount)
                        : ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
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

    public int getMaxProgress() {
        return PROCESS_TIME;
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
