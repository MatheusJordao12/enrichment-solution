package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.menu.OilRefineryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class OilRefineryScreen extends AbstractContainerScreen<OilRefineryMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID,
            "textures/gui/oil_refinery_gui.png"
    );

    private static final int ENERGY_BAR_X = 16;
    private static final int ENERGY_BAR_Y = 18;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 48;

    private static final int INPUT_TANK_X = 48;
    private static final int INPUT_TANK_Y = 18;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 48;

    private static final int ARROW_X = 76;
    private static final int ARROW_Y = 34;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;

    private static final int OUTPUT_TANK_X = 112;
    private static final int OUTPUT_TANK_Y = 18;

    private final SideConfigOverlayWidget sideConfigWidget = new SideConfigOverlayWidget();

    public OilRefineryScreen(OilRefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
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

        // Energy bar (fills from bottom to top)
        int energyHeight = menu.getScaledEnergy(ENERGY_BAR_HEIGHT);
        if (energyHeight > 0) {
            int offset = ENERGY_BAR_HEIGHT - energyHeight;
            guiGraphics.blit(
                    TEXTURE,
                    x + ENERGY_BAR_X,
                    y + ENERGY_BAR_Y + offset,
                    176,
                    offset,
                    ENERGY_BAR_WIDTH,
                    energyHeight
            );
        }

        // Crude Oil input tank
        renderFluidBar(
                guiGraphics,
                x + INPUT_TANK_X,
                y + INPUT_TANK_Y,
                menu.getInputFluidAmount(),
                menu.getInputFluidCapacity(),
                0xFF1F1916,
                0xFF3A2F2A
        );

        // Liquid Isobutane output tank
        renderFluidBar(
                guiGraphics,
                x + OUTPUT_TANK_X,
                y + OUTPUT_TANK_Y,
                menu.getOutputFluidAmount(),
                menu.getOutputFluidCapacity(),
                0xFF38BDF8,
                0xFFBAE6FD
        );

        // Progress arrow (fills left to right)
        if (menu.isCrafting()) {
            int progressWidth = menu.getScaledProgress(ARROW_WIDTH);
            if (progressWidth > 0) {
                guiGraphics.blit(
                        TEXTURE,
                        x + ARROW_X,
                        y + ARROW_Y,
                        176,
                        48,
                        progressWidth,
                        ARROW_HEIGHT
                );
            }
        }
    }

    private static void renderFluidBar(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int amount,
            int capacity,
            int mainColor,
            int highlightColor
    ) {
        if (capacity <= 0 || amount <= 0) {
            return;
        }
        int filled = Math.min(TANK_HEIGHT, Math.max(1, (int) Math.ceil((double) amount / capacity * TANK_HEIGHT)));
        int top = y + (TANK_HEIGHT - filled);
        guiGraphics.fill(x + 1, top, x + TANK_WIDTH - 1, y + TANK_HEIGHT, mainColor);
        guiGraphics.fill(x + 2, top, x + 5, y + TANK_HEIGHT, highlightColor);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        sideConfigWidget.render(guiGraphics, leftPos, topPos, mouseX, mouseY, menu, font);
        renderTooltip(guiGraphics, mouseX, mouseY);
        sideConfigWidget.renderTooltip(guiGraphics, leftPos, topPos, mouseX, mouseY, menu, font);

        if (isHovering(ENERGY_BAR_X - 1, ENERGY_BAR_Y - 1, ENERGY_BAR_WIDTH + 2, ENERGY_BAR_HEIGHT + 2, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "tooltip.enrichment_solution.energy",
                            menu.getEnergyStored(),
                            menu.getMaxEnergyStored()
                    ),
                    mouseX,
                    mouseY
            );
        } else if (isHovering(INPUT_TANK_X - 1, INPUT_TANK_Y - 1, TANK_WIDTH + 2, TANK_HEIGHT + 2, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "tooltip.enrichment_solution.oil_refinery.input",
                            String.format("%,d", menu.getInputFluidAmount()),
                            String.format("%,d", menu.getInputFluidCapacity())
                    ),
                    mouseX,
                    mouseY
            );
        } else if (isHovering(OUTPUT_TANK_X - 1, OUTPUT_TANK_Y - 1, TANK_WIDTH + 2, TANK_HEIGHT + 2, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "tooltip.enrichment_solution.oil_refinery.output",
                            String.format("%,d", menu.getOutputFluidAmount()),
                            String.format("%,d", menu.getOutputFluidCapacity())
                    ),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sideConfigWidget.mouseClicked(
                mouseX, mouseY, button, leftPos, topPos, menu.containerId, OilRefineryMenu.BUTTON_SIDE_BASE, minecraft)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
