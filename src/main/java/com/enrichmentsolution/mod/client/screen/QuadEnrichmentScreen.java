package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.menu.QuadEnrichmentMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class QuadEnrichmentScreen extends AbstractContainerScreen<QuadEnrichmentMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/gui/enrichment_chamber_gui.png"
    );

    private static final int ENERGY_BAR_X = 152;
    private static final int ENERGY_BAR_Y = 22;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 48;

    private Button autoSortButton;
    private final SideConfigOverlayWidget sideConfigWidget = new SideConfigOverlayWidget();

    public QuadEnrichmentScreen(QuadEnrichmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        autoSortButton = Button.builder(getAutoSortLabel(), btn -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, QuadEnrichmentMenu.BUTTON_TOGGLE_AUTO_SORT);
            }
        }).bounds(leftPos + 124, topPos + 30, 24, 16).build();
        addRenderableWidget(autoSortButton);
    }

    private Component getAutoSortLabel() {
        return Component.literal(menu.isAutoSort() ? "S:ON" : "S:OFF");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (autoSortButton != null) {
            autoSortButton.setMessage(getAutoSortLabel());
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Draw base GUI frame + player inventory + energy bar border
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Cover the entire upper machine area cleanly with the dark slate panel color
        guiGraphics.fill(x + 7, y + 15, x + 149, y + 71, 0xFF282D35);

        // Draw 4 input slots (y=18) and 4 output slots (y=52) + 4 progress bars
        int maxProgress = menu.getMaxProgress();
        for (int i = 0; i < 4; i++) {
            int sx = x + 35 + i * 22;

            // Input slot box (18x18 around 16x16 slot)
            drawSlotBox(guiGraphics, sx - 1, y + 17);
            // Output slot box
            drawSlotBox(guiGraphics, sx - 1, y + 51);

            // Vertical progress bar (between y=37 and y=49, height=12)
            guiGraphics.fill(sx + 5, y + 37, sx + 11, y + 49, 0xFF181A1E);
            int prog = menu.getProgress(i);
            if (prog > 0) {
                int filled = Math.min(12, (int) Math.ceil((double) prog / maxProgress * 12.0));
                guiGraphics.fill(sx + 6, y + 37, sx + 10, y + 37 + filled, 0xFF4CFF50);
            }
        }

        // Energy bar fill
        int energy = menu.getEnergyStored();
        int maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy > 0 && energy > 0) {
            int filled = (int) Math.ceil((double) energy / maxEnergy * ENERGY_BAR_HEIGHT);
            filled = Math.min(filled, ENERGY_BAR_HEIGHT);
            int offset = ENERGY_BAR_HEIGHT - filled;
            guiGraphics.blit(TEXTURE, x + ENERGY_BAR_X, y + ENERGY_BAR_Y + offset,
                    176, offset, ENERGY_BAR_WIDTH, filled);
        }
    }

    private void drawSlotBox(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF1E2228);
        g.fill(x, y, x + 17, y + 1, 0xFF14171C);
        g.fill(x, y, x + 1, y + 17, 0xFF14171C);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFF444A54);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFF444A54);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF20242A);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xE0E0E0, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xA0A0A0, false);

        Component stateText = menu.isLit()
                ? Component.translatable("gui.enrichment_solution.quad_enrichment.on")
                : Component.translatable("gui.enrichment_solution.quad_enrichment.off");
        int color = menu.isLit() ? 0x55FF55 : 0xFF5555;
        guiGraphics.drawString(font, stateText, 126, 52, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sideConfigWidget.mouseClicked(mouseX, mouseY, button, leftPos, topPos, menu.containerId, QuadEnrichmentMenu.BUTTON_SIDE_BASE, minecraft)) {
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

        if (isHovering(ENERGY_BAR_X, ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("tooltip.enrichment_solution.energy", menu.getEnergyStored(), menu.getMaxEnergyStored()),
                    mouseX,
                    mouseY
            );
        }
    }
}
