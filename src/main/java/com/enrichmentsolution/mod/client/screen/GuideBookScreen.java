package com.enrichmentsolution.mod.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game Guide Book screen with chapter navigation and multi-page machine & recipe documentation.
 */
public class GuideBookScreen extends Screen {

    private static final int SIDEBAR_WIDTH = 116;
    private static final int HEADER_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 28;

    private static final int COL_BG = 0xFF141820;
    private static final int COL_BORDER = 0xFF3A4A5E;
    private static final int COL_ACCENT = 0xFF00D4AA;
    private static final int COL_CONTENT_BG = 0xFF1B212C;
    private static final int COL_CONTENT_BORDER = 0xFF2B3647;
    private static final int COL_HEADING = 0xFF55FFFF;
    private static final int COL_BODY = 0xFFE2E8F0;
    private static final int COL_RECIPE = 0xFFFFD166;
    private static final int COL_MUTED = 0xFF94A3B8;

    private final List<Chapter> chapters = buildChapters();
    private final List<Button> chapterButtons = new ArrayList<>();

    private int currentChapter = 0;
    private int currentPage = 0;

    private Button prevButton;
    private Button nextButton;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public GuideBookScreen() {
        super(Component.translatable("screen.enrichment_solution.guide_book.title"));
    }

    @Override
    protected void init() {
        chapterButtons.clear();

        panelWidth = Math.min(390, this.width - 20);
        panelHeight = Math.min(248, this.height - 20);
        panelLeft = (this.width - panelWidth) / 2;
        panelTop = (this.height - panelHeight) / 2;

        int btnX = panelLeft + 8;
        int btnY = panelTop + HEADER_HEIGHT + 6;
        int btnW = SIDEBAR_WIDTH - 12;
        int btnH = 20;
        int gap = 4;

        for (int i = 0; i < chapters.size(); i++) {
            final int chapterIndex = i;
            Chapter chapter = chapters.get(i);
            Button btn = Button.builder(Component.translatable(chapter.titleKey), b -> selectChapter(chapterIndex))
                    .bounds(btnX, btnY + i * (btnH + gap), btnW, btnH)
                    .build();
            chapterButtons.add(btn);
            addRenderableWidget(btn);
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(btnX, panelTop + panelHeight - FOOTER_HEIGHT + 2, btnW, 20)
                .build());

        int contentLeft = panelLeft + SIDEBAR_WIDTH + 6;
        int contentRight = panelLeft + panelWidth - 8;
        int navY = panelTop + panelHeight - FOOTER_HEIGHT + 2;

        prevButton = Button.builder(Component.translatable("screen.enrichment_solution.guide_book.prev"), b -> previousPage())
                .bounds(contentLeft, navY, 58, 20)
                .build();
        nextButton = Button.builder(Component.translatable("screen.enrichment_solution.guide_book.next"), b -> nextPage())
                .bounds(contentRight - 58, navY, 58, 20)
                .build();

        addRenderableWidget(prevButton);
        addRenderableWidget(nextButton);

        refreshNavigationState();
    }

    private void selectChapter(int index) {
        if (index >= 0 && index < chapters.size()) {
            currentChapter = index;
            currentPage = 0;
            refreshNavigationState();
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
        } else if (currentChapter > 0) {
            currentChapter--;
            currentPage = chapters.get(currentChapter).pages.size() - 1;
        }
        refreshNavigationState();
    }

    private void nextPage() {
        Chapter chapter = chapters.get(currentChapter);
        if (currentPage + 1 < chapter.pages.size()) {
            currentPage++;
        } else if (currentChapter + 1 < chapters.size()) {
            currentChapter++;
            currentPage = 0;
        }
        refreshNavigationState();
    }

    private void refreshNavigationState() {
        for (int i = 0; i < chapterButtons.size(); i++) {
            Button btn = chapterButtons.get(i);
            Chapter chapter = chapters.get(i);
            boolean selected = (i == currentChapter);
            btn.active = !selected;
            Component label = Component.translatable(chapter.titleKey);
            btn.setMessage(selected ? Component.literal("> ").append(label) : label);
        }

        if (prevButton != null) {
            prevButton.active = !(currentChapter == 0 && currentPage == 0);
        }
        if (nextButton != null) {
            Chapter last = chapters.get(chapters.size() - 1);
            nextButton.active = !(currentChapter == chapters.size() - 1 && currentPage == last.pages.size() - 1);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            previousPage();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            nextPage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            previousPage();
            return true;
        }
        if (scrollY < 0) {
            nextPage();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Outer book panel
        guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, COL_BG);
        guiGraphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, COL_BORDER);
        guiGraphics.fill(panelLeft + 1, panelTop + 1, panelLeft + panelWidth - 1, panelTop + 3, COL_ACCENT);

        // Header divider
        guiGraphics.fill(panelLeft + 6, panelTop + HEADER_HEIGHT, panelLeft + panelWidth - 6, panelTop + HEADER_HEIGHT + 1, COL_BORDER);

        // Content box
        int contentLeft = panelLeft + SIDEBAR_WIDTH + 6;
        int contentTop = panelTop + HEADER_HEIGHT + 6;
        int contentW = panelWidth - SIDEBAR_WIDTH - 14;
        int contentH = panelHeight - HEADER_HEIGHT - FOOTER_HEIGHT - 10;
        guiGraphics.fill(contentLeft, contentTop, contentLeft + contentW, contentTop + contentH, COL_CONTENT_BG);
        guiGraphics.renderOutline(contentLeft, contentTop, contentW, contentH, COL_CONTENT_BORDER);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Title & chapter/page indicator
        guiGraphics.drawString(this.font, this.title, panelLeft + 10, panelTop + 9, 0xFFFFFF, false);

        Chapter chapter = chapters.get(currentChapter);
        Component pageIndicator = Component.translatable(
                "screen.enrichment_solution.guide_book.page",
                currentPage + 1,
                chapter.pages.size()
        );
        int indWidth = this.font.width(pageIndicator);
        int navCenterX = contentLeft + (contentW - indWidth) / 2;
        int navTextY = panelTop + panelHeight - FOOTER_HEIGHT + 8;
        guiGraphics.drawString(this.font, pageIndicator, navCenterX, navTextY, COL_MUTED, false);

        Component chapterHeader = Component.translatable(chapter.titleKey);
        int headerW = this.font.width(chapterHeader);
        guiGraphics.drawString(this.font, chapterHeader, panelLeft + panelWidth - headerW - 10, panelTop + 9, COL_ACCENT, false);

        // Render page lines
        Page page = chapter.pages.get(currentPage);
        int textX = contentLeft + 8;
        int textY = contentTop + 8;
        int maxTextWidth = contentW - 16;
        int maxY = contentTop + contentH - 8;

        for (PageEntry entry : page.entries) {
            if (textY >= maxY) {
                break;
            }
            if (entry.kind == EntryKind.SPACER) {
                textY += 5;
                continue;
            }
            int color = switch (entry.kind) {
                case HEADING -> COL_HEADING;
                case RECIPE -> COL_RECIPE;
                case BODY -> COL_BODY;
                case SPACER -> COL_BODY;
            };
            int indent = entry.kind == EntryKind.RECIPE ? 6 : 0;
            List<FormattedCharSequence> wrapped = this.font.split(Component.translatable(entry.translationKey), maxTextWidth - indent);
            for (FormattedCharSequence line : wrapped) {
                if (textY + this.font.lineHeight > maxY) {
                    break;
                }
                guiGraphics.drawString(this.font, line, textX + indent, textY, color, false);
                textY += this.font.lineHeight + 1;
            }
            if (entry.kind == EntryKind.HEADING) {
                textY += 2;
            } else {
                textY += 3;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum EntryKind {
        HEADING,
        BODY,
        RECIPE,
        SPACER
    }

    private record PageEntry(EntryKind kind, String translationKey) {
        static PageEntry heading(String key) {
            return new PageEntry(EntryKind.HEADING, key);
        }

        static PageEntry body(String key) {
            return new PageEntry(EntryKind.BODY, key);
        }

        static PageEntry recipe(String key) {
            return new PageEntry(EntryKind.RECIPE, key);
        }

        static PageEntry spacer() {
            return new PageEntry(EntryKind.SPACER, "");
        }
    }

    private record Page(List<PageEntry> entries) {}

    private record Chapter(String titleKey, List<Page> pages) {}

    private static List<Chapter> buildChapters() {
        return List.of(
                new Chapter("guide.enrichment_solution.chapter.overview", List.of(
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.overview.p1.h1"),
                                PageEntry.body("guide.enrichment_solution.overview.p1.b1"),
                                PageEntry.spacer(),
                                PageEntry.heading("guide.enrichment_solution.overview.p1.h2"),
                                PageEntry.recipe("guide.enrichment_solution.overview.p1.r1"),
                                PageEntry.recipe("guide.enrichment_solution.overview.p1.r2")
                        )),
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.overview.p2.h1"),
                                PageEntry.body("guide.enrichment_solution.overview.p2.b1"),
                                PageEntry.body("guide.enrichment_solution.overview.p2.b2"),
                                PageEntry.spacer(),
                                PageEntry.heading("guide.enrichment_solution.overview.p2.h2"),
                                PageEntry.recipe("guide.enrichment_solution.overview.p2.r1"),
                                PageEntry.recipe("guide.enrichment_solution.overview.p2.r2")
                        ))
                )),
                new Chapter("guide.enrichment_solution.chapter.energy", List.of(
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.energy.p1.h1"),
                                PageEntry.body("guide.enrichment_solution.energy.p1.b1"),
                                PageEntry.spacer(),
                                PageEntry.heading("guide.enrichment_solution.energy.p1.h2"),
                                PageEntry.recipe("guide.enrichment_solution.energy.p1.r1"),
                                PageEntry.recipe("guide.enrichment_solution.energy.p1.r2")
                        )),
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.energy.p2.h1"),
                                PageEntry.body("guide.enrichment_solution.energy.p2.b1"),
                                PageEntry.recipe("guide.enrichment_solution.energy.p2.r1"),
                                PageEntry.recipe("guide.enrichment_solution.energy.p2.r2"),
                                PageEntry.recipe("guide.enrichment_solution.energy.p2.r3"),
                                PageEntry.recipe("guide.enrichment_solution.energy.p2.r4")
                        )),
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.energy.p3.h1"),
                                PageEntry.body("guide.enrichment_solution.energy.p3.b1"),
                                PageEntry.recipe("guide.enrichment_solution.energy.p3.r1"),
                                PageEntry.recipe("guide.enrichment_solution.energy.p3.r2"),
                                PageEntry.recipe("guide.enrichment_solution.energy.p3.r3"),
                                PageEntry.recipe("guide.enrichment_solution.energy.p3.r4")
                        ))
                )),
                new Chapter("guide.enrichment_solution.chapter.enrichment", List.of(
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.enrichment.p1.h1"),
                                PageEntry.body("guide.enrichment_solution.enrichment.p1.b1"),
                                PageEntry.spacer(),
                                PageEntry.recipe("guide.enrichment_solution.enrichment.p1.r1"),
                                PageEntry.recipe("guide.enrichment_solution.enrichment.p1.r2")
                        )),
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.enrichment.p2.h1"),
                                PageEntry.body("guide.enrichment_solution.enrichment.p2.b1"),
                                PageEntry.body("guide.enrichment_solution.enrichment.p2.b2")
                        ))
                )),
                new Chapter("guide.enrichment_solution.chapter.circuits", List.of(
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.circuits.p1.h1"),
                                PageEntry.body("guide.enrichment_solution.circuits.p1.b1"),
                                PageEntry.recipe("guide.enrichment_solution.circuits.p1.r1"),
                                PageEntry.recipe("guide.enrichment_solution.circuits.p1.r2"),
                                PageEntry.recipe("guide.enrichment_solution.circuits.p1.r3"),
                                PageEntry.recipe("guide.enrichment_solution.circuits.p1.r4"),
                                PageEntry.recipe("guide.enrichment_solution.circuits.p1.r5"),
                                PageEntry.recipe("guide.enrichment_solution.circuits.p1.r6")
                        )),
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.circuits.p2.h1"),
                                PageEntry.body("guide.enrichment_solution.circuits.p2.b1"),
                                PageEntry.spacer(),
                                PageEntry.recipe("guide.enrichment_solution.circuits.p2.r1"),
                                PageEntry.recipe("guide.enrichment_solution.circuits.p2.r2"),
                                PageEntry.recipe("guide.enrichment_solution.circuits.p2.r3")
                        ))
                )),
                new Chapter("guide.enrichment_solution.chapter.fluids", List.of(
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.fluids.p1.h1"),
                                PageEntry.body("guide.enrichment_solution.fluids.p1.b1"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p1.r1"),
                                PageEntry.body("guide.enrichment_solution.fluids.p1.b2"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p1.r2")
                        )),
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.fluids.p2.h1"),
                                PageEntry.body("guide.enrichment_solution.fluids.p2.b1"),
                                PageEntry.body("guide.enrichment_solution.fluids.p2.b2"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p2.r1"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p2.r2"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p2.r3")
                        )),
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.fluids.p3.h1"),
                                PageEntry.body("guide.enrichment_solution.fluids.p3.b1"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p3.r1"),
                                PageEntry.body("guide.enrichment_solution.fluids.p3.b2"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p3.r2"),
                                PageEntry.body("guide.enrichment_solution.fluids.p3.b3"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p3.r3")
                        )),
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.fluids.p4.h1"),
                                PageEntry.body("guide.enrichment_solution.fluids.p4.b1"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p4.r1"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p4.r2"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p4.r3"),
                                PageEntry.recipe("guide.enrichment_solution.fluids.p4.r4")
                        ))
                )),
                new Chapter("guide.enrichment_solution.chapter.robot", List.of(
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.robot.p1.h1"),
                                PageEntry.body("guide.enrichment_solution.robot.p1.b1"),
                                PageEntry.spacer(),
                                PageEntry.recipe("guide.enrichment_solution.robot.p1.r1"),
                                PageEntry.recipe("guide.enrichment_solution.robot.p1.r2"),
                                PageEntry.recipe("guide.enrichment_solution.robot.p1.r3")
                        )),
                        new Page(List.of(
                                PageEntry.heading("guide.enrichment_solution.robot.p2.h1"),
                                PageEntry.body("guide.enrichment_solution.robot.p2.b1"),
                                PageEntry.recipe("guide.enrichment_solution.robot.p2.r1"),
                                PageEntry.spacer(),
                                PageEntry.body("guide.enrichment_solution.robot.p2.b2"),
                                PageEntry.recipe("guide.enrichment_solution.robot.p2.r2")
                        ))
                ))
        );
    }
}
