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
import net.minecraft.world.level.material.Fluids;

/**
 * Informative JEI category for the Heat Exchanger multiblock,
 * illustrating mB/t boil rates across representative thermal multipliers.
 */
public class HeatExchangerCategory implements IRecipeCategory<HeatExchangerCategory.HeatExchangerRecipe> {

    public record HeatExchangerRecipe(String multiplierLabel, int boilRateMbPerTick, int waterPerTick) {}

    public static final RecipeType<HeatExchangerRecipe> TYPE =
            RecipeType.create(EnrichmentSolutionMod.MOD_ID, "heat_exchanger", HeatExchangerRecipe.class);

    private static final int WIDTH = 148;
    private static final int HEIGHT = 68;
    private static final ResourceLocation CRAFTING_GUI =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    public HeatExchangerCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.HEAT_EXCHANGER.get()));
        this.slot = helper.getSlotDrawable();
        this.arrow = helper.createDrawable(CRAFTING_GUI, 90, 35, 22, 15);
    }

    @Override
    public RecipeType<HeatExchangerRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.enrichment_solution.heat_exchanger");
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
    public void setRecipe(IRecipeLayoutBuilder builder, HeatExchangerRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 11, 22)
                .setFluidRenderer(50L, true, 16, 16)
                .addFluidStack(ModFluids.LIQUID_ISOBUTANE.get(), recipe.boilRateMbPerTick());

        builder.addSlot(RecipeIngredientRole.INPUT, 33, 22)
                .setFluidRenderer(1L, false, 16, 16)
                .addFluidStack(Fluids.WATER, recipe.waterPerTick());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 107, 22)
                .setFluidRenderer(50L, true, 16, 16)
                .addFluidStack(ModFluids.GASEOUS_ISOBUTANE.get(), recipe.boilRateMbPerTick());
    }

    @Override
    public void draw(HeatExchangerRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        this.slot.draw(guiGraphics, 10, 21);
        this.slot.draw(guiGraphics, 32, 21);
        this.slot.draw(guiGraphics, 106, 21);
        this.arrow.draw(guiGraphics, 64, 22);

        var font = Minecraft.getInstance().font;
        guiGraphics.drawString(font,
                Component.translatable("jei.enrichment_solution.heat_exchanger.rate",
                        recipe.multiplierLabel(), recipe.boilRateMbPerTick()),
                10, 6, 0x404040, false);
        guiGraphics.drawString(font,
                Component.translatable("jei.enrichment_solution.heat_exchanger.multiblock"),
                10, 50, 0x606060, false);
    }
}
