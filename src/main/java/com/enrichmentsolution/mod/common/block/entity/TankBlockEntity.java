package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.TankBlock;
import com.enrichmentsolution.mod.common.menu.TankMenu;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Multiblock Fluid Tank BlockEntity for the Geothermal Generator base.
 * Supports connected structures up to 20x20x20 blocks (200,000 mB = 200 buckets per block)
 * and computes thermal multiplier from Y-level (80 -> 1x, -50 -> 20x), Lava sources (3x),
 * Magma blocks (2.25x), and Desert biome (1.25x).
 */
public class TankBlockEntity extends BlockEntity implements MenuProvider {

    public static final int CAPACITY_PER_BLOCK = 200_000; // 200 buckets
    public static final int MAX_SPAN = 20;

    private BlockPos masterPos;
    private int blockCount = 1;
    private final List<BlockPos> members = new ArrayList<>();

    private final FluidTank fluidTank = new FluidTank(CAPACITY_PER_BLOCK,
            stack -> stack.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private double temperature = 20.0D; // Celsius
    private double multiplier = 1.0D;
    private boolean hasLavaSource = false;
    private boolean hasMagma = false;
    private boolean isDesert = false;
    private int envCheckCooldown = 0;

    public TankBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TANK_BLOCK.get(), pos, blockState);
        this.masterPos = pos;
        this.members.add(pos);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TankBlockEntity blockEntity) {
        if (level.isClientSide || !blockEntity.isMaster()) {
            return;
        }
        if (--blockEntity.envCheckCooldown <= 0) {
            blockEntity.envCheckCooldown = 20;
            blockEntity.updateThermalState(level);
        }
    }

    public boolean isMaster() {
        return worldPosition.equals(masterPos);
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    @Nullable
    public TankBlockEntity getMasterEntity() {
        if (isMaster()) {
            return this;
        }
        if (level != null && level.getBlockEntity(masterPos) instanceof TankBlockEntity master) {
            return master;
        }
        return this;
    }

    public FluidTank getMasterTank() {
        TankBlockEntity master = getMasterEntity();
        return master != null ? master.fluidTank : this.fluidTank;
    }

    public int getBlockCount() {
        TankBlockEntity master = getMasterEntity();
        return master != null ? master.blockCount : 1;
    }

    public double getTemperature() {
        TankBlockEntity master = getMasterEntity();
        return master != null ? master.temperature : 20.0D;
    }

    public double getMultiplier() {
        TankBlockEntity master = getMasterEntity();
        return master != null ? master.multiplier : 1.0D;
    }

    public int getBonusFlags() {
        TankBlockEntity master = getMasterEntity();
        if (master == null) {
            return 0;
        }
        int flags = 0;
        if (master.hasLavaSource) flags |= 1;
        if (master.hasMagma) flags |= 2;
        if (master.isDesert) flags |= 4;
        return flags;
    }

    private void updateThermalState(Level level) {
        int avgY = 0;
        boolean foundLava = false;
        boolean foundMagma = false;

        List<BlockPos> scanList = members.isEmpty() ? List.of(worldPosition) : members;
        for (BlockPos member : scanList) {
            avgY += member.getY();
            for (Direction dir : Direction.values()) {
                BlockPos adj = member.relative(dir);
                BlockState adjState = level.getBlockState(adj);
                if (adjState.is(Blocks.LAVA) && adjState.getFluidState().isSource()) {
                    foundLava = true;
                } else if (adjState.is(Blocks.MAGMA_BLOCK)) {
                    foundMagma = true;
                }
            }
        }
        double meanY = (double) avgY / scanList.size();
        double t = Mth.clamp((80.0D - meanY) / 130.0D, 0.0D, 1.0D);
        double yMult = 1.0D + t * 19.0D; // Y=80 -> 1x, Y=-50 -> 20x

        var biome = level.getBiome(worldPosition);
        boolean desert = biome.is(Biomes.DESERT) || biome.is(Tags.Biomes.IS_DESERT);

        this.hasLavaSource = foundLava;
        this.hasMagma = foundMagma;
        this.isDesert = desert;
        this.multiplier = yMult * (foundLava ? 3.0D : 1.0D) * (foundMagma ? 2.25D : 1.0D) * (desert ? 1.25D : 1.0D);

        double targetTemp = 20.0D + this.multiplier * 10.0D;
        this.temperature += (targetTemp - this.temperature) * 0.2D;
        setChanged();
    }

    public static void rebuildNetwork(Level level, BlockPos seedPos, @Nullable BlockPos ignoredPos) {
        if (level.isClientSide) {
            return;
        }
        if (seedPos.equals(ignoredPos) || !(level.getBlockState(seedPos).getBlock() instanceof TankBlock)) {
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> component = new ArrayList<>();

        visited.add(seedPos);
        queue.add(seedPos);

        int minX = seedPos.getX(), maxX = seedPos.getX();
        int minY = seedPos.getY(), maxY = seedPos.getY();
        int minZ = seedPos.getZ(), maxZ = seedPos.getZ();

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            component.add(current);

            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (next.equals(ignoredPos) || visited.contains(next)) {
                    continue;
                }
                if (!(level.getBlockState(next).getBlock() instanceof TankBlock)) {
                    continue;
                }
                int nMinX = Math.min(minX, next.getX());
                int nMaxX = Math.max(maxX, next.getX());
                int nMinY = Math.min(minY, next.getY());
                int nMaxY = Math.max(maxY, next.getY());
                int nMinZ = Math.min(minZ, next.getZ());
                int nMaxZ = Math.max(maxZ, next.getZ());
                if (nMaxX - nMinX + 1 > MAX_SPAN || nMaxY - nMinY + 1 > MAX_SPAN || nMaxZ - nMinZ + 1 > MAX_SPAN) {
                    continue;
                }
                minX = nMinX; maxX = nMaxX;
                minY = nMinY; maxY = nMaxY;
                minZ = nMinZ; maxZ = nMaxZ;
                visited.add(next);
                queue.add(next);
            }
        }

        if (component.isEmpty()) {
            return;
        }

        component.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getZ));
        BlockPos newMasterPos = component.get(0);

        int totalWater = 0;
        double maxTemp = 20.0D;
        for (BlockPos pos : component) {
            if (level.getBlockEntity(pos) instanceof TankBlockEntity be) {
                totalWater += be.fluidTank.getFluidAmount();
                maxTemp = Math.max(maxTemp, be.temperature);
            }
        }

        int newCapacity = component.size() * CAPACITY_PER_BLOCK;
        int clampedWater = Math.min(totalWater, newCapacity);

        for (BlockPos pos : component) {
            if (level.getBlockEntity(pos) instanceof TankBlockEntity be) {
                be.masterPos = newMasterPos;
                if (pos.equals(newMasterPos)) {
                    be.blockCount = component.size();
                    be.members.clear();
                    be.members.addAll(component);
                    be.fluidTank.setCapacity(newCapacity);
                    be.fluidTank.setFluid(clampedWater > 0 ? new FluidStack(Fluids.WATER, clampedWater) : FluidStack.EMPTY);
                    be.temperature = maxTemp;
                    be.envCheckCooldown = 0;
                } else {
                    be.blockCount = 1;
                    be.members.clear();
                    be.fluidTank.setCapacity(CAPACITY_PER_BLOCK);
                    be.fluidTank.setFluid(FluidStack.EMPTY);
                }
                be.setChanged();
            }
        }
    }

    public static void onBlockBroken(Level level, BlockPos brokenPos, int previousWaterAmount) {
        List<BlockPos> neighborTanks = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos adj = brokenPos.relative(dir);
            if (level.getBlockState(adj).getBlock() instanceof TankBlock) {
                neighborTanks.add(adj);
            }
        }
        if (neighborTanks.isEmpty()) {
            return;
        }
        Set<BlockPos> processed = new HashSet<>();
        for (BlockPos neighbor : neighborTanks) {
            if (processed.contains(neighbor)) {
                continue;
            }
            rebuildNetwork(level, neighbor, brokenPos);
            if (level.getBlockEntity(neighbor) instanceof TankBlockEntity be) {
                TankBlockEntity master = be.getMasterEntity();
                if (master != null) {
                    processed.addAll(master.members);
                    if (previousWaterAmount > 0 && master.fluidTank.getFluidAmount() == 0) {
                        master.fluidTank.fill(new FluidStack(Fluids.WATER,
                                Math.min(previousWaterAmount, master.fluidTank.getCapacity())), IFluidHandler.FluidAction.EXECUTE);
                        previousWaterAmount = 0;
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("MasterPos", NbtUtils.writeBlockPos(masterPos));
        tag.putInt("BlockCount", blockCount);
        tag.putDouble("Temperature", temperature);
        tag.putDouble("Multiplier", multiplier);
        fluidTank.writeToNBT(registries, tag);

        ListTag memberList = new ListTag();
        for (BlockPos m : members) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", m.getX());
            entry.putInt("Y", m.getY());
            entry.putInt("Z", m.getZ());
            memberList.add(entry);
        }
        tag.put("Members", memberList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NbtUtils.readBlockPos(tag, "MasterPos").ifPresent(pos -> this.masterPos = pos);
        if (tag.contains("BlockCount")) {
            this.blockCount = Math.max(1, tag.getInt("BlockCount"));
        }
        if (tag.contains("Temperature")) {
            this.temperature = tag.getDouble("Temperature");
        }
        if (tag.contains("Multiplier")) {
            this.multiplier = tag.getDouble("Multiplier");
        }
        this.fluidTank.setCapacity(this.blockCount * CAPACITY_PER_BLOCK);
        this.fluidTank.readFromNBT(registries, tag);

        this.members.clear();
        if (tag.contains("Members", Tag.TAG_LIST)) {
            ListTag memberList = tag.getList("Members", Tag.TAG_COMPOUND);
            for (int i = 0; i < memberList.size(); i++) {
                CompoundTag entry = memberList.getCompound(i);
                this.members.add(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
            }
        }
        if (this.members.isEmpty()) {
            this.members.add(worldPosition);
        }
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            TankBlockEntity master = getMasterEntity();
            if (master == null) {
                return 0;
            }
            int fluid = master.fluidTank.getFluidAmount();
            int cap = master.fluidTank.getCapacity();
            int tempTenths = (int) Math.round(master.temperature * 10.0D);
            int multHundredths = (int) Math.round(master.multiplier * 100.0D);
            return switch (index) {
                case TankMenu.DATA_FLUID_LOW -> fluid & 0xFFFF;
                case TankMenu.DATA_FLUID_HIGH -> (fluid >>> 16) & 0xFFFF;
                case TankMenu.DATA_CAP_LOW -> cap & 0xFFFF;
                case TankMenu.DATA_CAP_HIGH -> (cap >>> 16) & 0xFFFF;
                case TankMenu.DATA_TEMP_TENTHS -> tempTenths;
                case TankMenu.DATA_MULT_HUNDREDTHS -> multHundredths;
                case TankMenu.DATA_BLOCK_COUNT -> master.blockCount;
                case TankMenu.DATA_BONUS_FLAGS -> master.getBonusFlags();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return TankMenu.DATA_COUNT;
        }
    };

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        TankBlockEntity master = getMasterEntity();
        return new TankMenu(containerId, playerInventory, master != null ? master : this, data);
    }
}
