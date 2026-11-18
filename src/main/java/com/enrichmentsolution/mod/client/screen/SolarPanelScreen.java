package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.menu.SolarPanelMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SolarPanelScreen extends AbstractContainerScreen<SolarPanelMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/gui/solar_painel.png"
    );

    private static final int ENERGY_BAR_X = 151;
    private static final int ENERGY_BAR_Y = 22;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 48;

    private static final int SCENE_X = 15;
    private static final int SCENE_Y = 23;
    private static final int SCENE_WIDTH = 21;
    private static final int SCENE_SKY_HEIGHT = 32;

    private final SideConfigOverlayWidget sideConfigWidget = new SideConfigOverlayWidget();

    public SolarPanelScreen(SolarPanelMenu menu, Inventory playerInventory, Component title) {
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

        var level = Minecraft.getInstance().level;
        if (level != null) {
            long timeOfDay = level.getDayTime() % 24000L;
            boolean isNight = timeOfDay >= 13000L && timeOfDay < 23000L;
            boolean isRaining = level.isRaining() || level.isThundering();

            if (isNight) {
                // Opaque dark night sky (hides sun and clouds)
                guiGraphics.fill(x + SCENE_X, y + SCENE_Y,
                        x + SCENE_X + SCENE_WIDTH, y + SCENE_Y + SCENE_SKY_HEIGHT,
                        0xFF0B1026);
                // Darken sky strips beside the solar panel
                guiGraphics.fill(x + SCENE_X, y + 55, x + 17, y + 68, 0xFF0B1026);
                guiGraphics.fill(x + 34, y + 55, x + SCENE_X + SCENE_WIDTH, y + 68, 0xFF0B1026);

                // Stars
                guiGraphics.fill(x + 17, y + 25, x + 18, y + 26, 0xFFE6EEFF);
                guiGraphics.fill(x + 32, y + 27, x + 33, y + 28, 0xFFB8C8E8);
                guiGraphics.fill(x + 19, y + 36, x + 20, y + 37, 0xFFE6EEFF);
                guiGraphics.fill(x + 29, y + 38, x + 30, y + 39, 0xFFE6EEFF);
                guiGraphics.fill(x + 24, y + 43, x + 25, y + 44, 0xFFB8C8E8);
                guiGraphics.fill(x + 17, y + 48, x + 18, y + 49, 0xFFE6EEFF);
                guiGraphics.fill(x + 33, y + 50, x + 34, y + 51, 0xFFB8C8E8);

                // Crescent moon
                guiGraphics.fill(x + 22, y + 26, x + 28, y + 32, 0xFFE8EEF8);
                guiGraphics.fill(x + 24, y + 27, x + 28, y + 31, 0xFF0B1026);
            } else if (isRaining) {
                guiGraphics.fill(x + SCENE_X, y + SCENE_Y,
                        x + SCENE_X + SCENE_WIDTH, y + SCENE_Y + SCENE_SKY_HEIGHT,
                        0x66405060);
            }
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
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        int textX = 42;
        int generation = menu.getGeneration();
        guiGraphics.drawString(
                font,
                Component.translatable("gui.enrichment_solution.solar_panel.generation", generation),
                textX, 26, 0xE0E0E0, false
        );

        String timeText = formatMinecraftTime();
        guiGraphics.drawString(
                font,
                Component.translatable("gui.enrichment_solution.solar_panel.time", timeText),
                textX, 39, 0xE0E0E0, false
        );

        int energy = menu.getEnergyStored();
        int maxEnergy = menu.getMaxEnergyStored();
        guiGraphics.drawString(
                font,
                Component.translatable("gui.enrichment_solution.solar_panel.energy", energy, maxEnergy),
                textX, 52, 0x90E090, false
        );
    }

    private String formatMinecraftTime() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return "--:--";
        }
        long ticks = (level.getDayTime() + 6000L) % 24000L;
        int hours = (int) (ticks / 1000L) % 24;
        int minutes = (int) (((ticks % 1000L) * 60L) / 1000L);
        return String.format("%02d:%02d", hours, minutes);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sideConfigWidget.mouseClicked(mouseX, mouseY, button, leftPos, topPos, menu.containerId, SolarPanelMenu.BUTTON_SIDE_BASE, minecraft)) {
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
