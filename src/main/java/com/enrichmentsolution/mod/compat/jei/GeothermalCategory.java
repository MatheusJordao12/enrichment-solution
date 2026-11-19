package com.enrichmentsolution.mod.compat.jei;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.registry.ModBlocks;
import com.enrichmentsolution.mod.registry.ModFluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Informative JEI category for the Geothermal Generator,
 * showing the FE/mB conversion rate and closed-loop exhaust gas return.
 */
public class GeothermalCategory implements IRecipeCategory<GeothermalCategory.GeothermalRecipe> {

    public record GeothermalRecipe(int gasMb, int feOutput, boolean maxRate) {}

    public static final RecipeType<GeothermalRecipe> TYPE =
            RecipeType.create(EnrichmentSolutionMod.MOD_ID, "geothermal", GeothermalRecipe.class);

    private static final int WIDTH = 148;
    private static final int HEIGHT = 68;
    private static final ResourceLocation CRAFTING_GUI =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    public GeothermalCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.GEOTHERMAL_GENERATOR.get()));
        this.slot = helper.getSlotDrawable();
        this.arrow = helper.createDrawable(CRAFTING_GUI, 90, 35, 22, 15);
    }

    @Override
    public RecipeType<GeothermalRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.enrichment_solution.geothermal");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GeothermalRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 15, 22)
                .setFluidRenderer(50L, true, 16, 16)
                .addFluidStack(ModFluids.GASEOUS_ISOBUTANE.get(), recipe.gasMb());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 107, 22)
                .setFluidRenderer(50L, true, 16, 16)
                .addFluidStack(ModFluids.GASEOUS_ISOBUTANE.get(), recipe.gasMb());
    }

    @Override
    public void draw(GeothermalRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        this.slot.draw(guiGraphics, 14, 21);
        this.slot.draw(guiGraphics, 106, 21);
        this.arrow.draw(guiGraphics, 58, 22);

        var font = Minecraft.getInstance().font;
        Component rateText = recipe.maxRate()
                ? Component.translatable("jei.enrichment_solution.geothermal.max_rate", recipe.gasMb(), recipe.feOutput())
                : Component.translatable("jei.enrichment_solution.geothermal.unit_rate", recipe.gasMb(), recipe.feOutput());
        guiGraphics.drawString(font, rateText, 10, 6, 0x404040, false);
        guiGraphics.drawString(font,
                Component.translatable("jei.enrichment_solution.geothermal.loop"),
                10, 50, 0x606060, false);
    }
}
