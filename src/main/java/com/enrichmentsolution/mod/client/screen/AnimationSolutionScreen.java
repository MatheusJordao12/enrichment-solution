package com.enrichmentsolution.mod.client.screen;

import com.enrichmentsolution.mod.client.config.ModClientConfig;
import com.enrichmentsolution.mod.client.config.SolarAnimationLimit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Settings screen for configuring how many solar panels animate at the same time.
 */
public class AnimationSolutionScreen extends Screen {

    private final Screen parent;
    private final Map<SolarAnimationLimit, Button> optionButtons = new EnumMap<>(SolarAnimationLimit.class);

    public AnimationSolutionScreen(Screen parent) {
        super(Component.translatable("screen.enrichment_solution.animation_solution.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        optionButtons.clear();

        SolarAnimationLimit[] values = SolarAnimationLimit.values();
        int cols = 5;
        int btnWidth = 64;
        int btnHeight = 20;
        int gapX = 6;
        int gapY = 8;

        int totalWidth = cols * btnWidth + (cols - 1) * gapX;
        int startX = (this.width - totalWidth) / 2;
        int startY = this.height / 2 - 28;

        for (int i = 0; i < values.length; i++) {
            SolarAnimationLimit limit = values[i];
            int row = i / cols;
            int col = i % cols;
            int x = startX + col * (btnWidth + gapX);
            int y = startY + row * (btnHeight + gapY);

            Button btn = Button.builder(formatLabel(limit, limit == ModClientConfig.getLimit()), b -> {
                        ModClientConfig.setLimit(limit);
                        refreshButtons();
                    })
                    .bounds(x, y, btnWidth, btnHeight)
                    .build();

            optionButtons.put(limit, btn);
            addRenderableWidget(btn);
        }

        refreshButtons();

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, startY + 2 * (btnHeight + gapY) + 18, 200, 20)
                .build());
    }

    private void refreshButtons() {
        SolarAnimationLimit selected = ModClientConfig.getLimit();
        for (Map.Entry<SolarAnimationLimit, Button> entry : optionButtons.entrySet()) {
            SolarAnimationLimit limit = entry.getKey();
            Button btn = entry.getValue();
            boolean isSelected = limit == selected;
            btn.setMessage(formatLabel(limit, isSelected));
            btn.active = !isSelected;
        }
    }

    private Component formatLabel(SolarAnimationLimit limit, boolean selected) {
        Component base = limit.getDisplayName();
        if (selected) {
            return Component.literal("[")
                    .append(base)
                    .append("]")
                    .withStyle(ChatFormatting.GREEN);
        }
        return base;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 72, 0xFFFFFF);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.enrichment_solution.animation_solution.subtitle"),
                this.width / 2,
                this.height / 2 - 54,
                0xA0A0A0
        );
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
