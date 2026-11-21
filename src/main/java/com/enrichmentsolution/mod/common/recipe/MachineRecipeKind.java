package com.enrichmentsolution.mod.common.recipe;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Machine that owns a {@link MachineRecipe}.
 * <p>
 * Each kind has its own {@link net.minecraft.world.item.crafting.RecipeType} and
 * {@link net.minecraft.world.item.crafting.RecipeSerializer}, so the JSON {@code type} is simply
 * {@code enrichment_solution:enrichment}, {@code enrichment_solution:refining}, {@code enrichment_solution:stamping}
 * or {@code enrichment_solution:pcb_printing}.
 */
public enum MachineRecipeKind {

    ENRICHMENT("enrichment"),
    REFINING("refining"),
    STAMPING("stamping"),
    PCB_PRINTING("pcb_printing"),
    PELLETIZING("pelletizing");

    private final String name;

    MachineRecipeKind(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /** Registry id shared by the recipe type and the recipe serializer of this kind. */
    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, this.name);
    }

    @Nullable
    public static MachineRecipeKind byName(String name) {
        for (MachineRecipeKind kind : values()) {
            if (kind.name.equals(name)) {
                return kind;
            }
        }
        return null;
    }
}
