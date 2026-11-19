package com.enrichmentsolution.mod.compat.jei;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.recipe.MachineRecipe;
import com.enrichmentsolution.mod.common.recipe.MachineRecipeKind;
import com.enrichmentsolution.mod.common.recipe.MachineRecipes;
import com.enrichmentsolution.mod.registry.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Comparator;
import java.util.List;

/** Registers the datapack machine recipes of the mod as JEI categories. */
@JeiPlugin
public class EnrichmentJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "jei_plugin");

    /** Most specific recipe first (same order the machines use), then stable by output name. */
    private static final Comparator<MachineRecipe> DISPLAY_ORDER = Comparator
            .comparingInt((MachineRecipe recipe) -> recipe.itemInputs().size())
            .reversed()
            .thenComparing(Comparator.comparing((MachineRecipe recipe) -> recipe.itemOutput().getDescriptionId()));

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new PcbPrintingCategory(guiHelper),
                new EnrichmentCategory(guiHelper),
                new RefiningCategory(guiHelper),
                new PressingCategory(guiHelper),
                new HeatExchangerCategory(guiHelper),
                new GeothermalCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RecipeManager manager = level.getRecipeManager();
        registration.addRecipes(EnrichmentCategory.TYPE, recipesOf(manager, MachineRecipeKind.ENRICHMENT));
        registration.addRecipes(RefiningCategory.TYPE, recipesOf(manager, MachineRecipeKind.REFINING));
        registration.addRecipes(PressingCategory.TYPE, recipesOf(manager, MachineRecipeKind.STAMPING));
        registration.addRecipes(PcbPrintingCategory.TYPE, recipesOf(manager, MachineRecipeKind.PCB_PRINTING));
        registration.addRecipes(HeatExchangerCategory.TYPE, List.of(
                new HeatExchangerCategory.HeatExchangerRecipe("1x", 5, 1),
                new HeatExchangerCategory.HeatExchangerRecipe("2.25x", 11, 1),
                new HeatExchangerCategory.HeatExchangerRecipe("3x", 15, 1),
                new HeatExchangerCategory.HeatExchangerRecipe("10x+", 50, 1)
        ));
        registration.addRecipes(GeothermalCategory.TYPE, List.of(
                new GeothermalCategory.GeothermalRecipe(1, 20, false),
                new GeothermalCategory.GeothermalRecipe(50, 1_000, true)
        ));
    }

    /** Same source as the machines: the recipe manager of the current level. */
    private static List<MachineRecipe> recipesOf(RecipeManager manager, MachineRecipeKind kind) {
        return manager.<RecipeInput, MachineRecipe>getAllRecipesFor(MachineRecipes.typeFor(kind)).stream()
                .map(RecipeHolder::value)
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PCB_PRINTER.get()), PcbPrintingCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ENRICHMENT_CHAMBER.get()), EnrichmentCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.OIL_REFINERY.get()), RefiningCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.STAMPING_PRESS.get()), PressingCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.HEAT_EXCHANGER.get()), HeatExchangerCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.TANK_BLOCK.get()), HeatExchangerCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GEOTHERMAL_GENERATOR.get()), GeothermalCategory.TYPE);
    }
}
