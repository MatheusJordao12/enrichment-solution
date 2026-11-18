package com.enrichmentsolution.mod.common.block;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Holds per-{@link RelativeSide} I/O configuration for a machine and provides capability wrappers
 * and NBT serialization.
 */
public class SideConfigHolder {

    private static final String TAG_SIDE_CONFIG = "SideConfig";

    private final EnumMap<RelativeSide, SideConfigMode> modes = new EnumMap<>(RelativeSide.class);

    public SideConfigHolder(SideConfigMode defaultMode) {
        for (RelativeSide side : RelativeSide.values()) {
            modes.put(side, defaultMode);
        }
    }

    public SideConfigHolder(Map<RelativeSide, SideConfigMode> defaults) {
        for (RelativeSide side : RelativeSide.values()) {
            modes.put(side, defaults.getOrDefault(side, SideConfigMode.NONE));
        }
    }

    public static SideConfigHolder allInput() {
        return new SideConfigHolder(SideConfigMode.INPUT);
    }

    public static SideConfigHolder allOutput() {
        return new SideConfigHolder(SideConfigMode.OUTPUT);
    }

    public static SideConfigHolder allNone() {
        return new SideConfigHolder(SideConfigMode.NONE);
    }

    public static SideConfigHolder createDefault(
            SideConfigMode front,
            SideConfigMode back,
            SideConfigMode left,
            SideConfigMode right,
            SideConfigMode top,
            SideConfigMode bottom
    ) {
        EnumMap<RelativeSide, SideConfigMode> map = new EnumMap<>(RelativeSide.class);
        map.put(RelativeSide.FRONT, front);
        map.put(RelativeSide.BACK, back);
        map.put(RelativeSide.LEFT, left);
        map.put(RelativeSide.RIGHT, right);
        map.put(RelativeSide.TOP, top);
        map.put(RelativeSide.BOTTOM, bottom);
        return new SideConfigHolder(map);
    }

    public SideConfigMode getMode(RelativeSide side) {
        return modes.getOrDefault(side, SideConfigMode.NONE);
    }

    public void setMode(RelativeSide side, SideConfigMode mode) {
        modes.put(side, mode);
    }

    public SideConfigMode cycleMode(RelativeSide side) {
        SideConfigMode next = getMode(side).next();
        modes.put(side, next);
        return next;
    }

    public SideConfigMode getModeForWorldSide(@Nullable Direction worldSide, @Nullable Direction facing) {
        if (worldSide == null) {
            return SideConfigMode.INPUT;
        }
        return getMode(RelativeSide.fromWorldDirection(worldSide, facing));
    }

    public void saveAdditional(CompoundTag tag) {
        CompoundTag sub = new CompoundTag();
        for (RelativeSide side : RelativeSide.values()) {
            sub.putInt(side.getKey(), getMode(side).ordinal());
        }
        tag.put(TAG_SIDE_CONFIG, sub);
    }

    public void loadAdditional(CompoundTag tag) {
        if (!tag.contains(TAG_SIDE_CONFIG, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag sub = tag.getCompound(TAG_SIDE_CONFIG);
        for (RelativeSide side : RelativeSide.values()) {
            if (sub.contains(side.getKey(), Tag.TAG_INT)) {
                modes.put(side, SideConfigMode.fromOrdinal(sub.getInt(side.getKey())));
            }
        }
    }

    @Nullable
    public IFluidHandler wrapFluidHandler(IFluidHandler delegate, @Nullable Direction worldSide, @Nullable Direction facing) {
        if (worldSide == null) {
            return delegate;
        }
        SideConfigMode mode = getModeForWorldSide(worldSide, facing);
        if (mode == SideConfigMode.NONE) {
            return null;
        }
        return new SidedFluidHandler(delegate, mode);
    }

    @Nullable
    public IEnergyStorage wrapEnergyStorage(IEnergyStorage delegate, @Nullable Direction worldSide, @Nullable Direction facing) {
        if (worldSide == null) {
            return delegate;
        }
        SideConfigMode mode = getModeForWorldSide(worldSide, facing);
        if (mode == SideConfigMode.NONE) {
            return null;
        }
        return new SidedEnergyStorage(delegate, mode);
    }

    public boolean canPushTo(@Nullable Direction worldSide, @Nullable Direction facing) {
        return getModeForWorldSide(worldSide, facing) == SideConfigMode.OUTPUT;
    }

    public boolean canPullFrom(@Nullable Direction worldSide, @Nullable Direction facing) {
        return getModeForWorldSide(worldSide, facing) == SideConfigMode.INPUT;
    }

    private record SidedFluidHandler(IFluidHandler delegate, SideConfigMode mode) implements IFluidHandler {

        @Override
        public int getTanks() {
            return delegate.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return delegate.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return delegate.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return delegate.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!mode.canReceive()) {
                return 0;
            }
            return delegate.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!mode.canExtract()) {
                return FluidStack.EMPTY;
            }
            return delegate.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!mode.canExtract()) {
                return FluidStack.EMPTY;
            }
            return delegate.drain(maxDrain, action);
        }
    }

    private record SidedEnergyStorage(IEnergyStorage delegate, SideConfigMode mode) implements IEnergyStorage {

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (!canReceive()) {
                return 0;
            }
            return delegate.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (!canExtract()) {
                return 0;
            }
            return delegate.extractEnergy(toExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return delegate.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return delegate.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return mode.canExtract() && delegate.canExtract();
        }

        @Override
        public boolean canReceive() {
            return mode.canReceive() && delegate.canReceive();
        }
    }
}
