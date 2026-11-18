package com.enrichmentsolution.mod.common.fluid;

import com.enrichmentsolution.mod.registry.ModDataComponents;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Helper utilities for reading, writing, and mixing thermal data components
 * (temperature and multiplier) on {@link FluidStack} instances.
 */
public final class ThermalFluidHelper {

    public static final double DEFAULT_TEMPERATURE = 20.0D;
    public static final double DEFAULT_MULTIPLIER = 1.0D;

    private ThermalFluidHelper() {}

    /**
     * Returns the fluid temperature in Celsius, defaulting to {@code 20.0D} if absent.
     */
    public static double getTemperature(FluidStack stack) {
        if (stack.isEmpty()) {
            return DEFAULT_TEMPERATURE;
        }
        return stack.getOrDefault(ModDataComponents.FLUID_TEMPERATURE.get(), DEFAULT_TEMPERATURE);
    }

    /**
     * Returns the thermal multiplier, defaulting to {@code 1.0D} if absent.
     */
    public static double getMultiplier(FluidStack stack) {
        if (stack.isEmpty()) {
            return DEFAULT_MULTIPLIER;
        }
        return stack.getOrDefault(ModDataComponents.FLUID_MULTIPLIER.get(), DEFAULT_MULTIPLIER);
    }

    /**
     * Attaches thermal components to {@code stack} when {@code temperature > 20.0D} or
     * {@code multiplier > 1.0D}, or clears them otherwise so ambient water stays component-free.
     */
    public static FluidStack withThermalData(FluidStack stack, double temperature, double multiplier) {
        if (stack.isEmpty()) {
            return stack;
        }
        if (temperature > DEFAULT_TEMPERATURE || multiplier > DEFAULT_MULTIPLIER) {
            stack.set(ModDataComponents.FLUID_TEMPERATURE.get(), temperature);
            stack.set(ModDataComponents.FLUID_MULTIPLIER.get(), multiplier);
        } else {
            stack.remove(ModDataComponents.FLUID_TEMPERATURE.get());
            stack.remove(ModDataComponents.FLUID_MULTIPLIER.get());
        }
        return stack;
    }

    /**
     * Mixes {@code acceptedAmount} mB of {@code incoming} into {@code existing} using a
     * volume-weighted average of both temperature and multiplier.
     */
    public static FluidStack mergeThermalStacks(FluidStack existing, FluidStack incoming, int acceptedAmount) {
        if (acceptedAmount <= 0) {
            return existing.copy();
        }
        if (existing.isEmpty()) {
            return withThermalData(
                    incoming.copyWithAmount(acceptedAmount),
                    getTemperature(incoming),
                    getMultiplier(incoming));
        }

        int existingAmount = existing.getAmount();
        int totalAmount = existingAmount + acceptedAmount;
        double avgTemp = (getTemperature(existing) * existingAmount + getTemperature(incoming) * acceptedAmount) / totalAmount;
        double avgMult = (getMultiplier(existing) * existingAmount + getMultiplier(incoming) * acceptedAmount) / totalAmount;

        FluidStack merged = existing.copyWithAmount(totalAmount);
        return withThermalData(merged, avgTemp, avgMult);
    }
}
