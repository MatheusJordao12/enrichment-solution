package com.enrichmentsolution.mod.common.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * Datapack driven recipe used by every processing machine of the mod.
 * <p>
 * {@code fluidInput} / {@code fluidOutput} are {@link FluidStack#EMPTY} when the machine does not use fluids and
 * {@code itemOutput} is empty when the machine does not produce items.
 * The recipe type/serializer (and therefore the JSON {@code type}) is decided by {@link #kind()}.
 */
public record MachineRecipe(
        MachineRecipeKind kind,
        List<MachineItemInput> itemInputs,
        FluidStack fluidInput,
        ItemStack itemOutput,
        FluidStack fluidOutput,
        int energyPerTick,
        int ticks
) implements Recipe<RecipeInput> {

    public MachineRecipe {
        itemInputs = itemInputs == null ? List.of() : List.copyOf(itemInputs);
        fluidInput = fluidInput == null ? FluidStack.EMPTY : fluidInput;
        itemOutput = itemOutput == null ? ItemStack.EMPTY : itemOutput;
        fluidOutput = fluidOutput == null ? FluidStack.EMPTY : fluidOutput;
        energyPerTick = Math.max(0, energyPerTick);
        ticks = Math.max(1, ticks);
    }

    public boolean hasItemInputs() {
        return !this.itemInputs.isEmpty();
    }

    public boolean hasItemOutput() {
        return !this.itemOutput.isEmpty();
    }

    public boolean hasFluidInput() {
        return !this.fluidInput.isEmpty();
    }

    public boolean hasFluidOutput() {
        return !this.fluidOutput.isEmpty();
    }

    /** Total energy consumed by one operation, for displays. */
    public int totalEnergy() {
        return this.energyPerTick * this.ticks;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return MachineRecipeMatcher.matchSlots(this, input::getItem, input.size()) != null;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return this.itemOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.itemOutput;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (MachineItemInput input : this.itemInputs) {
            ingredients.add(input.ingredient());
        }
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MachineRecipes.serializerFor(this.kind);
    }

    @Override
    public RecipeType<?> getType() {
        return MachineRecipes.typeFor(this.kind);
    }

    /** Machine recipes are never recorded in the recipe book, so they are never filtered out as "incomplete". */
    @Override
    public boolean isIncomplete() {
        return false;
    }
}
