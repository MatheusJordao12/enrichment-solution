package com.enrichmentsolution.mod.client;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.client.renderer.SolarPanelRenderer;
import com.enrichmentsolution.mod.client.screen.AnimationSolutionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Client-side game-bus event handlers for the Animation Solution settings button and frame counter reset.
 */
@EventBusSubscriber(modid = EnrichmentSolutionMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientForgeEvents {

    @SubscribeEvent
    public static void onOptionsScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof OptionsScreen optionsScreen) {
            int btnWidth = 118;
            int btnHeight = 20;
            int x = optionsScreen.width - btnWidth - 8;
            int y = 6;
            event.addListener(Button.builder(
                            Component.translatable("button.enrichment_solution.animation_solution"),
                            b -> Minecraft.getInstance().setScreen(new AnimationSolutionScreen(optionsScreen))
                    )
                    .bounds(x, y, btnWidth, btnHeight)
                    .build());
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            SolarPanelRenderer.resetFrameCounter();
        }
    }

    private ClientForgeEvents() {}
}
