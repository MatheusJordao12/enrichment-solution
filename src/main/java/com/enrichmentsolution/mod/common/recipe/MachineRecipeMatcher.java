package com.enrichmentsolution.mod.common.recipe;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntFunction;

/**
 * Unordered slot matching shared by the machines that own several input slots
 * (Stamping Press and PCB Printer), plus the output stacking rule used by every machine.
 */
public final class MachineRecipeMatcher {

    private MachineRecipeMatcher() {
    }

    /**
     * Greedily matches the item inputs of a recipe against machine slots, without touching the inventory.
     *
     * @return the slot used by each item input (indexed like {@link MachineRecipe#itemInputs()}), or {@code null} if the
     * slots do not satisfy the recipe.
     */
    public static int @Nullable [] matchSlots(MachineRecipe recipe, IntFunction<ItemStack> slotGetter, int slotCount) {
        List<MachineItemInput> inputs = recipe.itemInputs();
        if (inputs.isEmpty() || inputs.size() > slotCount) {
            return null;
        }
        int[] matchedSlots = new int[inputs.size()];
        boolean[] slotUsed = new boolean[slotCount];

        for (int input = 0; input < inputs.size(); input++) {
            MachineItemInput requirement = inputs.get(input);
            int foundSlot = -1;
            for (int slot = 0; slot < slotCount; slot++) {
                if (slotUsed[slot]) {
                    continue;
                }
                if (requirement.matches(slotGetter.apply(slot))) {
                    foundSlot = slot;
                    break;
                }
            }
            if (foundSlot < 0) {
                return null;
            }
            slotUsed[foundSlot] = true;
            matchedSlots[input] = foundSlot;
        }
        return matchedSlots;
    }

    /** Whether the machine output slot can accept the recipe result. */
    public static boolean canAcceptOutput(ItemStack current, ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize();
    }
}
