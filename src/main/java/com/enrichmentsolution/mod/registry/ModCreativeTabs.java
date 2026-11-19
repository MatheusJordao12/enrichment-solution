package com.enrichmentsolution.mod.registry;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnrichmentSolutionMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.enrichment_solution.main"))
                    .icon(() -> new ItemStack(ModItems.HI_TECH_GOGGLES.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.MACHINE_CASING.get());
                        output.accept(ModBlocks.STEEL_ORE.get());
                        output.accept(ModBlocks.DEEPSLATE_STEEL_ORE.get());
                        output.accept(ModBlocks.BAUXITE_ORE.get());
                        output.accept(ModBlocks.DEEPSLATE_BAUXITE_ORE.get());
                        output.accept(ModBlocks.ALUMINIUM_ORE.get());
                        output.accept(ModBlocks.DEEPSLATE_ALUMINIUM_ORE.get());
                        output.accept(ModBlocks.URANIUM_ORE.get());
                        output.accept(ModBlocks.DEEPSLATE_URANIUM_ORE.get());
                        output.accept(ModBlocks.ENRICHMENT_CHAMBER.get());
                        output.accept(ModBlocks.SOLAR_PANEL.get());
                        output.accept(ModBlocks.ENERGY_CABLE.get());
                        output.accept(ModBlocks.ADVANCED_ENERGY_CABLE.get());
                        output.accept(ModBlocks.ULTIMATE_ENERGY_CABLE.get());
                        output.accept(ModBlocks.ULTRA_ENERGY_CABLE.get());
                        output.accept(ModBlocks.BASIC_FLUID_PIPE.get());
                        output.accept(ModBlocks.ADVANCED_FLUID_PIPE.get());
                        output.accept(ModBlocks.ULTIMATE_FLUID_PIPE.get());
                        output.accept(ModBlocks.ULTRA_FLUID_PIPE.get());
                        output.accept(ModBlocks.ISOLATED_THERMAL_PIPE.get());
                        output.accept(ModBlocks.REINFORCED_THERMAL_PIPE.get());
                        output.accept(ModBlocks.VACUUM_THERMAL_PIPE.get());
                        output.accept(ModBlocks.CRYOGENIC_THERMAL_PIPE.get());
                        output.accept(ModBlocks.ENERGY_CELL.get());
                        output.accept(ModBlocks.ADVANCED_ENERGY_CELL.get());
                        output.accept(ModBlocks.ULTIMATE_ENERGY_CELL.get());
                        output.accept(ModBlocks.ULTRA_ENERGY_CELL.get());
                        output.accept(ModBlocks.ROBOT_CHARGER.get());
                        output.accept(ModBlocks.PCB_PRINTER.get());
                        output.accept(ModBlocks.STAMPING_PRESS.get());
                        output.accept(ModBlocks.TANK_BLOCK.get());
                        output.accept(ModBlocks.TANK_WATER_INPUT.get());
                        output.accept(ModBlocks.TANK_WATER_OUTPUT.get());
                        output.accept(ModBlocks.OIL_REFINERY.get());
                        output.accept(ModBlocks.HEAT_EXCHANGER.get());
                        output.accept(ModBlocks.GEOTHERMAL_GENERATOR.get());
                        output.accept(ModBlocks.CONDENSER.get());
                        output.accept(ModBlocks.FLUID_PUMP.get());
                        output.accept(ModItems.RAW_URANIUM.get());
                        output.accept(ModItems.URANIUM_INGOT.get());
                        output.accept(ModItems.ENRICHED_URANIUM.get());
                        output.accept(ModItems.SOLAR_UPGRADE_TEMPLATE.get());
                        output.accept(ModItems.AUTO_RECHARGER_UPGRADE_TEMPLATE.get());
                        output.accept(ModItems.ROBOT_CIRCUIT.get());
                        output.accept(ModItems.BASIC_CIRCUIT.get());
                        output.accept(ModItems.ADVANCED_CIRCUIT.get());
                        output.accept(ModItems.ULTIMATE_CIRCUIT.get());
                        output.accept(ModItems.ULTRA_CIRCUIT.get());
                        output.accept(ModItems.HI_TECH_GOGGLES.get());
                        output.accept(ModItems.WHEEL.get());
                        output.accept(ModItems.ROBOT_CASING.get());
                        output.accept(ModItems.PRESSED_ALUMINIUM.get());
                        output.accept(ModItems.MECHANICAL_EYES.get());
                        output.accept(ModItems.RAW_STEEL.get());
                        output.accept(ModItems.STEEL_INGOT.get());
                        output.accept(ModItems.RAW_BAUXITE.get());
                        output.accept(ModItems.BAUXITE_INGOT.get());
                        output.accept(ModItems.RAW_ALUMINIUM.get());
                        output.accept(ModItems.ALUMINIUM_INGOT.get());
                        output.accept(ModItems.ROBOT.get());
                        output.accept(ModItems.CRUDE_OIL_BUCKET.get());
                        output.accept(ModItems.LIQUID_ISOBUTANE_BUCKET.get());
                        output.accept(ModItems.ENRICHMENT_GUIDE.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private ModCreativeTabs() {}
}
