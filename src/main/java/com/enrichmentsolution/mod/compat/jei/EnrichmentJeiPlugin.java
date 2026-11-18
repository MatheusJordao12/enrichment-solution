package com.enrichmentsolution.mod.compat.jei;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.block.entity.EnrichmentChamberBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.OilRefineryBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.PcbPrinterBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.StampingPressBlockEntity;
import com.enrichmentsolution.mod.registry.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Registers the machine recipes of the mod as JEI categories. */
@JeiPlugin
public class EnrichmentJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "jei_plugin");

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
                new PressingCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PcbPrintingCategory.TYPE, PcbPrinterBlockEntity.getPrintingRecipes());
        registration.addRecipes(EnrichmentCategory.TYPE, EnrichmentChamberBlockEntity.getJeiRecipes());
        registration.addRecipes(RefiningCategory.TYPE, OilRefineryBlockEntity.getJeiRecipes());
        registration.addRecipes(PressingCategory.TYPE, StampingPressBlockEntity.getJeiRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PCB_PRINTER.get()), PcbPrintingCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ENRICHMENT_CHAMBER.get()), EnrichmentCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.OIL_REFINERY.get()), RefiningCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.STAMPING_PRESS.get()), PressingCategory.TYPE);
    }
}
