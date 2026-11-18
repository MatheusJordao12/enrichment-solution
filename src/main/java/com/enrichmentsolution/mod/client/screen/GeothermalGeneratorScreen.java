package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.block.entity.GeothermalGeneratorBlockEntity;
import com.enrichmentsolution.mod.common.menu.GeothermalGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GeothermalGeneratorScreen extends AbstractContainerScreen<GeothermalGeneratorMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID,
            "textures/gui/geothermal_generator_gui.png"
    );

    private static final int INPUT_TANK_X = 26;
    private static final int OUTPUT_TANK_X = 112;
    private static final int TANK_Y = 18;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 50;

    private static final int ENERGY_BAR_X = 148;
    private static final int ENERGY_BAR_Y = 18;
    private static final int ENERGY_BAR_WIDTH = 14;
    private static final int ENERGY_BAR_HEIGHT = 48;

    private static final int ARROW_X = 68;
    private static final int ARROW_Y = 35;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;

    private final SideConfigOverlayWidget sideConfigWidget = new SideConfigOverlayWidget();

    public GeothermalGeneratorScreen(GeothermalGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        String feText = menu.getGeneration() + " FE/t";
        String flowText = menu.getFlowRate() + " mB/t";
        guiGraphics.drawString(font, feText, 54, 21, 0x15803D, false);
        guiGraphics.drawString(font, flowText, 54, 57, 0x0369A1, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Input Gaseous Isobutane tank (pressurized vapor)
        renderFluidBar(guiGraphics, x + INPUT_TANK_X, y + TANK_Y, menu.getScaledInputGas(TANK_HEIGHT), 0xFF67E8F9, 0xFFA5F3FC);

        // Output Gaseous Isobutane tank (expanded exhaust vapor)
        renderFluidBar(guiGraphics, x + OUTPUT_TANK_X, y + TANK_Y, menu.getScaledOutputGas(TANK_HEIGHT), 0xFF94A3B8, 0xFFCBD5E1);

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

        // Turbine flow arrow
        if (menu.isGenerating()) {
            int arrowWidth = Math.max(1, menu.getFlowRate() * ARROW_WIDTH / GeothermalGeneratorBlockEntity.MAX_PROCESS_MB_PER_TICK);
            arrowWidth = Math.min(arrowWidth, ARROW_WIDTH);
            guiGraphics.blit(
                    TEXTURE,
                    x + ARROW_X,
                    y + ARROW_Y,
                    176,
                    48,
                    arrowWidth,
                    ARROW_HEIGHT
            );
        }
    }

    private static void renderFluidBar(GuiGraphics guiGraphics, int x, int y, int filledHeight, int mainColor, int highlightColor) {
        if (filledHeight <= 0) {
            return;
        }
        int top = y + (TANK_HEIGHT - filledHeight);
        guiGraphics.fill(x + 1, top, x + TANK_WIDTH - 1, y + TANK_HEIGHT, mainColor);
        guiGraphics.fill(x + 2, top, x + TANK_WIDTH - 4, y + TANK_HEIGHT, highlightColor);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        sideConfigWidget.render(guiGraphics, leftPos, topPos, mouseX, mouseY, menu, font);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(INPUT_TANK_X - 1, TANK_Y - 1, TANK_WIDTH + 2, TANK_HEIGHT + 2, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "tooltip.enrichment_solution.geothermal_generator.input_gas",
                            menu.getInputGas(),
                            menu.getMaxTank()
                    ),
                    mouseX,
                    mouseY
            );
        } else if (isHovering(OUTPUT_TANK_X - 1, TANK_Y - 1, TANK_WIDTH + 2, TANK_HEIGHT + 2, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "tooltip.enrichment_solution.geothermal_generator.output_gas",
                            menu.getOutputGas(),
                            menu.getMaxTank()
                    ),
                    mouseX,
                    mouseY
            );
        } else if (isHovering(ENERGY_BAR_X - 1, ENERGY_BAR_Y - 1, ENERGY_BAR_WIDTH + 2, ENERGY_BAR_HEIGHT + 2, mouseX, mouseY)) {
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

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        sideConfigWidget.renderTooltip(guiGraphics, leftPos, topPos, mouseX, mouseY, menu, font);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sideConfigWidget.mouseClicked(mouseX, mouseY, button, leftPos, topPos, menu.containerId, GeothermalGeneratorMenu.BUTTON_SIDE_BASE, minecraft)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
