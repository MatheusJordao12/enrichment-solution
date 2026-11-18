package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.menu.HeatExchangerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the Heat Exchanger.
 * Renders the 3 fluid tanks (Water, Liquid Isobutane, Gaseous Isobutane),
 * thermal multiplier bar, boiling progress arrow, and hover tooltips.
 */
public class HeatExchangerScreen extends AbstractContainerScreen<HeatExchangerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID,
            "textures/gui/heat_exchanger_gui.png"
    );

    private static final int WATER_X = 26;
    private static final int LIQUID_X = 52;
    private static final int GAS_X = 116;
    private static final int TANK_Y = 18;
    private static final int TANK_W = 16;
    private static final int TANK_H = 50;

    private static final int TEMP_X = 148;
    private static final int TEMP_Y = 18;
    private static final int TEMP_W = 14;
    private static final int TEMP_H = 50;

    private static final int ARROW_X = 80;
    private static final int ARROW_Y = 35;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;

    private final SideConfigOverlayWidget sideConfigWidget = new SideConfigOverlayWidget();

    public HeatExchangerScreen(HeatExchangerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY - 1, 0xC8F0D0, false);
        int blockCount = menu.getBlockCount();
        if (blockCount > 1) {
            guiGraphics.drawString(font, "(" + blockCount + "x)", titleLabelX + font.width(title) + 5, titleLabelY - 1, 0x38BDF8, false);
        }
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xA8B8AE, false);

        if (menu.isBoiling()) {
            guiGraphics.drawString(font, menu.getBoilRate() + " mB/t", 75, 56, 0xFB923C, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderFluidBar(guiGraphics, x + WATER_X, y + TANK_Y, menu.getWaterAmount(), menu.getWaterCapacity(), 0xFF0284C7, 0xFF38BDF8);
        renderFluidBar(guiGraphics, x + LIQUID_X, y + TANK_Y, menu.getLiquidAmount(), menu.getLiquidCapacity(), 0xFF0891B2, 0xFF67E8F9);
        renderFluidBar(guiGraphics, x + GAS_X, y + TANK_Y, menu.getGasAmount(), menu.getGasCapacity(), 0xFF94A3B8, 0xFFE0F2FE);

        // Thermal bar
        double mult = menu.getMultiplier();
        double norm = Math.min(1.0D, mult / 50.0D);
        int tFilled = Math.max(2, (int) Math.ceil(norm * TEMP_H));
        int tTop = y + TEMP_Y + (TEMP_H - tFilled);
        int barColor = mult >= 15.0D ? 0xFFEF4444 : (mult >= 4.0D ? 0xFFF97316 : 0xFF38BDF8);
        guiGraphics.fill(x + TEMP_X + 1, tTop, x + TEMP_X + TEMP_W - 1, y + TEMP_Y + TEMP_H, barColor);

        // Boiling progress arrow animation when active
        if (menu.isBoiling()) {
            int animWidth = (int) ((System.currentTimeMillis() / 60L) % (ARROW_W + 1));
            if (animWidth > 0) {
                guiGraphics.blit(TEXTURE, x + ARROW_X, y + ARROW_Y, 176, 48, animWidth, ARROW_H);
            }
        }
    }

    private void renderFluidBar(GuiGraphics g, int fx, int fy, int amount, int capacity, int bodyColor, int highlightColor) {
        if (capacity > 0 && amount > 0) {
            int filled = Math.min(TANK_H, (int) Math.ceil((double) amount / capacity * TANK_H));
            int top = fy + (TANK_H - filled);
            g.fill(fx + 1, top, fx + TANK_W - 1, fy + TANK_H, bodyColor);
            g.fill(fx + 2, top, fx + TANK_W - 4, fy + TANK_H, highlightColor);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        sideConfigWidget.render(guiGraphics, leftPos, topPos, mouseX, mouseY, menu, font);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(WATER_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            int waterAmount = menu.getWaterAmount();
            guiGraphics.renderTooltip(
                    font,
                    waterAmount > 0
                            ? Component.translatable("tooltip.enrichment_solution.heat_exchanger.water_temperature",
                                    String.format("%,d", waterAmount),
                                    String.format("%,d", menu.getWaterCapacity()),
                                    String.format("%.1f", menu.getWaterTemperature()))
                            : Component.translatable("tooltip.enrichment_solution.heat_exchanger.water",
                                    String.format("%,d", waterAmount),
                                    String.format("%,d", menu.getWaterCapacity())),
                    mouseX, mouseY);
        } else if (isHovering(LIQUID_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("tooltip.enrichment_solution.heat_exchanger.liquid",
                            String.format("%,d", menu.getLiquidAmount()),
                            String.format("%,d", menu.getLiquidCapacity())),
                    mouseX, mouseY);
        } else if (isHovering(GAS_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("tooltip.enrichment_solution.heat_exchanger.gas",
                            String.format("%,d", menu.getGasAmount()),
                            String.format("%,d", menu.getGasCapacity())),
                    mouseX, mouseY);
        } else if (isHovering(TEMP_X, TEMP_Y, TEMP_W, TEMP_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("tooltip.enrichment_solution.heat_exchanger.temp",
                            String.format("%.1f", menu.getTemperature()),
                            String.format("%.2f", menu.getMultiplier())),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        sideConfigWidget.renderTooltip(guiGraphics, leftPos, topPos, mouseX, mouseY, menu, font);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sideConfigWidget.mouseClicked(mouseX, mouseY, button, leftPos, topPos, menu.containerId, HeatExchangerMenu.BUTTON_SIDE_BASE, minecraft)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
