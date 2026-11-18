package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.common.block.RelativeSide;
import com.enrichmentsolution.mod.common.block.SideConfigMode;
import com.enrichmentsolution.mod.common.menu.ISideConfigurableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Self-contained Mekanism-style Side Config tab button and popup panel for machine screens.
 */
public class SideConfigOverlayWidget {

    private static final int TAB_OFFSET_X = -22;
    private static final int TAB_OFFSET_Y = 6;
    private static final int TAB_SIZE = 20;

    private static final int PANEL_OFFSET_X = -68;
    private static final int PANEL_OFFSET_Y = 28;
    private static final int PANEL_WIDTH = 66;
    private static final int PANEL_HEIGHT = 76;

    private static final int BTN_SIZE = 15;

    private record FaceButton(RelativeSide side, int relX, int relY) {}

    private static final List<FaceButton> FACE_BUTTONS = List.of(
            new FaceButton(RelativeSide.TOP, 25, 16),
            new FaceButton(RelativeSide.LEFT, 8, 33),
            new FaceButton(RelativeSide.FRONT, 25, 33),
            new FaceButton(RelativeSide.RIGHT, 42, 33),
            new FaceButton(RelativeSide.BOTTOM, 25, 50),
            new FaceButton(RelativeSide.BACK, 42, 50)
    );

    private boolean expanded = false;

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void render(
            GuiGraphics guiGraphics,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            ISideConfigurableMenu menu,
            Font font
    ) {
        int tabX = leftPos + TAB_OFFSET_X;
        int tabY = topPos + TAB_OFFSET_Y;
        boolean tabHovered = isInside(mouseX, mouseY, tabX, tabY, TAB_SIZE, TAB_SIZE);

        int tabBorder = tabHovered ? 0xFFFFFFFF : (expanded ? 0xFF38BDF8 : 0xFF27272A);
        int tabBg = expanded ? 0xFF1E293B : 0xFF18181B;
        guiGraphics.fill(tabX, tabY, tabX + TAB_SIZE, tabY + TAB_SIZE, tabBorder);
        guiGraphics.fill(tabX + 1, tabY + 1, tabX + TAB_SIZE - 1, tabY + TAB_SIZE - 1, tabBg);

        // Draw a crisp 3x3 gear/grid icon inside the 20x20 tab
        int iconColor = expanded ? 0xFF38BDF8 : 0xFFE4E4E7;
        for (int gx = 0; gx < 3; gx++) {
            for (int gy = 0; gy < 3; gy++) {
                int px = tabX + 4 + gx * 4;
                int py = tabY + 4 + gy * 4;
                int cellColor = (gx == 1 && gy == 1) ? 0xFFEA580C : iconColor;
                guiGraphics.fill(px, py, px + 3, py + 3, cellColor);
            }
        }

        if (!expanded) {
            return;
        }

        int panelX = leftPos + PANEL_OFFSET_X;
        int panelY = topPos + PANEL_OFFSET_Y;

        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF3F3F46);
        guiGraphics.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + PANEL_HEIGHT - 1, 0xFF18181B);
        guiGraphics.drawString(font, "Sides", panelX + 6, panelY + 5, 0xFFE4E4E7, false);

        for (FaceButton faceButton : FACE_BUTTONS) {
            int bx = panelX + faceButton.relX();
            int by = panelY + faceButton.relY();
            SideConfigMode mode = menu.getRelativeSideMode(faceButton.side());
            boolean hovered = isInside(mouseX, mouseY, bx, by, BTN_SIZE, BTN_SIZE);

            guiGraphics.fill(bx, by, bx + BTN_SIZE, by + BTN_SIZE, hovered ? 0xFFFFFFFF : 0xFF27272A);
            guiGraphics.fill(bx + 1, by + 1, bx + BTN_SIZE - 1, by + BTN_SIZE - 1, mode.getColor());

            String letter = faceButton.side().getShortLabel();
            int textWidth = font.width(letter);
            guiGraphics.drawString(font, letter, bx + (BTN_SIZE - textWidth) / 2, by + 4, 0xFFFFFFFF, false);
        }
    }

    public void renderTooltip(
            GuiGraphics guiGraphics,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            ISideConfigurableMenu menu,
            Font font
    ) {
        int tabX = leftPos + TAB_OFFSET_X;
        int tabY = topPos + TAB_OFFSET_Y;
        if (isInside(mouseX, mouseY, tabX, tabY, TAB_SIZE, TAB_SIZE)) {
            guiGraphics.renderTooltip(font, Component.literal("Side Config"), mouseX, mouseY);
            return;
        }

        if (!expanded) {
            return;
        }

        int panelX = leftPos + PANEL_OFFSET_X;
        int panelY = topPos + PANEL_OFFSET_Y;
        for (FaceButton faceButton : FACE_BUTTONS) {
            int bx = panelX + faceButton.relX();
            int by = panelY + faceButton.relY();
            if (isInside(mouseX, mouseY, bx, by, BTN_SIZE, BTN_SIZE)) {
                SideConfigMode mode = menu.getRelativeSideMode(faceButton.side());
                guiGraphics.renderTooltip(
                        font,
                        Component.literal(faceButton.side().getDisplayName() + ": " + mode.name()),
                        mouseX,
                        mouseY
                );
                return;
            }
        }
    }

    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button,
            int leftPos,
            int topPos,
            int containerId,
            int buttonIdBase,
            Minecraft minecraft
    ) {
        if (button != 0) {
            return false;
        }

        int tabX = leftPos + TAB_OFFSET_X;
        int tabY = topPos + TAB_OFFSET_Y;
        if (isInside(mouseX, mouseY, tabX, tabY, TAB_SIZE, TAB_SIZE)) {
            expanded = !expanded;
            return true;
        }

        if (!expanded || minecraft == null || minecraft.gameMode == null) {
            return false;
        }

        int panelX = leftPos + PANEL_OFFSET_X;
        int panelY = topPos + PANEL_OFFSET_Y;
        for (FaceButton faceButton : FACE_BUTTONS) {
            int bx = panelX + faceButton.relX();
            int by = panelY + faceButton.relY();
            if (isInside(mouseX, mouseY, bx, by, BTN_SIZE, BTN_SIZE)) {
                minecraft.gameMode.handleInventoryButtonClick(containerId, buttonIdBase + faceButton.side().ordinal());
                return true;
            }
        }

        return false;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
