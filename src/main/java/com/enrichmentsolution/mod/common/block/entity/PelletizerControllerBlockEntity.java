package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.ConveyorBeltBlock;
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
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BlockEntity for the Pelletizer Multiblock Controller.
 * Validates a connected {@code reactor_casing} shell containing {@code crusher_module},
 * {@code hydraulic_press_head}, and {@code sintering_heater}, pulls {@code enriched_uranium}
 * from conveyor belts inside/adjacent to the machine, and outputs {@code uranium_pellet}
 * onto the outgoing conveyor belt.
 */
public class PelletizerControllerBlockEntity extends AbstractMachineBlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    public static final int ENERGY_CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int DEFAULT_PROCESS_TIME = 100;

    private static final int MIN_REACTOR_CASINGS = 20;
    private static final int MAX_SCAN_BLOCKS = 96;

    private final MachineRecipeCache recipes = new MachineRecipeCache(MachineRecipeKind.PELLETIZING);

    private int progress = 0;
    private boolean structureValid = false;

    private record StructureScan(
            boolean valid,
            int casings,
            int crushers,
            int presses,
            int heaters,
            List<BlockPos> heaterPositions,
            BlockPos minBound,
            BlockPos maxBound
    ) {
    }

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
        StructureScan scan = scanStructure(level, pos);
        this.structureValid = scan.valid();

        boolean isWorking = false;
        if (scan.valid()) {
            pullFromTunnelBelts(level, scan);

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

            pushToExitBelt(level, scan);
        } else if (progress > 0) {
            progress = 0;
            setChanged();
        }

        if (state.getValue(PelletizerControllerBlock.ACTIVE) != isWorking) {
            level.setBlock(pos, state.setValue(PelletizerControllerBlock.ACTIVE, isWorking), Block.UPDATE_ALL);
        }
        setHeatersActive(level, scan.heaterPositions(), isWorking);
    }

    private void pullFromTunnelBelts(Level level, StructureScan scan) {
        for (int x = scan.minBound().getX(); x <= scan.maxBound().getX(); x++) {
            for (int y = scan.minBound().getY(); y <= scan.maxBound().getY(); y++) {
                for (int z = scan.minBound().getZ(); z <= scan.maxBound().getZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!(level.getBlockEntity(p) instanceof ConveyorBeltBlockEntity belt)) {
                        continue;
                    }
                    ItemStack beltItem = belt.getStoredItem();
                    if (beltItem.isEmpty()) {
                        continue;
                    }
                    if (!isValidPelletizingInput(beltItem)) {
                        continue;
                    }
                    ItemStack currentInput = itemHandler.getStackInSlot(SLOT_INPUT);
                    if (currentInput.isEmpty()
                            || (ItemStack.isSameItemSameComponents(currentInput, beltItem)
                            && currentInput.getCount() + beltItem.getCount() <= currentInput.getMaxStackSize())) {
                        ItemStack taken = belt.takeStoredItem();
                        if (!taken.isEmpty()) {
                            ItemStack rem = ItemHandlerHelper.insertItemStacked(itemHandler, taken, false);
                            if (!rem.isEmpty()) {
                                belt.acceptItem(rem);
                            }
                            setChanged();
                        }
                    }
                }
            }
        }
    }

    private void pushToExitBelt(Level level, StructureScan scan) {
        ItemStack output = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return;
        }

        ConveyorBeltBlockEntity bestBelt = null;
        int bestScore = Integer.MIN_VALUE;

        for (int x = scan.minBound().getX(); x <= scan.maxBound().getX(); x++) {
            for (int y = scan.minBound().getY(); y <= scan.maxBound().getY(); y++) {
                for (int z = scan.minBound().getZ(); z <= scan.maxBound().getZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState bs = level.getBlockState(p);
                    if (!(bs.getBlock() instanceof ConveyorBeltBlock)
                            || !(level.getBlockEntity(p) instanceof ConveyorBeltBlockEntity belt)
                            || !belt.canAcceptItem()) {
                        continue;
                    }
                    Direction facing = bs.getValue(ConveyorBeltBlock.FACING);
                    Direction moveDir = belt.isReverse() ? facing.getOpposite() : facing;
                    // Prefer the belt closest to the exit in moveDir
                    int score = p.getX() * moveDir.getStepX() + p.getZ() * moveDir.getStepZ();
                    if (bestBelt == null || score > bestScore) {
                        bestBelt = belt;
                        bestScore = score;
                    }
                }
            }
        }

        if (bestBelt != null) {
            ItemStack toSend = itemHandler.extractItem(SLOT_OUTPUT, output.getCount(), false);
            if (!toSend.isEmpty()) {
                if (!bestBelt.acceptItem(toSend)) {
                    itemHandler.insertItem(SLOT_OUTPUT, toSend, false);
                } else {
                    setChanged();
                }
            }
        }
    }

    private boolean isValidPelletizingInput(ItemStack stack) {
        return recipes.find(level, recipe ->
                recipe.hasItemInputs()
                        && recipe.itemInputs().size() == 1
                        && recipe.hasItemOutput()
                        && recipe.itemInputs().get(0).ingredient().test(stack)) != null;
    }

    public void setHeaterActive(Level level, BlockPos controllerPos, BlockState controllerState, boolean active) {
        setHeatersActive(level, scanStructure(level, controllerPos).heaterPositions(), active);
    }

    private static void setHeatersActive(Level level, List<BlockPos> heaters, boolean active) {
        for (BlockPos heaterPos : heaters) {
            BlockState heaterState = level.getBlockState(heaterPos);
            if (heaterState.getBlock() instanceof SinteringHeaterBlock
                    && heaterState.getValue(SinteringHeaterBlock.ACTIVE) != active) {
                level.setBlock(heaterPos, heaterState.setValue(SinteringHeaterBlock.ACTIVE, active), Block.UPDATE_ALL);
            }
        }
    }

    public boolean isStructureValid() {
        if (level == null) {
            return structureValid;
        }
        return scanStructure(level, worldPosition).valid();
    }

    @Nullable
    public Component getMissingBlockMessage() {
        if (level == null) {
            return null;
        }
        StructureScan scan = scanStructure(level, worldPosition);
        if (scan.valid()) {
            return null;
        }
        if (scan.casings() < MIN_REACTOR_CASINGS) {
            return Component.literal("Missing Reactor Casing (" + scan.casings() + "/" + MIN_REACTOR_CASINGS + ")");
        }
        if (scan.crushers() < 1) {
            return Component.literal("Missing Crusher Module");
        }
        if (scan.presses() < 1) {
            return Component.literal("Missing Hydraulic Press Head");
        }
        if (scan.heaters() < 1) {
            return Component.literal("Missing Sintering Heater");
        }
        return null;
    }

    private StructureScan scanStructure(Level level, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);

        int casings = 0;
        int crushers = 0;
        int presses = 0;
        int heaters = 0;
        List<BlockPos> heaterPositions = new ArrayList<>();

        int minX = origin.getX(), minY = origin.getY(), minZ = origin.getZ();
        int maxX = origin.getX(), maxY = origin.getY(), maxZ = origin.getZ();

        while (!queue.isEmpty() && visited.size() < MAX_SCAN_BLOCKS) {
            BlockPos current = queue.poll();
            BlockState state = level.getBlockState(current);
            Block block = state.getBlock();

            if (block == ModBlocks.REACTOR_CASING.get()) {
                casings++;
            } else if (block == ModBlocks.CRUSHER_MODULE.get()) {
                crushers++;
            } else if (block == ModBlocks.HYDRAULIC_PRESS_HEAD.get()) {
                presses++;
            } else if (block == ModBlocks.SINTERING_HEATER.get()) {
                heaters++;
                heaterPositions.add(current);
            }

            minX = Math.min(minX, current.getX());
            minY = Math.min(minY, current.getY());
            minZ = Math.min(minZ, current.getZ());
            maxX = Math.max(maxX, current.getX());
            maxY = Math.max(maxY, current.getY());
            maxZ = Math.max(maxZ, current.getZ());

            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (visited.contains(next)) {
                    continue;
                }
                if (Math.abs(next.getX() - origin.getX()) > 6
                        || Math.abs(next.getY() - origin.getY()) > 4
                        || Math.abs(next.getZ() - origin.getZ()) > 6) {
                    continue;
                }
                Block nextBlock = level.getBlockState(next).getBlock();
                if (isStructurePart(nextBlock)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }

        boolean valid = casings >= MIN_REACTOR_CASINGS && crushers >= 1 && presses >= 1 && heaters >= 1;
        return new StructureScan(
                valid,
                casings,
                crushers,
                presses,
                heaters,
                heaterPositions,
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ)
        );
    }

    private static boolean isStructurePart(Block block) {
        return block == ModBlocks.REACTOR_CASING.get()
                || block == ModBlocks.CRUSHER_MODULE.get()
                || block == ModBlocks.HYDRAULIC_PRESS_HEAD.get()
                || block == ModBlocks.SINTERING_HEATER.get()
                || block == ModBlocks.PELLETIZER_CONTROLLER.get();
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
