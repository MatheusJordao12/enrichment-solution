package com.enrichmentsolution.mod.client;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.client.model.RobotModel;
import com.enrichmentsolution.mod.client.render.GogglesLayer;
import com.enrichmentsolution.mod.client.renderer.EnrichmentChamberRenderer;
import com.enrichmentsolution.mod.client.renderer.PcbPrinterRenderer;
import com.enrichmentsolution.mod.client.renderer.RobotRenderer;
import com.enrichmentsolution.mod.client.renderer.SolarPanelRenderer;
import com.enrichmentsolution.mod.client.screen.CondenserScreen;
import com.enrichmentsolution.mod.client.screen.EnergyCellScreen;
import com.enrichmentsolution.mod.client.screen.EnrichmentChamberScreen;
import com.enrichmentsolution.mod.client.screen.FluidPumpScreen;
import com.enrichmentsolution.mod.client.screen.GeothermalGeneratorScreen;
import com.enrichmentsolution.mod.client.screen.HeatExchangerScreen;
import com.enrichmentsolution.mod.client.screen.OilRefineryScreen;
import com.enrichmentsolution.mod.client.screen.PcbPrinterScreen;
import com.enrichmentsolution.mod.client.screen.QuadEnrichmentScreen;
import com.enrichmentsolution.mod.client.screen.RobotScreen;
import com.enrichmentsolution.mod.client.screen.SolarPanelScreen;
import com.enrichmentsolution.mod.client.screen.StampingPressScreen;
import com.enrichmentsolution.mod.client.screen.TankScreen;
import com.enrichmentsolution.mod.common.network.ModNetwork;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModEntities;
import com.enrichmentsolution.mod.registry.ModMenuTypes;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = EnrichmentSolutionMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    /**
     * Wires the client HUD into the common network layer. {@code ModNetwork} never references
     * client classes itself, so a dedicated server never loads {@code InspectHud}.
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModNetwork.setInspectSink(InspectHud::accept);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ENRICHMENT_CHAMBER.get(), EnrichmentChamberScreen::new);
        event.register(ModMenuTypes.QUAD_ENRICHMENT_CHAMBER.get(), QuadEnrichmentScreen::new);
        event.register(ModMenuTypes.SOLAR_PANEL.get(), SolarPanelScreen::new);
        event.register(ModMenuTypes.ROBOT.get(), RobotScreen::new);
        event.register(ModMenuTypes.ENERGY_CELL.get(), EnergyCellScreen::new);
        event.register(ModMenuTypes.PCB_PRINTER.get(), PcbPrinterScreen::new);
        event.register(ModMenuTypes.STAMPING_PRESS.get(), StampingPressScreen::new);
        event.register(ModMenuTypes.TANK.get(), TankScreen::new);
        event.register(ModMenuTypes.OIL_REFINERY.get(), OilRefineryScreen::new);
        event.register(ModMenuTypes.HEAT_EXCHANGER.get(), HeatExchangerScreen::new);
        event.register(ModMenuTypes.GEOTHERMAL_GENERATOR.get(), GeothermalGeneratorScreen::new);
        event.register(ModMenuTypes.CONDENSER.get(), CondenserScreen::new);
        event.register(ModMenuTypes.FLUID_PUMP.get(), FluidPumpScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SOLAR_PANEL.get(), SolarPanelRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ENRICHMENT_CHAMBER.get(), EnrichmentChamberRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PCB_PRINTER.get(), PcbPrinterRenderer::new);
        event.registerEntityRenderer(ModEntities.ROBOT.get(), RobotRenderer::new);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new GogglesLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RobotModel.LAYER_LOCATION, RobotModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SolarPanelRenderer.HEAD_MODEL);
        event.register(SolarPanelRenderer.ADVANCED_LEFT_MODEL);
        event.register(SolarPanelRenderer.ADVANCED_RIGHT_MODEL);
        event.register(PcbPrinterRenderer.PEN_MODEL);
        event.register(PcbPrinterRenderer.PCB_MODEL);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        ClientFluidExtensions.register(event);
        event.registerItem(new GogglesClientExtensions(), ModItems.HI_TECH_GOGGLES.get());
    }

    private ClientSetup() {}
}
