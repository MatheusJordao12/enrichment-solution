package com.enrichmentsolution.mod.common.compat;

import net.neoforged.neoforge.fluids.FluidStack;

/** JEI snapshot of an Oil Refinery recipe. */
public record RefiningRecipe(FluidStack input, FluidStack output, int energyPerTick, int ticks) {
}
