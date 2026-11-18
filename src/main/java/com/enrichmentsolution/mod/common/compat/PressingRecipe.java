package com.enrichmentsolution.mod.common.compat;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** JEI snapshot of a Stamping Press recipe (unordered inputs). */
public record PressingRecipe(List<ItemStack> inputs, ItemStack output, int energyPerTick, int ticks) {
}
