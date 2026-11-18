package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.common.menu.StampingPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class StampingPressScreen extends AbstractContainerScreen<StampingPressMenu> {

    private static final int BAR_X = 150;
    private static final int BAR_Y = 17;
    private static final int BAR_W = 14;
    private static final int BAR_H = 52;

    private final SideConfigOverlayWidget sideConfigWidget = new SideConfigOverlayWidget();

    public StampingPressScreen(StampingPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Vanilla-style 3D panel background
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(x, y, x + imageWidth - 1, y + 1, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + 1, y + imageHeight - 1, 0xFFFFFFFF);
        guiGraphics.fill(x + 1, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);
        guiGraphics.fill(x + imageWidth - 1, y + 1, x + imageWidth, y + imageHeight, 0xFF555555);

        // 2x2 Input slots
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                drawSlot(guiGraphics, x + 33 + col * 18, y + 23 + row * 18);
            }
        }

        // Output slot
        drawSlot(guiGraphics, x + 111, y + 32);

        // Progress bar
        int px = x + 78;
        int py = y + 37;
        guiGraphics.fill(px, py, px + 24, py + 8, 0xFF555555);
        int progress = menu.getProgress();
        int maxProgress = menu.getMaxProgress();
        if (maxProgress > 0 && progress > 0) {
            int filled = Math.min(24, (progress * 24) / maxProgress);
            guiGraphics.fill(px, py, px + filled, py + 8, 0xFF38BDF8);
        }

        // Vertical Energy Bar
        int bxBar = x + BAR_X;
        int byBar = y + BAR_Y;
        guiGraphics.fill(bxBar - 1, byBar - 1, bxBar + BAR_W + 1, byBar + BAR_H + 1, 0xFF373737);
        guiGraphics.fill(bxBar, byBar, bxBar + BAR_W, byBar + BAR_H, 0xFF1E293B);
        int energy = menu.getEnergyStored();
        int maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy > 0 && energy > 0) {
            int filled = Math.min(BAR_H, (int) Math.ceil((double) energy / maxEnergy * BAR_H));
            int top = byBar + (BAR_H - filled);
            guiGraphics.fill(bxBar + 1, top, bxBar + BAR_W - 1, byBar + BAR_H, 0xFF16A34A);
            guiGraphics.fill(bxBar + 2, top, bxBar + BAR_W - 4, byBar + BAR_H, 0xFF4ADE80);
        }

        // Player inventory (3x9) & hotbar (1x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(guiGraphics, x + 7 + col * 18, y + 83 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(guiGraphics, x + 7 + col * 18, y + 141);
        }
    }

    private static void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sideConfigWidget.mouseClicked(mouseX, mouseY, button, leftPos, topPos, menu.containerId, StampingPressMenu.BUTTON_SIDE_BASE, minecraft)) {
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

        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "tooltip.enrichment_solution.energy",
                            String.format("%,d", menu.getEnergyStored()),
                            String.format("%,d", menu.getMaxEnergyStored())
                    ),
                    mouseX,
                    mouseY
            );
        }
    }
}
