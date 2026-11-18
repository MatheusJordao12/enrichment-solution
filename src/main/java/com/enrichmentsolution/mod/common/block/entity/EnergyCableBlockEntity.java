package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.CableTier;
import com.enrichmentsolution.mod.common.block.EnergyCableBlock;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Pulls and transfers Forge Energy (FE) across connected cable networks according to its CableTier.
 */
public class EnergyCableBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.IConduitConfigurable {

    private static final int MAX_NETWORK_SCAN = 256;

    private final int transferRate;
    private final com.enrichmentsolution.mod.common.block.ConduitSideConfig conduitConfig = new com.enrichmentsolution.mod.common.block.ConduitSideConfig();

    @Override
    public com.enrichmentsolution.mod.common.block.ConduitSideConfig getConduitConfig() {
        return conduitConfig;
    }

    @Override
    public void onConduitConfigChanged() {
        setChanged();
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
     * Traverses connected cable blocks via BFS and pushes energy to non-cable IEnergyStorage receivers.
     */
    private void distributeEnergyAlongNetwork(Level level, BlockPos startPos) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(startPos);
        queue.add(startPos);

        while (!queue.isEmpty() && energyStorage.getEnergyStored() > 0 && visited.size() <= MAX_NETWORK_SCAN) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (neighborBe instanceof EnergyCableBlockEntity) {
                    if (visited.add(neighborPos)) {
                        queue.add(neighborPos);
                    }
                    continue;
                }

                if (neighborBe == null) {
                    continue;
                }
                if (level.getBlockEntity(current) instanceof EnergyCableBlockEntity cur && !cur.conduitConfig.canPush(dir)) {
                    continue;
                }

                IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
                if (receiver != null && receiver.canReceive()) {
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
        }
    }
}
