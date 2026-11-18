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

/** JEI category showing the circuits printed by the PCB Printer. */
public class PcbPrintingCategory implements IRecipeCategory<MachineRecipe> {

    public static final RecipeType<MachineRecipe> TYPE =
            RecipeType.create(EnrichmentSolutionMod.MOD_ID, "pcb_printing", MachineRecipe.class);

    private static final int WIDTH = 134;
    private static final int HEIGHT = 54;
    private static final int SLOT_STEP = 18;
    private static final ResourceLocation CRAFTING_GUI =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    public PcbPrintingCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.PCB_PRINTER.get()));
        this.slot = helper.getSlotDrawable();
        this.arrow = helper.createDrawable(CRAFTING_GUI, 90, 35, 22, 15);
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.enrichment_solution.pcb_printing");
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
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            MachineItemInput input = recipe.itemInputs().get(i);
            ItemStack stack = input.displayStack();
            if (!stack.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, 2 + i * SLOT_STEP, 20).addItemStack(stack);
            }
        }
        if (recipe.hasItemOutput()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 20).addItemStack(recipe.itemOutput());
        }
    }

    @Override
    public void draw(MachineRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            this.slot.draw(guiGraphics, 1 + i * SLOT_STEP, 19);
        }
        this.slot.draw(guiGraphics, 112, 19);
        this.arrow.draw(guiGraphics, 96, 20);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.enrichment_solution.pcb_printing.energy"),
                2, 6, 0x606060, false);
    }
}
