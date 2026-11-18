package com.enrichmentsolution.mod.common.compat;

import net.minecraft.world.item.ItemStack;

/** JEI snapshot of an Enrichment Chamber recipe. */
public record EnrichmentRecipe(ItemStack input, ItemStack output, int energyPerTick, int ticks) {
}
