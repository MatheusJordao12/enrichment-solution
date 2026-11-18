package com.enrichmentsolution.mod;

import com.enrichmentsolution.mod.client.config.ModClientConfig;
import com.enrichmentsolution.mod.client.screen.AnimationSolutionScreen;
import com.enrichmentsolution.mod.common.entity.RobotEntity;
import com.enrichmentsolution.mod.common.network.ModNetwork;
import com.enrichmentsolution.mod.common.recipe.MachineRecipes;
import com.enrichmentsolution.mod.registry.ModBlockEntities;
import com.enrichmentsolution.mod.registry.ModBlocks;
import com.enrichmentsolution.mod.registry.ModCreativeTabs;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import com.enrichmentsolution.mod.registry.ModEntities;
import com.enrichmentsolution.mod.registry.ModFluids;
import com.enrichmentsolution.mod.registry.ModItems;
import com.enrichmentsolution.mod.registry.ModMenuTypes;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(EnrichmentSolutionMod.MOD_ID)
public class EnrichmentSolutionMod {

    public static final String MOD_ID = "enrichment_solution";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EnrichmentSolutionMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModFluids.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        MachineRecipes.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC);
            modContainer.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (container, parent) -> new AnimationSolutionScreen(parent)
            );
        }

        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(ModNetwork::register);

        LOGGER.info("Enrichment Solution initialized.");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ENRICHMENT_CHAMBER.get(),
                (be, side) -> be.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ENRICHMENT_CHAMBER.get(),
                (be, side) -> be.getItemHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.REACTOR_CORE.get(),
                (be, side) -> be.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.REACTOR_CORE.get(),
                (be, side) -> be.getItemHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.SOLAR_PANEL.get(),
                (be, side) -> be.getEnergyStorage(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ENERGY_CABLE.get(),
                (be, side) -> be.getEnergyStorage(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ENERGY_CELL.get(),
                (be, side) -> be.getEnergyStorage(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ROBOT_CHARGER.get(),
                (be, side) -> be.getEnergyStorage(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.PCB_PRINTER.get(),
                (be, side) -> be.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.PCB_PRINTER.get(),
                (be, side) -> be.getItemHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.STAMPING_PRESS.get(),
                (be, side) -> be.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.STAMPING_PRESS.get(),
                (be, side) -> be.getItemHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.TANK_PORT.get(),
                (be, side) -> be.getFluidHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.OIL_REFINERY.get(),
                (be, side) -> be.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.OIL_REFINERY.get(),
                (be, side) -> be.getFluidHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.HEAT_EXCHANGER.get(),
                (be, side) -> be.getFluidHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.GEOTHERMAL_GENERATOR.get(),
                (be, side) -> be.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.GEOTHERMAL_GENERATOR.get(),
                (be, side) -> be.getFluidHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CONDENSER.get(),
                (be, side) -> be.getFluidHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FLUID_PIPE.get(),
                (be, side) -> be.getFluidHandler(side)
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.FLUID_PUMP.get(),
                (be, side) -> be.getEnergyStorage(side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FLUID_PUMP.get(),
                (be, side) -> be.getFluidHandler(side)
        );
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ROBOT.get(), RobotEntity.createAttributes().build());
    }
}
