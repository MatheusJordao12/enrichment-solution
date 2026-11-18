package com.enrichmentsolution.mod.common.compat;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Plain snapshot of a PCB Printer recipe, used by the JEI plugin without exposing the machine internals.
 */
public record PcbPrintingRecipe(ItemStack output, List<ItemStack> inputs, int energyPerTick) {

    public PcbPrintingRecipe(ItemStack output, List<ItemStack> inputs) {
        this(output, inputs, 0);
    }
}
