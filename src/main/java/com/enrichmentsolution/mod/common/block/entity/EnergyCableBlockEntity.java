package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.CableTier;
import com.enrichmentsolution.mod.common.block.EnergyCableBlock;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.network.ConduitLink;
import com.enrichmentsolution.mod.common.block.entity.network.ConduitNetworkCache;
import com.enrichmentsolution.mod.common.block.entity.network.ConduitTarget;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

/**
 * Pulls and transfers Forge Energy (FE) across connected cable networks according to its CableTier.
 */
public class EnergyCableBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.IConduitConfigurable {

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
        if (energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored() <= 0) {
            return;
        }
        ConduitLink.pullFromAdjacent(
                level,
                pos,
                EnergyCableBlockEntity.class,
                conduitConfig,
                Capabilities.EnergyStorage.BLOCK,
                (neighborPos, dir, source) -> {
                    int toPull = Math.min(transferRate, energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored());
                    if (toPull <= 0) {
                        return false;
                    }
                    if (source.canExtract()) {
                        int simulated = source.extractEnergy(toPull, true);
                        if (simulated > 0) {
                            int accepted = energyStorage.receiveEnergy(simulated, false);
                            if (accepted > 0) {
                                source.extractEnergy(accepted, false);
                            }
                        }
                    }
                    return energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored() > 0;
                }
        );
    }

    /**
     * Delivers stored energy to the receivers pre-computed for this cable. The route list is
     * rebuilt only after the network changed, so a steady network costs one cache lookup per tick.
     */
    private void distributeEnergyAlongNetwork(Level level, BlockPos startPos) {
        List<ConduitTarget> targets = NETWORK_CACHE.routes(level, startPos, () -> scanNetwork(level, startPos));
        ConduitLink.distribute(
                level,
                targets,
                ConduitTarget::pos,
                ConduitTarget::accessSide,
                Capabilities.EnergyStorage.BLOCK,
                () -> energyStorage.getEnergyStored() > 0,
                (target, receiver) -> {
                    if (!receiver.canReceive()) {
                        return;
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
        );
    }

    /**
     * Walks the connected cable network once and records every reachable non-cable block entity.
     */
    private List<ConduitTarget> scanNetwork(Level level, BlockPos startPos) {
        return ConduitLink.scanNetwork(
                level,
                startPos,
                EnergyCableBlockEntity.class,
                EnergyCableBlockEntity::getConduitConfig,
                Void.TYPE,
                (state, cable) -> Void.TYPE,
                (pos, accessSide, state) -> new ConduitTarget(pos, accessSide)
        );
    }
}
