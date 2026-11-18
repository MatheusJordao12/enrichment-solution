package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.common.menu.TankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TankScreen extends AbstractContainerScreen<TankMenu> {

    private static final int FLUID_X = 14;
    private static final int FLUID_Y = 18;
    private static final int FLUID_W = 16;
    private static final int FLUID_H = 50;

    private static final int TEMP_X = 146;
    private static final int TEMP_Y = 18;
    private static final int TEMP_W = 14;
    private static final int TEMP_H = 50;

    public TankScreen(TankMenu menu, Inventory playerInventory, Component title) {
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

        // Water Fluid Bar
        int fx = x + FLUID_X;
        int fy = y + FLUID_Y;
        guiGraphics.fill(fx - 1, fy - 1, fx + FLUID_W + 1, fy + FLUID_H + 1, 0xFF373737);
        guiGraphics.fill(fx, fy, fx + FLUID_W, fy + FLUID_H, 0xFF1E293B);
        int fluid = menu.getFluidAmount();
        int cap = menu.getFluidCapacity();
        if (cap > 0 && fluid > 0) {
            int filled = Math.min(FLUID_H, (int) Math.ceil((double) fluid / cap * FLUID_H));
            int top = fy + (FLUID_H - filled);
            guiGraphics.fill(fx + 1, top, fx + FLUID_W - 1, fy + FLUID_H, 0xFF0284C7);
            guiGraphics.fill(fx + 2, top, fx + FLUID_W - 4, fy + FLUID_H, 0xFF38BDF8);
        }

        // Thermal Bar
        int tx = x + TEMP_X;
        int ty = y + TEMP_Y;
        guiGraphics.fill(tx - 1, ty - 1, tx + TEMP_W + 1, ty + TEMP_H + 1, 0xFF373737);
        guiGraphics.fill(tx, ty, tx + TEMP_W, ty + TEMP_H, 0xFF1E293B);
        double mult = menu.getMultiplier();
        double norm = Math.min(1.0D, mult / 50.0D);
        int tFilled = Math.max(2, (int) Math.ceil(norm * TEMP_H));
        int tTop = ty + (TEMP_H - tFilled);
        int barColor = mult >= 15.0D ? 0xFFEF4444 : (mult >= 4.0D ? 0xFFF97316 : 0xFF38BDF8);
        guiGraphics.fill(tx + 1, tTop, tx + TEMP_W - 1, ty + TEMP_H, barColor);

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

        int infoX = 36;
        int fluid = menu.getFluidAmount();
        int cap = menu.getFluidCapacity();
        guiGraphics.drawString(font, Component.translatable("gui.enrichment_solution.tank.blocks", menu.getBlockCount()), infoX, 20, 0x404040, false);
        guiGraphics.drawString(font, String.format("%,d / %,d B", fluid / 1000, cap / 1000), infoX, 31, 0x0369A1, false);
        guiGraphics.drawString(font, String.format("%.1f °C", menu.getTemperature()), infoX, 42, 0xB91C1C, false);
        guiGraphics.drawString(font, String.format("%.2fx", menu.getMultiplier()), infoX, 53, 0xC2410C, false);

        int flags = menu.getBonusFlags();
        StringBuilder bonuses = new StringBuilder();
        if ((flags & 1) != 0) bonuses.append("Lava ");
        if ((flags & 2) != 0) bonuses.append("Magma ");
        if ((flags & 4) != 0) bonuses.append("Desert");
        if (!bonuses.isEmpty()) {
            guiGraphics.drawString(font, bonuses.toString().trim(), infoX, 63, 0x52525B, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(FLUID_X, FLUID_Y, FLUID_W, FLUID_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "tooltip.enrichment_solution.tank.fluid",
                            String.format("%,d", menu.getFluidAmount()),
                            String.format("%,d", menu.getFluidCapacity())
                    ),
                    mouseX,
                    mouseY
            );
        } else if (isHovering(TEMP_X, TEMP_Y, TEMP_W, TEMP_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "tooltip.enrichment_solution.tank.temp",
                            String.format("%.1f", menu.getTemperature()),
                            String.format("%.2f", menu.getMultiplier())
                    ),
                    mouseX,
                    mouseY
            );
        }
    }
}
