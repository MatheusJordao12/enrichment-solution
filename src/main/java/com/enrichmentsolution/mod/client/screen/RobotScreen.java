package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.menu.RobotMenu;
import com.enrichmentsolution.mod.common.network.RobotConfigPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.neoforged.neoforge.network.PacketDistributor;

public class RobotScreen extends AbstractContainerScreen<RobotMenu> {

    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/gui/robot_gui.png"
    );
    private static final ResourceLocation FOLLOW_WALKING_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/gui/robot_follow_walking.png"
    );
    private static final ResourceLocation FOLLOW_SITTING_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/gui/robot_follow_sitting.png"
    );
    private static final ResourceLocation OPEN_CHEST_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/gui/robot_open_chest.png"
    );

    // Follow button (main tab)
    private static final int FOLLOW_BTN_X = 27;
    private static final int FOLLOW_BTN_Y = 54;
    private static final int FOLLOW_BTN_W = 16;
    private static final int FOLLOW_BTN_H = 16;

    // 54x54 Open Expanded Storage button (main tab when tier >= 1)
    private static final int STORAGE_BTN_X = 61;
    private static final int STORAGE_BTN_Y = 16;
    private static final int STORAGE_BTN_SIZE = 54;

    // Side tab buttons (attached to right edge of main GUI)
    private static final int SIDE_BTN_W = 22;
    private static final int SIDE_BTN_H = 22;
    private static final int BATTERY_BTN_Y = 6;
    private static final int TOOL_CFG_BTN_Y = 30;

    // Side panels
    private static final int PANEL_OFFSET_X = 22;
    private static final int PANEL_Y = 6;
    private static final int BATTERY_PANEL_W = 72;
    private static final int BATTERY_PANEL_H = 76;
    private static final int TOOL_PANEL_W = 88;
    private static final int TOOL_PANEL_H = 92;

    // Energy & XP bars inside Battery panel
    private static final int BAR_REL_X = 31;
    private static final int BAR_REL_Y = 11;
    private static final int BAR_W = 14;
    private static final int BAR_H = 52;
    private static final int XP_BAR_REL_X = 51;
    private static final int XP_BAR_REL_Y = 11;
    private static final int XP_BAR_W = 12;
    private static final int XP_BAR_H = 52;

    // Expanded storage controls
    private static final int BACK_BTN_X = 154;
    private static final int BACK_BTN_Y = 4;
    private static final int BACK_BTN_W = 14;
    private static final int BACK_BTN_H = 11;
    private static final int SCROLLBAR_X = 170;
    private static final int SCROLLBAR_Y = 17;
    private static final int SCROLLBAR_W = 4;
    private static final int SCROLLBAR_H = 108;

    private boolean batteryTabOpen = false;
    private boolean toolConfigTabOpen = false;
    private boolean expandedStorageOpen = false;
    private int scrollRow = 0;
    private boolean draggingScrollbar = false;

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
    protected void init() {
        super.init();
        syncLayout();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (toolConfigTabOpen && !isPickaxeEquipped()) {
            toolConfigTabOpen = false;
        }
        if (expandedStorageOpen && menu.getSlotUpgradeTier() == 0) {
            expandedStorageOpen = false;
            scrollRow = 0;
        }
        syncLayout();
    }

    private void syncLayout() {
        int tier = menu.getSlotUpgradeTier();
        if (expandedStorageOpen && tier >= 1) {
            int maxScroll = Math.max(0, (menu.getStorageCapacity() / 9) - RobotMenu.VISIBLE_EXPANDED_ROWS);
            scrollRow = Mth.clamp(scrollRow, 0, maxScroll);
            this.imageHeight = tier >= 2 ? 220 : 166;
            this.inventoryLabelY = tier >= 2 ? 126 : 72;
        } else {
            expandedStorageOpen = false;
            scrollRow = 0;
            this.imageHeight = 166;
            this.inventoryLabelY = 72;
        }

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        menu.updateSlotPositions(expandedStorageOpen, batteryTabOpen, scrollRow, this.imageWidth);
    }

    private boolean isPickaxeEquipped() {
        ItemStack tool = menu.getToolStack();
        return !tool.isEmpty() && (tool.getItem() instanceof PickaxeItem || tool.is(ItemTags.PICKAXES));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        if (expandedStorageOpen) {
            renderExpandedStorageBg(guiGraphics, x, y, mouseX, mouseY);
            return;
        }

        // Main 176x166 high-tech panel
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Tier >= 1: replace 3x3 slot grid at (61, 16) with 54x54 Open Storage button
        if (menu.getSlotUpgradeTier() >= 1) {
            renderOpenStorageButton(guiGraphics, x + STORAGE_BTN_X, y + STORAGE_BTN_Y, mouseX, mouseY);
        }

        // Follow / Stay icon button
        renderFollowButton(guiGraphics, x + FOLLOW_BTN_X, y + FOLLOW_BTN_Y, mouseX, mouseY);

        // Side tab buttons
        renderSideButtons(guiGraphics, x, y, mouseX, mouseY);

        // Active side panel
        if (batteryTabOpen) {
            renderBatteryPanel(guiGraphics, x + imageWidth + PANEL_OFFSET_X, y + PANEL_Y, mouseX, mouseY);
        } else if (toolConfigTabOpen && isPickaxeEquipped()) {
            renderToolConfigPanel(guiGraphics, x + imageWidth + PANEL_OFFSET_X, y + PANEL_Y, mouseX, mouseY);
        }
    }

    private void renderOpenStorageButton(GuiGraphics guiGraphics, int bx, int by, int mouseX, int mouseY) {
        boolean hovered = isInside(mouseX, mouseY, bx, by, STORAGE_BTN_SIZE, STORAGE_BTN_SIZE);
        guiGraphics.fill(bx, by, bx + STORAGE_BTN_SIZE, by + STORAGE_BTN_SIZE, hovered ? 0xFF38BDF8 : 0xFF3F4350);
        guiGraphics.fill(bx + 1, by + 1, bx + STORAGE_BTN_SIZE - 1, by + STORAGE_BTN_SIZE - 1, hovered ? 0xFF252A36 : 0xFF1B1E26);
        guiGraphics.fill(bx + 2, by + 2, bx + STORAGE_BTN_SIZE - 2, by + 3, 0xFF343846);

        guiGraphics.blit(OPEN_CHEST_TEXTURE, bx + 19, by + 12, 0, 0, 16, 16, 16, 16);

        Component label = Component.translatable("gui.enrichment_solution.robot.slots_count", menu.getStorageCapacity());
        int lw = font.width(label);
        guiGraphics.drawString(font, label, bx + (STORAGE_BTN_SIZE - lw) / 2, by + 35, 0xFFE4E4E7, false);
    }

    private void renderFollowButton(GuiGraphics guiGraphics, int bx, int by, int mouseX, int mouseY) {
        boolean follow = menu.isFollowOwner();
        boolean hovered = isInside(mouseX, mouseY, bx, by, FOLLOW_BTN_W, FOLLOW_BTN_H);
        int border = hovered ? 0xFFFFFFFF : (follow ? 0xFF38BDF8 : 0xFF52525B);
        int bg = follow ? 0xFF1E3A5F : 0xFF27272E;
        guiGraphics.fill(bx, by, bx + FOLLOW_BTN_W, by + FOLLOW_BTN_H, border);
        guiGraphics.fill(bx + 1, by + 1, bx + FOLLOW_BTN_W - 1, by + FOLLOW_BTN_H - 1, bg);

        ResourceLocation icon = follow ? FOLLOW_WALKING_TEXTURE : FOLLOW_SITTING_TEXTURE;
        guiGraphics.blit(icon, bx + 1, by + 1, FOLLOW_BTN_W - 2, FOLLOW_BTN_H - 2, 0, 0, 16, 16, 16, 16);
    }

    private void renderSideButtons(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        int btnX = x + imageWidth;

        // Battery tab button
        int batY = y + BATTERY_BTN_Y;
        boolean batHovered = isInside(mouseX, mouseY, btnX, batY, SIDE_BTN_W, SIDE_BTN_H);
        guiGraphics.blit(GUI_TEXTURE, btnX, batY, (batteryTabOpen || batHovered) ? 200 : 224, 170, SIDE_BTN_W, SIDE_BTN_H);
        // Mini battery icon inside tab
        guiGraphics.fill(btnX + 7, batY + 5, btnX + 15, batY + 6, 0xFF94A3B8);
        guiGraphics.fill(btnX + 6, batY + 6, btnX + 16, batY + 17, 0xFF0F172A);
        guiGraphics.fill(btnX + 7, batY + 7, btnX + 15, batY + 16, 0xFF22C55E);

        // Tool config tab button
        int toolY = y + TOOL_CFG_BTN_Y;
        ItemStack toolStack = menu.getToolStack();
        boolean hasTool = !toolStack.isEmpty();
        boolean toolHovered = isInside(mouseX, mouseY, btnX, toolY, SIDE_BTN_W, SIDE_BTN_H);

        if (!hasTool) {
            // Dark/disabled appearance (0xFF3A3A3F)
            guiGraphics.fill(btnX, toolY, btnX + SIDE_BTN_W, toolY + SIDE_BTN_H, 0xFF27272A);
            guiGraphics.fill(btnX + 1, toolY + 1, btnX + SIDE_BTN_W - 1, toolY + SIDE_BTN_H - 1, 0xFF3A3A3F);
        } else {
            guiGraphics.blit(GUI_TEXTURE, btnX, toolY, (toolConfigTabOpen || toolHovered) ? 200 : 224, 170, SIDE_BTN_W, SIDE_BTN_H);
            guiGraphics.renderItem(toolStack, btnX + 3, toolY + 3);
        }
    }

    private void renderBatteryPanel(GuiGraphics guiGraphics, int px, int py, int mouseX, int mouseY) {
        drawBeveledPanel(guiGraphics, px, py, BATTERY_PANEL_W, BATTERY_PANEL_H);

        // 3 energy cell slot frames
        for (int row = 0; row < 3; row++) {
            guiGraphics.blit(GUI_TEXTURE, px + 7, py + 10 + row * 18, 180, 170, 18, 18);
        }

        // Vertical energy bar
        int bx = px + BAR_REL_X;
        int by = py + BAR_REL_Y;
        guiGraphics.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, 0xFF373737);
        guiGraphics.fill(bx, by, bx + BAR_W, by + BAR_H, 0xFF1E293B);

        int energy = menu.getEnergyStored();
        int maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy > 0 && energy > 0) {
            int filled = Math.min(BAR_H, (int) Math.ceil((double) energy / maxEnergy * BAR_H));
            int top = by + (BAR_H - filled);
            guiGraphics.fill(bx + 1, top, bx + BAR_W - 1, by + BAR_H, 0xFF16A34A);
            guiGraphics.fill(bx + 2, top, bx + BAR_W - 4, by + BAR_H, 0xFF4ADE80);
        }

        // Vertical XP Tank bar
        int xbx = px + XP_BAR_REL_X;
        int xby = py + XP_BAR_REL_Y;
        guiGraphics.fill(xbx - 1, xby - 1, xbx + XP_BAR_W + 1, xby + XP_BAR_H + 1, 0xFF373737);
        guiGraphics.fill(xbx, xby, xbx + XP_BAR_W, xby + XP_BAR_H, 0xFF1E293B);

        int xp = menu.getStoredXp();
        int maxXp = menu.getMaxStoredXp();
        if (maxXp > 0 && xp > 0) {
            int filled = Math.min(XP_BAR_H, (int) Math.ceil((double) xp / maxXp * XP_BAR_H));
            int top = xby + (XP_BAR_H - filled);
            guiGraphics.fill(xbx + 1, top, xbx + XP_BAR_W - 1, xby + XP_BAR_H, 0xFF59C710);
            guiGraphics.fill(xbx + 2, top, xbx + XP_BAR_W - 3, xby + XP_BAR_H, 0xFF80FF20);
        }
    }

    private void renderToolConfigPanel(GuiGraphics guiGraphics, int px, int py, int mouseX, int mouseY) {
        drawBeveledPanel(guiGraphics, px, py, TOOL_PANEL_W, TOOL_PANEL_H);

        guiGraphics.drawString(font, Component.translatable("gui.enrichment_solution.robot.tool_config_short"),
                px + 6, py + 5, 0xFFE4E4E7, false);

        // Target Y
        guiGraphics.drawString(font,
                Component.translatable("gui.enrichment_solution.robot.tool_config.target_y", menu.getMiningTargetY()),
                px + 6, py + 17, 0xFF94A3B8, false);
        String[] yLabels = {"-10", "-1", "+1", "+10"};
        for (int i = 0; i < 4; i++) {
            int bx = px + 5 + i * 20;
            int by = py + 28;
            drawSmallButton(guiGraphics, bx, by, 18, 12, yLabels[i], isInside(mouseX, mouseY, bx, by, 18, 12), false);
        }

        // Chunk Radius
        guiGraphics.drawString(font,
                Component.translatable("gui.enrichment_solution.robot.tool_config.chunk_radius", menu.getChunkRadius()),
                px + 6, py + 44, 0xFF94A3B8, false);
        drawSmallButton(guiGraphics, px + 5, py + 55, 37, 12, "-", isInside(mouseX, mouseY, px + 5, py + 55, 37, 12), false);
        drawSmallButton(guiGraphics, px + 46, py + 55, 37, 12, "+", isInside(mouseX, mouseY, px + 46, py + 55, 37, 12), false);

        // Dig Chunks toggle at bottom
        int digX = px + 5;
        int digY = py + 72;
        boolean digOn = menu.isDigChunks();
        boolean digHovered = isInside(mouseX, mouseY, digX, digY, 78, 14);
        int border = digHovered ? 0xFFFFFFFF : (digOn ? 0xFF4ADE80 : 0xFF52525B);
        int bg = digOn ? 0xFF15803D : 0xFF27272E;
        guiGraphics.fill(digX, digY, digX + 78, digY + 14, border);
        guiGraphics.fill(digX + 1, digY + 1, digX + 77, digY + 13, bg);
        Component digText = Component.translatable("gui.enrichment_solution.robot.tool_config.dig_chunks");
        int dw = font.width(digText);
        guiGraphics.drawString(font, digText, digX + (78 - dw) / 2, digY + 3, 0xFFFFFFFF, false);
    }

    private void renderExpandedStorageBg(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        int tier = menu.getSlotUpgradeTier();
        int totalRows = menu.getStorageCapacity() / 9;
        int visibleRows = Math.min(RobotMenu.VISIBLE_EXPANDED_ROWS, totalRows);
        int storageAreaH = visibleRows * 18 + 4;

        // Top header from robot_gui.png
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, 17);

        // Middle dark-steel beveled background for expanded slots
        drawBeveledPanel(guiGraphics, x, y + 16, imageWidth, storageAreaH);

        // Bottom player inventory + hotbar from robot_gui.png
        guiGraphics.blit(GUI_TEXTURE, x, y + imageHeight - 92, 0, 74, imageWidth, 92);

        // Storage slot frames
        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < 9; col++) {
                guiGraphics.blit(GUI_TEXTURE, x + 7 + col * 18, y + 16 + row * 18, 180, 170, 18, 18);
            }
        }

        // Back button in header
        int bx = x + BACK_BTN_X;
        int by = y + BACK_BTN_Y;
        boolean backHover = isInside(mouseX, mouseY, bx, by, BACK_BTN_W, BACK_BTN_H);
        drawSmallButton(guiGraphics, bx, by, BACK_BTN_W, BACK_BTN_H, "<", backHover, false);

        // Scrollbar for Tier 3 (9x9 = 81 slots, 6 visible rows)
        if (tier >= 3 && totalRows > RobotMenu.VISIBLE_EXPANDED_ROWS) {
            int sx = x + SCROLLBAR_X;
            int sy = y + SCROLLBAR_Y;
            guiGraphics.fill(sx, sy, sx + SCROLLBAR_W, sy + SCROLLBAR_H, 0xFF0F172A);
            int maxScroll = totalRows - RobotMenu.VISIBLE_EXPANDED_ROWS;
            int thumbH = Math.max(18, (int) ((double) SCROLLBAR_H * RobotMenu.VISIBLE_EXPANDED_ROWS / totalRows));
            int thumbY = sy + (int) ((double) (SCROLLBAR_H - thumbH) * scrollRow / maxScroll);
            guiGraphics.fill(sx, thumbY, sx + SCROLLBAR_W, thumbY + thumbH, 0xFF38BDF8);
        }
    }

    private void drawBeveledPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF101218);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF232630);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, 0xFF3F4454);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, 0xFF3F4454);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF1A1D24);
    }

    private void drawSmallButton(GuiGraphics g, int x, int y, int w, int h, String text, boolean hovered, boolean active) {
        int border = hovered ? 0xFFFFFFFF : (active ? 0xFF38BDF8 : 0xFF52525B);
        int bg = active ? 0xFF1E3A5F : 0xFF2B2F3A;
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        int tw = font.width(text);
        g.drawString(font, text, x + (w - tw) / 2, y + (h - 8) / 2, 0xFFFFFFFF, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE4E4E7, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE4E4E7, false);
        if (!expandedStorageOpen) {
            guiGraphics.drawString(font, Component.translatable("gui.enrichment_solution.robot.tool"), 14, 24, 0xFF94A3B8, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = leftPos;
            int y = topPos;

            if (expandedStorageOpen) {
                if (isInside(mouseX, mouseY, x + BACK_BTN_X, y + BACK_BTN_Y, BACK_BTN_W, BACK_BTN_H)) {
                    expandedStorageOpen = false;
                    syncLayout();
                    return true;
                }
                int totalRows = menu.getStorageCapacity() / 9;
                if (totalRows > RobotMenu.VISIBLE_EXPANDED_ROWS
                        && isInside(mouseX, mouseY, x + SCROLLBAR_X - 2, y + SCROLLBAR_Y, SCROLLBAR_W + 4, SCROLLBAR_H)) {
                    draggingScrollbar = true;
                    updateScrollFromMouse(mouseY);
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            // Follow button
            if (isInside(mouseX, mouseY, x + FOLLOW_BTN_X, y + FOLLOW_BTN_Y, FOLLOW_BTN_W, FOLLOW_BTN_H)
                    && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                return true;
            }

            // 54x54 Open Expanded Storage button (tier >= 1)
            if (menu.getSlotUpgradeTier() >= 1
                    && isInside(mouseX, mouseY, x + STORAGE_BTN_X, y + STORAGE_BTN_Y, STORAGE_BTN_SIZE, STORAGE_BTN_SIZE)) {
                expandedStorageOpen = true;
                batteryTabOpen = false;
                toolConfigTabOpen = false;
                syncLayout();
                return true;
            }

            // Battery side tab button
            int sideX = x + imageWidth;
            if (isInside(mouseX, mouseY, sideX, y + BATTERY_BTN_Y, SIDE_BTN_W, SIDE_BTN_H)) {
                batteryTabOpen = !batteryTabOpen;
                if (batteryTabOpen) {
                    toolConfigTabOpen = false;
                }
                syncLayout();
                return true;
            }

            // Tool config side tab button (only clickable if pickaxe equipped)
            if (isInside(mouseX, mouseY, sideX, y + TOOL_CFG_BTN_Y, SIDE_BTN_W, SIDE_BTN_H)) {
                if (isPickaxeEquipped()) {
                    toolConfigTabOpen = !toolConfigTabOpen;
                    if (toolConfigTabOpen) {
                        batteryTabOpen = false;
                    }
                    syncLayout();
                }
                return true;
            }

            // Tool config panel controls
            if (toolConfigTabOpen && isPickaxeEquipped()) {
                int px = x + imageWidth + PANEL_OFFSET_X;
                int py = y + PANEL_Y;
                int[] deltas = {-10, -1, 1, 10};
                for (int i = 0; i < 4; i++) {
                    if (isInside(mouseX, mouseY, px + 5 + i * 20, py + 28, 18, 12)) {
                        sendConfig(Mth.clamp(menu.getMiningTargetY() + deltas[i], -64, 320), menu.isDigChunks(), menu.getChunkRadius());
                        return true;
                    }
                }
                if (isInside(mouseX, mouseY, px + 5, py + 55, 37, 12)) {
                    sendConfig(menu.getMiningTargetY(), menu.isDigChunks(), Mth.clamp(menu.getChunkRadius() - 1, 0, 4));
                    return true;
                }
                if (isInside(mouseX, mouseY, px + 46, py + 55, 37, 12)) {
                    sendConfig(menu.getMiningTargetY(), menu.isDigChunks(), Mth.clamp(menu.getChunkRadius() + 1, 0, 4));
                    return true;
                }
                if (isInside(mouseX, mouseY, px + 5, py + 72, 78, 14)) {
                    sendConfig(menu.getMiningTargetY(), !menu.isDigChunks(), menu.getChunkRadius());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && expandedStorageOpen) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (expandedStorageOpen) {
            int maxScroll = Math.max(0, (menu.getStorageCapacity() / 9) - RobotMenu.VISIBLE_EXPANDED_ROWS);
            if (maxScroll > 0) {
                int delta = scrollY > 0 ? -1 : 1;
                scrollRow = Mth.clamp(scrollRow + delta, 0, maxScroll);
                syncLayout();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateScrollFromMouse(double mouseY) {
        int maxScroll = Math.max(0, (menu.getStorageCapacity() / 9) - RobotMenu.VISIBLE_EXPANDED_ROWS);
        if (maxScroll <= 0) {
            return;
        }
        double rel = (mouseY - (topPos + SCROLLBAR_Y)) / (double) SCROLLBAR_H;
        scrollRow = Mth.clamp((int) Math.round(rel * maxScroll), 0, maxScroll);
        syncLayout();
    }

    private void sendConfig(int targetY, boolean digChunks, int chunkRadius) {
        int entityId = menu.getRobotEntityId();
        if (entityId >= 0) {
            PacketDistributor.sendToServer(new RobotConfigPayload(entityId, targetY, digChunks, chunkRadius));
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        if (!expandedStorageOpen) {
            int sideX = guiLeft + imageWidth;
            if (isInside(mouseX, mouseY, sideX, guiTop + BATTERY_BTN_Y, SIDE_BTN_W, SIDE_BTN_H)
                    || isInside(mouseX, mouseY, sideX, guiTop + TOOL_CFG_BTN_Y, SIDE_BTN_W, SIDE_BTN_H)) {
                return false;
            }
            if (batteryTabOpen && isInside(mouseX, mouseY, sideX + PANEL_OFFSET_X, guiTop + PANEL_Y, BATTERY_PANEL_W, BATTERY_PANEL_H)) {
                return false;
            }
            if (toolConfigTabOpen && isPickaxeEquipped()
                    && isInside(mouseX, mouseY, sideX + PANEL_OFFSET_X, guiTop + PANEL_Y, TOOL_PANEL_W, TOOL_PANEL_H)) {
                return false;
            }
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderCustomTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        if (expandedStorageOpen) {
            if (isInside(mouseX, mouseY, x + BACK_BTN_X, y + BACK_BTN_Y, BACK_BTN_W, BACK_BTN_H)) {
                guiGraphics.renderTooltip(font, Component.translatable("gui.enrichment_solution.robot.close_storage"), mouseX, mouseY);
            }
            return;
        }

        if (isInside(mouseX, mouseY, x + FOLLOW_BTN_X, y + FOLLOW_BTN_Y, FOLLOW_BTN_W, FOLLOW_BTN_H)) {
            String key = menu.isFollowOwner()
                    ? "gui.enrichment_solution.robot.follow_tooltip"
                    : "gui.enrichment_solution.robot.stay_tooltip";
            guiGraphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
            return;
        }

        if (menu.getSlotUpgradeTier() >= 1
                && isInside(mouseX, mouseY, x + STORAGE_BTN_X, y + STORAGE_BTN_Y, STORAGE_BTN_SIZE, STORAGE_BTN_SIZE)) {
            guiGraphics.renderTooltip(font,
                    Component.translatable("gui.enrichment_solution.robot.open_storage", menu.getStorageCapacity()),
                    mouseX, mouseY);
            return;
        }

        int sideX = x + imageWidth;
        if (isInside(mouseX, mouseY, sideX, y + BATTERY_BTN_Y, SIDE_BTN_W, SIDE_BTN_H)) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.enrichment_solution.robot.battery_tab"), mouseX, mouseY);
            return;
        }

        if (isInside(mouseX, mouseY, sideX, y + TOOL_CFG_BTN_Y, SIDE_BTN_W, SIDE_BTN_H)) {
            ItemStack toolStack = menu.getToolStack();
            if (toolStack.isEmpty()) {
                guiGraphics.renderTooltip(font, Component.translatable("gui.enrichment_solution.robot.tool_config.no_tool"), mouseX, mouseY);
            } else if (isPickaxeEquipped()) {
                guiGraphics.renderTooltip(font, Component.translatable("gui.enrichment_solution.robot.tool_config_tab"), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(font, Component.translatable("gui.enrichment_solution.robot.tool_config.not_pickaxe"), mouseX, mouseY);
            }
            return;
        }

        if (batteryTabOpen) {
            int barX = sideX + PANEL_OFFSET_X + BAR_REL_X;
            int barY = y + PANEL_Y + BAR_REL_Y;
            if (isInside(mouseX, mouseY, barX, barY, BAR_W, BAR_H)) {
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

            int xpBarX = sideX + PANEL_OFFSET_X + XP_BAR_REL_X;
            int xpBarY = y + PANEL_Y + XP_BAR_REL_Y;
            if (isInside(mouseX, mouseY, xpBarX, xpBarY, XP_BAR_W, XP_BAR_H)) {
                guiGraphics.renderTooltip(
                        font,
                        Component.translatable(
                                "tooltip.enrichment_solution.robot.xp_tank",
                                String.format("%,d", menu.getStoredXp()),
                                String.format("%,d", menu.getMaxStoredXp())
                        ),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
