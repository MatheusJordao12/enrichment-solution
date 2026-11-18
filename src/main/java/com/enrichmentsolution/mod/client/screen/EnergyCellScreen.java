package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.menu.EnergyCellMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class EnergyCellScreen extends AbstractContainerScreen<EnergyCellMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/gui/energy_cell.png"
    );

    private static final int ENERGY_BAR_X = 151;
    private static final int ENERGY_BAR_Y = 20;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 50;

    private static final int BTN_SIZE = 15;

    private record FaceButton(Direction direction, String letter, int relX, int relY) {}

    private static final List<FaceButton> FACE_BUTTONS = List.of(
            new FaceButton(Direction.UP, "U", 12, 19),
            new FaceButton(Direction.NORTH, "N", 29, 19),
            new FaceButton(Direction.WEST, "W", 12, 36),
            new FaceButton(Direction.SOUTH, "S", 29, 36),
            new FaceButton(Direction.EAST, "E", 46, 36),
            new FaceButton(Direction.DOWN, "D", 29, 53)
    );

    public EnergyCellScreen(EnergyCellMenu menu, Inventory playerInventory, Component title) {
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

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        for (FaceButton button : FACE_BUTTONS) {
            int bx = x + button.relX();
            int by = y + button.relY();
            SideConfigMode mode = menu.getSideMode(button.direction());
            boolean hovered = isHovering(button.relX(), button.relY(), BTN_SIZE, BTN_SIZE, mouseX, mouseY);

            guiGraphics.fill(bx, by, bx + BTN_SIZE, by + BTN_SIZE, hovered ? 0xFFFFFFFF : 0xFF27272A);
            guiGraphics.fill(bx + 1, by + 1, bx + BTN_SIZE - 1, by + BTN_SIZE - 1, mode.getColor());
            int textWidth = font.width(button.letter());
            guiGraphics.drawString(font, button.letter(), bx + (BTN_SIZE - textWidth) / 2, by + 4, 0xFFFFFFFF, false);
        }

        int energy = menu.getEnergyStored();
        int maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy > 0 && energy > 0) {
            int filled = (int) Math.ceil((double) energy / maxEnergy * ENERGY_BAR_HEIGHT);
            filled = Math.min(filled, ENERGY_BAR_HEIGHT);
            int offset = ENERGY_BAR_HEIGHT - filled;
            guiGraphics.blit(
                    TEXTURE,
                    x + ENERGY_BAR_X,
                    y + ENERGY_BAR_Y + offset,
                    176,
                    offset,
                    ENERGY_BAR_WIDTH,
                    filled
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xE0E0E0, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xE0E0E0, false);

        int energy = menu.getEnergyStored();
        int maxEnergy = menu.getMaxEnergyStored();
        int percent = maxEnergy > 0 ? (int) ((energy * 100L) / maxEnergy) : 0;

        int infoX = 68;
        guiGraphics.drawString(font, Component.translatable("gui.enrichment_solution.energy_cell.stored"), infoX, 22, 0xCBD5E1, false);
        guiGraphics.drawString(font, String.format("%,d FE", energy), infoX, 32, 0x90E090, false);
        guiGraphics.drawString(font, String.format("/ %,d", maxEnergy), infoX, 42, 0x94A3B8, false);
        guiGraphics.drawString(font, percent + "%", infoX, 54, 0xE2E8F0, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            for (FaceButton faceButton : FACE_BUTTONS) {
                if (isHovering(faceButton.relX(), faceButton.relY(), BTN_SIZE, BTN_SIZE, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, faceButton.direction().ordinal());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(ENERGY_BAR_X, ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, mouseX, mouseY)) {
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
            return;
        }

        for (FaceButton faceButton : FACE_BUTTONS) {
            if (isHovering(faceButton.relX(), faceButton.relY(), BTN_SIZE, BTN_SIZE, mouseX, mouseY)) {
                SideConfigMode mode = menu.getSideMode(faceButton.direction());
                Component dirName = Component.translatable("direction.enrichment_solution." + faceButton.direction().getName());
                Component modeName = Component.translatable("gui.enrichment_solution.side_mode." + mode.getKey());
                guiGraphics.renderTooltip(
                        font,
                        Component.translatable("tooltip.enrichment_solution.energy_cell.side", dirName, modeName),
                        mouseX,
                        mouseY
                );
                return;
            }
        }
    }
}
