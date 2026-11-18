package com.enrichmentsolution.mod.common.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Custom Forge Energy (FE) storage with change notification and internal read/write helpers
 * for machines and nuclear reactors.
 */
public class ModEnergyStorage extends EnergyStorage {

    private final Runnable onChanged;

    public ModEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        super(capacity, maxReceive, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate) {
            onEnergyChanged();
        }
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = super.extractEnergy(toExtract, simulate);
        if (extracted > 0 && !simulate) {
            onEnergyChanged();
        }
        return extracted;
    }

    /**
     * Allows a machine to consume its own stored energy even when external extraction is disabled.
     */
    public int consumeInternal(int amount, boolean simulate) {
        int consumed = Math.min(this.energy, Math.max(0, amount));
        if (consumed > 0 && !simulate) {
            this.energy -= consumed;
            onEnergyChanged();
        }
        return consumed;
    }

    /**
     * Allows a generator/reactor to produce energy internally even when external insertion is disabled.
     */
    public int generateInternal(int amount, boolean simulate) {
        int generated = Math.min(this.capacity - this.energy, Math.max(0, amount));
        if (generated > 0 && !simulate) {
            this.energy += generated;
            onEnergyChanged();
        }
        return generated;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(this.capacity, energy));
        onEnergyChanged();
    }

    protected void onEnergyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
