package com.enrichmentsolution.mod.common.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Per block entity cache of the recipes of one {@link MachineRecipeKind}.
 * <p>
 * The recipe list is reloaded only when the {@link RecipeManager} instance changes (world load, {@code /reload} or a
 * datapack sync), otherwise the cached copy is reused. Recipes are sorted so that the most specific recipe comes first:
 * more item inputs first, then recipe id, which keeps machine matching deterministic regardless of map iteration order.
 */
public final class MachineRecipeCache {

    private static final Comparator<RecipeHolder<MachineRecipe>> PREFERENCE = Comparator
            .<RecipeHolder<MachineRecipe>>comparingInt(holder -> holder.value().itemInputs().size())
            .reversed()
            .thenComparing(holder -> holder.id().toString());

    private final MachineRecipeKind kind;
    @Nullable
    private RecipeManager cachedManager;
    private List<RecipeHolder<MachineRecipe>> cachedRecipes = List.of();

    public MachineRecipeCache(MachineRecipeKind kind) {
        this.kind = kind;
    }

    public MachineRecipeKind kind() {
        return this.kind;
    }

    /** All recipes of this kind, ordered by {@link #PREFERENCE}. */
    public List<RecipeHolder<MachineRecipe>> get(Level level) {
        RecipeManager manager = level.getRecipeManager();
        if (manager != this.cachedManager) {
            List<RecipeHolder<MachineRecipe>> loaded = new ArrayList<>(
                    manager.<RecipeInput, MachineRecipe>getAllRecipesFor(MachineRecipes.typeFor(this.kind)));
            loaded.sort(PREFERENCE);
            this.cachedRecipes = List.copyOf(loaded);
            this.cachedManager = manager;
        }
        return this.cachedRecipes;
    }

    /** First recipe (in preference order) that satisfies the filter. */
    @Nullable
    public RecipeHolder<MachineRecipe> find(Level level, Predicate<MachineRecipe> filter) {
        for (RecipeHolder<MachineRecipe> holder : this.get(level)) {
            if (filter.test(holder.value())) {
                return holder;
            }
        }
        return null;
    }

    @Nullable
    public RecipeHolder<MachineRecipe> byId(Level level, @Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        for (RecipeHolder<MachineRecipe> holder : this.get(level)) {
            if (holder.id().equals(id)) {
                return holder;
            }
        }
        return null;
    }
}
