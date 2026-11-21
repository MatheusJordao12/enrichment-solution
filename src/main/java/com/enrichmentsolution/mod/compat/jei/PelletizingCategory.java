package com.enrichmentsolution.mod.compat.jei;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.recipe.MachineItemInput;
import com.enrichmentsolution.mod.common.recipe.MachineRecipe;
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

/** JEI category for the Pelletizer multiblock. */
public class PelletizingCategory implements IRecipeCategory<MachineRecipe> {

    public static final RecipeType<MachineRecipe> TYPE =
            RecipeType.create(EnrichmentSolutionMod.MOD_ID, "pelletizing", MachineRecipe.class);

    private static final int WIDTH = 148;
    private static final int HEIGHT = 54;
    private static final ResourceLocation CRAFTING_GUI =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    public PelletizingCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.PELLETIZER_CONTROLLER.get()));
        this.slot = helper.getSlotDrawable();
        this.arrow = helper.createDrawable(CRAFTING_GUI, 90, 35, 22, 15);
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.enrichment_solution.pelletizing");
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
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
        if (!recipe.itemInputs().isEmpty()) {
            MachineItemInput input = recipe.itemInputs().get(0);
            builder.addSlot(RecipeIngredientRole.INPUT, 24, 22).addItemStack(input.displayStack());
        }
        if (recipe.hasItemOutput()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 106, 22).addItemStack(recipe.itemOutput());
        }
    }

    @Override
    public void draw(MachineRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        this.slot.draw(guiGraphics, 23, 21);
        this.slot.draw(guiGraphics, 105, 21);
        this.arrow.draw(guiGraphics, 63, 22);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.enrichment_solution.pelletizing.cost",
                        recipe.energyPerTick(), recipe.ticks(), recipe.totalEnergy()),
                4, 6, 0x606060, false);
    }
}
