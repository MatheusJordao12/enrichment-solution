package com.enrichmentsolution.mod.common.block.entity;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigHolder;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.block.SolarPanelBlock;
import com.enrichmentsolution.mod.common.block.entity.base.AbstractMachineBlockEntity;
import com.enrichmentsolution.mod.common.config.ModServerConfig;
import com.enrichmentsolution.mod.common.menu.SolarPanelMenu;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Generates Forge Energy (FE) from sunlight and pushes it to adjacent blocks.
 * Supports upgrading to a 2-panel Advanced Solar Panel with a transformation animation.
 */
public class SolarPanelBlockEntity extends AbstractMachineBlockEntity implements com.enrichmentsolution.mod.common.block.ISideConfigurableBlockEntity, MenuProvider {

    public static final int UPGRADE_ANIMATION_TICKS = 32;

    private int lastGeneration = 0;
    private long upgradeAnimStartTick = -1L;
    private final SideConfigHolder sideConfig = SideConfigHolder.allOutput();

    public SolarPanelBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.SOLAR_PANEL.get(),
                pos,
                blockState,
                ModServerConfig.get(ModServerConfig.SOLAR_ENERGY_CAPACITY),
                0,
                ModServerConfig.get(ModServerConfig.SOLAR_MAX_EXTRACT),
                0
        );
    }

    private Direction getFacing() {
        return getBlockState().hasProperty(SolarPanelBlock.FACING)
                ? getBlockState().getValue(SolarPanelBlock.FACING)
                : Direction.NORTH;
    }

    @Override
    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return sideConfig.wrapEnergyStorage(energyStorage, side, getFacing());
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

    public boolean isAdvanced() {
        return getBlockState().hasProperty(SolarPanelBlock.ADVANCED) && getBlockState().getValue(SolarPanelBlock.ADVANCED);
    }

    public void triggerUpgradeAnimation(Level level) {
        this.upgradeAnimStartTick = level.getGameTime();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /**
     * Returns 0.0 -> 1.0 during the upgrade transformation animation, or 1.0 when finished/idle.
     */
    public float getUpgradeAnimationProgress(Level level, float partialTick) {
        if (upgradeAnimStartTick < 0L) {
            return 1.0F;
        }
        float elapsed = (level.getGameTime() - upgradeAnimStartTick) + partialTick;
        if (elapsed >= UPGRADE_ANIMATION_TICKS) {
            return 1.0F;
        }
        return Math.max(0.0F, elapsed / UPGRADE_ANIMATION_TICKS);
    }

    @Override
    protected void tickServer(Level level, BlockPos pos, BlockState state) {
        int generated = calculateGeneration(level, pos);
        lastGeneration = generated;

        if (generated > 0) {
            energyStorage.generateInternal(generated, false);
        }

        pushEnergyToNeighbors(level, pos);
    }

    private int calculateGeneration(Level level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight() || !level.isDay()) {
            return 0;
        }
        if (!level.canSeeSky(pos.above())) {
            return 0;
        }
        boolean advanced = isAdvanced();
        if (level.isRaining()) {
            return ModServerConfig.get(advanced ? ModServerConfig.SOLAR_ADV_FE_PER_TICK_RAIN : ModServerConfig.SOLAR_FE_PER_TICK_RAIN);
        }
        return ModServerConfig.get(advanced ? ModServerConfig.SOLAR_ADV_FE_PER_TICK_CLEAR : ModServerConfig.SOLAR_FE_PER_TICK_CLEAR);
    }

    private void pushEnergyToNeighbors(Level level, BlockPos pos) {
        if (energyStorage.getEnergyStored() <= 0) {
            return;
        }
        Direction facing = getFacing();
        for (Direction direction : Direction.values()) {
            if (!sideConfig.canPushTo(direction, facing)) {
                continue;
            }
            BlockPos neighborPos = pos.relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null) {
                continue;
            }
            IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());
            if (receiver != null && receiver.canReceive()) {
                int extractable = energyStorage.extractEnergy(ModServerConfig.get(ModServerConfig.SOLAR_MAX_EXTRACT), true);
                int accepted = receiver.receiveEnergy(extractable, false);
                if (accepted > 0) {
                    energyStorage.extractEnergy(accepted, false);
                }
            }
        }
    }

    public int getLastGeneration() {
        return lastGeneration;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("UpgradeAnimStart", upgradeAnimStartTick);
        sideConfig.saveAdditional(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("UpgradeAnimStart")) {
            upgradeAnimStartTick = tag.getLong("UpgradeAnimStart");
        }
        sideConfig.loadAdditional(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putLong("UpgradeAnimStart", upgradeAnimStartTick);
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
            if (index >= SolarPanelMenu.DATA_SIDE_BASE && index < SolarPanelMenu.DATA_COUNT) {
                return sideConfig.getMode(RelativeSide.fromOrdinal(index - SolarPanelMenu.DATA_SIDE_BASE)).ordinal();
            }
            var energy = getEnergyStorage(null);
            int stored = energy != null ? energy.getEnergyStored() : 0;
            int max = energy != null ? energy.getMaxEnergyStored() : 0;
            return switch (index) {
                case SolarPanelMenu.DATA_ENERGY_LOW -> stored & 0xFFFF;
                case SolarPanelMenu.DATA_ENERGY_HIGH -> (stored >>> 16) & 0xFFFF;
                case SolarPanelMenu.DATA_MAX_ENERGY_LOW -> max & 0xFFFF;
                case SolarPanelMenu.DATA_MAX_ENERGY_HIGH -> (max >>> 16) & 0xFFFF;
                case SolarPanelMenu.DATA_GENERATION -> lastGeneration;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= SolarPanelMenu.DATA_SIDE_BASE && index < SolarPanelMenu.DATA_COUNT) {
                sideConfig.setMode(RelativeSide.fromOrdinal(index - SolarPanelMenu.DATA_SIDE_BASE), SideConfigMode.fromOrdinal(value));
            }
        }

        @Override
        public int getCount() {
            return SolarPanelMenu.DATA_COUNT;
        }
    };

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enrichment_solution.solar_panel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SolarPanelMenu(containerId, playerInventory, this, data);
    }
}
