package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.CableTier;
import com.enrichmentsolution.mod.common.block.EnergyCableBlock;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.network.ConduitNetworkCache;
import com.enrichmentsolution.mod.common.block.entity.network.ConduitTarget;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Pulls and transfers Forge Energy (FE) across connected cable networks according to its CableTier.
 */
public class EnergyCableBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.IConduitConfigurable {

    private static final int MAX_NETWORK_SCAN = 256;

    /** Pre-computed routes of every cable of every loaded level, shared by all cable block entities. */
    private static final ConduitNetworkCache<ConduitTarget> NETWORK_CACHE = new ConduitNetworkCache<>();

    private final int transferRate;
    private final com.enrichmentsolution.mod.common.block.ConduitSideConfig conduitConfig = new com.enrichmentsolution.mod.common.block.ConduitSideConfig();

    @Override
    public com.enrichmentsolution.mod.common.block.ConduitSideConfig getConduitConfig() {
        return conduitConfig;
    }

    @Override
    public void onConduitConfigChanged() {
        if (level != null) {
            invalidateNetwork(level);
        }
        setChanged();
    }

    /**
     * Marks the cached cable routes of {@code level} stale. Must be called whenever the network
     * shape changes: a cable or machine is placed/removed next to a cable, or a cable's side
     * configuration changes.
     */
    public static void invalidateNetwork(Level level) {
        NETWORK_CACHE.invalidate(level);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        conduitConfig.save(tag);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        conduitConfig.load(tag);
    }

    public EnergyCableBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, resolveTier(blockState));
    }

    private EnergyCableBlockEntity(BlockPos pos, BlockState blockState, CableTier tier) {
        super(
                ModBlockEntities.ENERGY_CABLE.get(),
                pos,
                blockState,
                tier.getCapacity(),
                tier.getTransferRate(),
                tier.getTransferRate(),
                0
        );
        this.transferRate = tier.getTransferRate();
    }

    private static CableTier resolveTier(BlockState state) {
        return state.getBlock() instanceof EnergyCableBlock cable ? cable.getTier() : CableTier.BASIC;
    }

    /** Server-side accessors used by the goggles inspector overlay. */
    public int getStoredEnergy() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergy() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        pullEnergyFromAdjacentSources(level, pos);
        if (energyStorage.getEnergyStored() <= 0) {
            return;
        }
        distributeEnergyAlongNetwork(level, pos);
    }

    /**
     * Actively draws energy from adjacent non-cable energy providers up to this tier's transfer rate.
     */
    private void pullEnergyFromAdjacentSources(Level level, BlockPos pos) {
        int remainingSpace = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        if (remainingSpace <= 0) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe == null || neighborBe instanceof EnergyCableBlockEntity || !conduitConfig.canPull(dir)) {
                continue;
            }
            IEnergyStorage source = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
            if (source != null && source.canExtract()) {
                int toPull = Math.min(transferRate, energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored());
                if (toPull <= 0) {
                    return;
                }
                int simulated = source.extractEnergy(toPull, true);
                if (simulated > 0) {
                    int accepted = energyStorage.receiveEnergy(simulated, false);
                    if (accepted > 0) {
                        source.extractEnergy(accepted, false);
                    }
                }
            }
        }
    }

    /**
     * Delivers stored energy to the receivers pre-computed for this cable. The route list is
     * rebuilt only after the network changed, so a steady network costs one cache lookup per tick.
     */
    private void distributeEnergyAlongNetwork(Level level, BlockPos startPos) {
        List<ConduitTarget> targets = NETWORK_CACHE.routes(level, startPos, () -> scanNetwork(level, startPos));
        for (ConduitTarget target : targets) {
            if (energyStorage.getEnergyStored() <= 0) {
                return;
            }
            IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, target.pos(), target.accessSide());
            if (receiver == null || !receiver.canReceive()) {
                continue;
            }
            int extractable = energyStorage.extractEnergy(transferRate, true);
            if (extractable <= 0) {
                return;
            }
            int accepted = receiver.receiveEnergy(extractable, false);
            if (accepted > 0) {
                energyStorage.extractEnergy(accepted, false);
            }
        }
    }

    /**
     * Walks the connected cable network once and records every reachable non-cable block entity.
     * Targets whose exit side is closed by the side configuration of the last cable on the path are
     * left out. The energy capability itself is resolved on delivery, so changing a machine face
     * keeps working without rebuilding.
     */
    private List<ConduitTarget> scanNetwork(Level level, BlockPos startPos) {
        List<ConduitTarget> targets = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        visited.add(startPos);
        queue.add(startPos);

        while (!queue.isEmpty() && visited.size() <= MAX_NETWORK_SCAN) {
            BlockPos current = queue.poll();
            if (!(level.getBlockEntity(current) instanceof EnergyCableBlockEntity currentCable)) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (neighborBe instanceof EnergyCableBlockEntity) {
                    if (visited.add(neighborPos)) {
                        queue.add(neighborPos);
                    }
                    continue;
                }

                if (neighborBe == null || !currentCable.conduitConfig.canPush(dir)) {
                    continue;
                }
                targets.add(new ConduitTarget(neighborPos, dir.getOpposite()));
            }
        }
        return targets;
    }
}
