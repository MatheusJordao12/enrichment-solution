package com.enrichmentsolution.mod.compat.jei;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.compat.EnrichmentRecipe;
import com.enrichmentsolution.mod.registry.ModBlocks;
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

/** JEI category for the Enrichment Chamber. */
public class EnrichmentCategory implements IRecipeCategory<EnrichmentRecipe> {

    public static final RecipeType<EnrichmentRecipe> TYPE =
            RecipeType.create(EnrichmentSolutionMod.MOD_ID, "enrichment", EnrichmentRecipe.class);

    private static final int WIDTH = 110;
    private static final int HEIGHT = 54;
    private static final ResourceLocation CRAFTING_GUI =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    public EnrichmentCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.ENRICHMENT_CHAMBER.get()));
        this.slot = helper.getSlotDrawable();
        this.arrow = helper.createDrawable(CRAFTING_GUI, 90, 35, 22, 15);
    }

    @Override
    public RecipeType<EnrichmentRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.enrichment_solution.enrichment");
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
    public void setRecipe(IRecipeLayoutBuilder builder, EnrichmentRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 11, 20).addItemStack(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 77, 20).addItemStack(recipe.output());
    }

    @Override
    public void draw(EnrichmentRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        this.slot.draw(guiGraphics, 10, 19);
        this.slot.draw(guiGraphics, 76, 19);
        this.arrow.draw(guiGraphics, 40, 20);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.enrichment_solution.cost",
                        recipe.energyPerTick(), recipe.energyPerTick() * recipe.ticks()),
                10, 6, 0x606060, false);
    }
}
