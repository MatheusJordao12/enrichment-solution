package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.common.menu.RobotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RobotScreen extends AbstractContainerScreen<RobotMenu> {

    private static final int BTN_X = 12;
    private static final int BTN_Y = 56;
    private static final int BTN_W = 44;
    private static final int BTN_H = 13;

    private static final int BAR_X = 150;
    private static final int BAR_Y = 17;
    private static final int BAR_W = 14;
    private static final int BAR_H = 52;

    public RobotScreen(RobotMenu menu, Inventory playerInventory, Component title) {
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

        // Pickaxe slot (26, 35)
        drawSlot(guiGraphics, x + 25, y + 34);

        // 3x3 Robot storage slots (62, 17)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlot(guiGraphics, x + 61 + col * 18, y + 16 + row * 18);
            }
        }

        // 3 Energy Cell slots (124, 17)
        for (int row = 0; row < 3; row++) {
            drawSlot(guiGraphics, x + 123, y + 16 + row * 18);
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

        // Player inventory (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(guiGraphics, x + 7 + col * 18, y + 83 + row * 18);
            }
        }

        // Player hotbar (1x9)
        for (int col = 0; col < 9; col++) {
            drawSlot(guiGraphics, x + 7 + col * 18, y + 141);
        }

        // Follow / Stay mode toggle button
        boolean follow = menu.isFollowOwner();
        boolean hovered = isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY);
        int bx = x + BTN_X;
        int by = y + BTN_Y;
        guiGraphics.fill(bx, by, bx + BTN_W, by + BTN_H, hovered ? 0xFFFFFFFF : 0xFF373737);
        guiGraphics.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, follow ? 0xFF2563EB : 0xFF52525B);
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
        guiGraphics.drawString(font, Component.translatable("gui.enrichment_solution.robot.tool"), 14, 24, 0x555555, false);

        Component modeText = Component.translatable(
                menu.isFollowOwner() ? "gui.enrichment_solution.robot.follow" : "gui.enrichment_solution.robot.stay"
        );
        int textW = font.width(modeText);
        guiGraphics.drawString(font, modeText, BTN_X + (BTN_W - textW) / 2, BTN_Y + 3, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null
                && isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

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
