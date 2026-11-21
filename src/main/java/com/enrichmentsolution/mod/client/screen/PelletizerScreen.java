package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.common.menu.PelletizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PelletizerScreen extends AbstractContainerScreen<PelletizerMenu> {

    private static final int ENERGY_BAR_X = 19;
    private static final int ENERGY_BAR_Y = 19;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 48;

    private static final int INPUT_SLOT_X = 56;
    private static final int INPUT_SLOT_Y = 35;
    private static final int OUTPUT_SLOT_X = 116;
    private static final int OUTPUT_SLOT_Y = 35;

    private static final int ARROW_X = 80;
    private static final int ARROW_Y = 35;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;

    private static final int BADGE_X = 38;
    private static final int BADGE_Y = 56;
    private static final int BADGE_WIDTH = 130;
    private static final int BADGE_HEIGHT = 13;

    public PelletizerScreen(PelletizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Vanilla-style 3D panel background (176x166)
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(x, y, x + imageWidth - 1, y + 1, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + 1, y + imageHeight - 1, 0xFFFFFFFF);
        guiGraphics.fill(x + 1, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);
        guiGraphics.fill(x + imageWidth - 1, y + 1, x + imageWidth, y + imageHeight, 0xFF555555);

        // Energy bar on left (fills from bottom to top)
        int bx = x + ENERGY_BAR_X;
        int by = y + ENERGY_BAR_Y;
        guiGraphics.fill(bx - 1, by - 1, bx + ENERGY_BAR_WIDTH + 1, by + ENERGY_BAR_HEIGHT + 1, 0xFF373737);
        guiGraphics.fill(bx, by, bx + ENERGY_BAR_WIDTH, by + ENERGY_BAR_HEIGHT, 0xFF1E293B);
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();
        if (maxEnergy > 0 && energy > 0) {
            int filled = Math.min(ENERGY_BAR_HEIGHT, (int) Math.ceil((double) energy / maxEnergy * ENERGY_BAR_HEIGHT));
            int top = by + (ENERGY_BAR_HEIGHT - filled);
            guiGraphics.fill(bx + 1, top, bx + ENERGY_BAR_WIDTH - 1, by + ENERGY_BAR_HEIGHT, 0xFF16A34A);
            guiGraphics.fill(bx + 2, top, bx + ENERGY_BAR_WIDTH - 4, by + ENERGY_BAR_HEIGHT, 0xFF4ADE80);
        }

        // Input and output slots
        drawSlot(guiGraphics, x + INPUT_SLOT_X - 1, y + INPUT_SLOT_Y - 1);
        drawSlot(guiGraphics, x + OUTPUT_SLOT_X - 1, y + OUTPUT_SLOT_Y - 1);

        // Progress arrow (fills left to right)
        drawArrow(guiGraphics, x + ARROW_X, y + ARROW_Y, 0xFF7A7A7A);
        int progress = menu.getProgress();
        int maxProgress = menu.getMaxProgress();
        if (maxProgress > 0 && progress > 0) {
            int progressWidth = Math.min(ARROW_WIDTH, (int) Math.round((double) progress / maxProgress * ARROW_WIDTH));
            if (progressWidth > 0) {
                guiGraphics.enableScissor(x + ARROW_X, y + ARROW_Y, x + ARROW_X + progressWidth, y + ARROW_Y + ARROW_HEIGHT);
                drawArrow(guiGraphics, x + ARROW_X, y + ARROW_Y, 0xFF22C55E);
                guiGraphics.disableScissor();
            }
        }

        // Multiblock structure status badge
        boolean valid = menu.isStructureValid();
        int badgeX = x + BADGE_X;
        int badgeY = y + BADGE_Y;
        int borderColor = valid ? 0xFF22C55E : 0xFFEF4444;
        int fillColor = valid ? 0xFF14532D : 0xFF7F1D1D;
        int textColor = valid ? 0xFF86EFAC : 0xFFFCA5A5;
        guiGraphics.fill(badgeX, badgeY, badgeX + BADGE_WIDTH, badgeY + BADGE_HEIGHT, borderColor);
        guiGraphics.fill(badgeX + 1, badgeY + 1, badgeX + BADGE_WIDTH - 1, badgeY + BADGE_HEIGHT - 1, fillColor);

        Component statusText = Component.translatable(valid
                ? "gui.enrichment_solution.pelletizer.structure_valid"
                : "gui.enrichment_solution.pelletizer.structure_incomplete");
        float scale = 0.78f;
        int scaledTextWidth = Math.round(font.width(statusText) * scale);
        int textX = badgeX + Math.max(3, (BADGE_WIDTH - scaledTextWidth) / 2);
        int textY = badgeY + 3;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(textX, textY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.drawString(font, statusText, 0, 0, textColor, false);
        guiGraphics.pose().popPose();

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

    private static void drawArrow(GuiGraphics g, int x, int y, int color) {
        // Shaft (16x7)
        g.fill(x, y + 5, x + 16, y + 12, color);
        // Arrowhead (8px wide, tapering from 17px tall to 1px tip)
        for (int dx = 0; dx < 8; dx++) {
            int halfHeight = 8 - dx;
            g.fill(x + 16 + dx, y + 8 - halfHeight, x + 17 + dx, y + 9 + halfHeight, color);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(ENERGY_BAR_X - 1, ENERGY_BAR_Y - 1, ENERGY_BAR_WIDTH + 2, ENERGY_BAR_HEIGHT + 2, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "tooltip.enrichment_solution.energy",
                            String.format("%,d", menu.getEnergy()),
                            String.format("%,d", menu.getMaxEnergy())
                    ),
                    mouseX,
                    mouseY
            );
        }
    }
}
