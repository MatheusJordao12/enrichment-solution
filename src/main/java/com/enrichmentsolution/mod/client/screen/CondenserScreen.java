package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.menu.CondenserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CondenserScreen extends AbstractContainerScreen<CondenserMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID,
            "textures/gui/condenser_gui.png"
    );

    private static final int TANK_W = 16;
    private static final int TANK_H = 50;
    private static final int TANK_Y = 18;

    private static final int GAS_X = 26;
    private static final int WATER_X = 52;
    private static final int LIQUID_X = 134;

    private static final int ARROW_X = 89;
    private static final int ARROW_Y = 35;
    private static final int ARROW_W = 25;
    private static final int ARROW_H = 17;

    private final SideConfigOverlayWidget sideConfigWidget = new SideConfigOverlayWidget();

    public CondenserScreen(CondenserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY - 1, 0xC8F0D0, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xA8B8AE, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        drawFluidBar(guiGraphics, x + GAS_X, y + TANK_Y, menu.getGasAmount(), menu.getGasCapacity(), 0xFF7DD3FC, 0xFFE0F2FE);
        drawFluidBar(guiGraphics, x + WATER_X, y + TANK_Y, menu.getWaterAmount(), menu.getWaterCapacity(), 0xFF0284C7, 0xFF38BDF8);
        drawFluidBar(guiGraphics, x + LIQUID_X, y + TANK_Y, menu.getLiquidAmount(), menu.getLiquidCapacity(), 0xFF0891B2, 0xFF67E8F9);

        if (menu.isCondensing()) {
            guiGraphics.blit(TEXTURE, x + ARROW_X, y + ARROW_Y, 176, 48, ARROW_W, ARROW_H);
        }
    }

    private static void drawFluidBar(GuiGraphics g, int x, int y, int amount, int capacity, int mainColor, int highlightColor) {
        if (capacity <= 0 || amount <= 0) {
            return;
        }
        int filled = Math.min(TANK_H, (int) Math.ceil((double) amount / capacity * TANK_H));
        int top = y + (TANK_H - filled);
        g.fill(x + 1, top, x + TANK_W - 1, y + TANK_H, mainColor);
        g.fill(x + 2, top, x + TANK_W - 4, y + TANK_H, highlightColor);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        sideConfigWidget.render(guiGraphics, leftPos, topPos, mouseX, mouseY, menu, font);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(GAS_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("tooltip.enrichment_solution.condenser.gas",
                            menu.getGasAmount(), menu.getGasCapacity()),
                    mouseX, mouseY
            );
        } else if (isHovering(WATER_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("tooltip.enrichment_solution.condenser.water",
                            menu.getWaterAmount(), menu.getWaterCapacity()),
                    mouseX, mouseY
            );
        } else if (isHovering(LIQUID_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("tooltip.enrichment_solution.condenser.liquid",
                            menu.getLiquidAmount(), menu.getLiquidCapacity()),
                    mouseX, mouseY
            );
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        sideConfigWidget.renderTooltip(guiGraphics, leftPos, topPos, mouseX, mouseY, menu, font);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sideConfigWidget.mouseClicked(mouseX, mouseY, button, leftPos, topPos, menu.containerId, CondenserMenu.BUTTON_SIDE_BASE, minecraft)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
