package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.menu.EnrichmentChamberMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class EnrichmentChamberScreen extends AbstractContainerScreen<EnrichmentChamberMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID,
            "textures/gui/enrichment_chamber_gui.png"
    );

    private static final int ENERGY_BAR_X = 19;
    private static final int ENERGY_BAR_Y = 19;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 48;

    private static final int ARROW_X = 80;
    private static final int ARROW_Y = 35;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;

    private final SideConfigOverlayWidget sideConfigWidget = new SideConfigOverlayWidget();

    public EnrichmentChamberScreen(EnrichmentChamberMenu menu, Inventory playerInventory, Component title) {
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sideConfigWidget.mouseClicked(mouseX, mouseY, button, leftPos, topPos, menu.containerId, EnrichmentChamberMenu.BUTTON_SIDE_BASE, minecraft)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        }
    }
}
