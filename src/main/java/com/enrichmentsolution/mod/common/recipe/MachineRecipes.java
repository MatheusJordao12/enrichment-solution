package com.enrichmentsolution.mod.common.recipe;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Registry entries for the machine recipes. One recipe type + one serializer per {@link MachineRecipeKind},
 * so the datapack JSON can use {@code "type": "enrichment_solution:enrichment"} (and so on) directly.
 */
public final class MachineRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, EnrichmentSolutionMod.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, EnrichmentSolutionMod.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> ENRICHMENT_TYPE =
            RECIPE_TYPES.register("enrichment", () -> RecipeType.<MachineRecipe>simple(MachineRecipeKind.ENRICHMENT.id()));

    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> REFINING_TYPE =
            RECIPE_TYPES.register("refining", () -> RecipeType.<MachineRecipe>simple(MachineRecipeKind.REFINING.id()));

    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> STAMPING_TYPE =
            RECIPE_TYPES.register("stamping", () -> RecipeType.<MachineRecipe>simple(MachineRecipeKind.STAMPING.id()));

    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> PCB_PRINTING_TYPE =
            RECIPE_TYPES.register("pcb_printing", () -> RecipeType.<MachineRecipe>simple(MachineRecipeKind.PCB_PRINTING.id()));

    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> PELLETIZING_TYPE =
            RECIPE_TYPES.register("pelletizing", () -> RecipeType.<MachineRecipe>simple(MachineRecipeKind.PELLETIZING.id()));

    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> ENRICHMENT_SERIALIZER =
            RECIPE_SERIALIZERS.register("enrichment", () -> new MachineRecipeSerializer(MachineRecipeKind.ENRICHMENT));

    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> REFINING_SERIALIZER =
            RECIPE_SERIALIZERS.register("refining", () -> new MachineRecipeSerializer(MachineRecipeKind.REFINING));

    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> STAMPING_SERIALIZER =
            RECIPE_SERIALIZERS.register("stamping", () -> new MachineRecipeSerializer(MachineRecipeKind.STAMPING));

    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> PCB_PRINTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("pcb_printing", () -> new MachineRecipeSerializer(MachineRecipeKind.PCB_PRINTING));

    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> PELLETIZING_SERIALIZER =
            RECIPE_SERIALIZERS.register("pelletizing", () -> new MachineRecipeSerializer(MachineRecipeKind.PELLETIZING));

    private MachineRecipes() {
    }

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }

    public static RecipeType<MachineRecipe> typeFor(MachineRecipeKind kind) {
        return switch (kind) {
            case ENRICHMENT -> ENRICHMENT_TYPE.get();
            case REFINING -> REFINING_TYPE.get();
            case STAMPING -> STAMPING_TYPE.get();
            case PCB_PRINTING -> PCB_PRINTING_TYPE.get();
            case PELLETIZING -> PELLETIZING_TYPE.get();
        };
    }

    public static RecipeSerializer<?> serializerFor(MachineRecipeKind kind) {
        return switch (kind) {
            case ENRICHMENT -> ENRICHMENT_SERIALIZER.get();
            case REFINING -> REFINING_SERIALIZER.get();
            case STAMPING -> STAMPING_SERIALIZER.get();
            case PCB_PRINTING -> PCB_PRINTING_SERIALIZER.get();
            case PELLETIZING -> PELLETIZING_SERIALIZER.get();
        };
    }
}
