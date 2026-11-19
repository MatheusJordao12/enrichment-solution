package com.enrichmentsolution.mod.client;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.client.config.ModClientConfig;
import com.enrichmentsolution.mod.client.screen.AnimationSolutionScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = EnrichmentSolutionMod.MOD_ID, dist = Dist.CLIENT)
public final class ClientEntrypoint {

    public ClientEntrypoint(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC);
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> new AnimationSolutionScreen(parent)
        );
    }
}
