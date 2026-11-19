package com.enrichmentsolution.mod.datagen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.registry.ModBlocks;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Generates item models for standard items and block items.
 * Custom-element item models (cables, pipes, hi_tech_goggles, configurator, plutonium_extractor, isolated_backpack, raw_oxidated_plutonium, hammer_x) remain hand-crafted in {@code src/main/resources}.
 */
public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, EnrichmentSolutionMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerModels() {
        // Simple generated items
        basicItem(ModItems.RAW_URANIUM.get());
        basicItem(ModItems.URANIUM_INGOT.get());
        basicItem(ModItems.ENRICHED_URANIUM.get());
        basicItem(ModItems.SOLAR_UPGRADE_TEMPLATE.get());
        basicItem(ModItems.AUTO_RECHARGER_UPGRADE_TEMPLATE.get());
        basicItem(ModItems.SLOT_UPGRADE_TIER_1.get());
        basicItem(ModItems.SLOT_UPGRADE_TIER_2.get());
        basicItem(ModItems.SLOT_UPGRADE_TIER_3.get());
        basicItem(ModItems.ROBOT_CIRCUIT.get());
        basicItem(ModItems.BASIC_CIRCUIT.get());
        basicItem(ModItems.ADVANCED_CIRCUIT.get());
        basicItem(ModItems.ULTIMATE_CIRCUIT.get());
        basicItem(ModItems.ULTRA_CIRCUIT.get());
        basicItem(ModItems.WHEEL.get());
        basicItem(ModItems.ROBOT_CASING.get());
        basicItem(ModItems.PRESSED_ALUMINIUM.get());
        basicItem(ModItems.MECHANICAL_EYES.get());
        basicItem(ModItems.RAW_STEEL.get());
        basicItem(ModItems.STEEL_INGOT.get());
        basicItem(ModItems.RAW_BAUXITE.get());
        basicItem(ModItems.BAUXITE_INGOT.get());
        basicItem(ModItems.RAW_ALUMINIUM.get());
        basicItem(ModItems.ALUMINIUM_INGOT.get());
        basicItem(ModItems.RAW_METEOROTITE.get());
        basicItem(ModItems.METEOROTITE_INGOT.get());
        basicItem(ModItems.RAW_UNSTABLE_OLD_URANIUM.get());
        basicItem(ModItems.UNSTABLE_OLD_URANIUM_INGOT.get());
        basicItem(ModItems.OXIDATED_PLUTONIUM_INGOT.get());
        basicItem(ModItems.ROBOT.get());
        basicItem(ModItems.CRUDE_OIL_BUCKET.get());
        basicItem(ModItems.LIQUID_ISOBUTANE_BUCKET.get());

        // Custom 3D item model (configurator) lives in src/main/resources/assets/enrichment_solution/models/item/configurator.json

        // Block items using their block model as parent
        simpleBlockItem(ModBlocks.MACHINE_CASING.get());
        simpleBlockItem(ModBlocks.STEEL_ORE.get());
        simpleBlockItem(ModBlocks.DEEPSLATE_STEEL_ORE.get());
        simpleBlockItem(ModBlocks.URANIUM_ORE.get());
        simpleBlockItem(ModBlocks.DEEPSLATE_URANIUM_ORE.get());
        simpleBlockItem(ModBlocks.BAUXITE_ORE.get());
        simpleBlockItem(ModBlocks.DEEPSLATE_BAUXITE_ORE.get());
        simpleBlockItem(ModBlocks.ALUMINIUM_ORE.get());
        simpleBlockItem(ModBlocks.DEEPSLATE_ALUMINIUM_ORE.get());
        simpleBlockItem(ModBlocks.METEOROTITE_ORE.get());
        simpleBlockItem(ModBlocks.UNSTABLE_OLD_URANIUM_ORE.get());
        simpleBlockItem(ModBlocks.DEEPSLATE_UNSTABLE_OLD_URANIUM_ORE.get());
        simpleBlockItem(ModBlocks.OXIDATED_PLUTONIUM_ORE.get());
        simpleBlockItem(ModBlocks.DEEPSLATE_OXIDATED_PLUTONIUM_ORE.get());
        simpleBlockItem(ModBlocks.ENRICHMENT_CHAMBER.get());
        simpleBlockItem(ModBlocks.REACTOR_CASING.get());
        simpleBlockItem(ModBlocks.REACTOR_CORE.get());
        simpleBlockItem(ModBlocks.ENERGY_CELL.get());
        simpleBlockItem(ModBlocks.ADVANCED_ENERGY_CELL.get());
        simpleBlockItem(ModBlocks.ULTIMATE_ENERGY_CELL.get());
        simpleBlockItem(ModBlocks.ULTRA_ENERGY_CELL.get());
        simpleBlockItem(ModBlocks.ROBOT_CHARGER.get());
        simpleBlockItem(ModBlocks.PCB_PRINTER.get());
        simpleBlockItem(ModBlocks.STAMPING_PRESS.get());
        simpleBlockItem(ModBlocks.TANK_BLOCK.get());
        simpleBlockItem(ModBlocks.TANK_WATER_INPUT.get());
        simpleBlockItem(ModBlocks.TANK_WATER_OUTPUT.get());
        simpleBlockItem(ModBlocks.OIL_REFINERY.get());
        simpleBlockItem(ModBlocks.DISTILLATION_TOWER.get());
        simpleBlockItem(ModBlocks.HEAT_EXCHANGER.get());
        simpleBlockItem(ModBlocks.GEOTHERMAL_GENERATOR.get());
        simpleBlockItem(ModBlocks.CONDENSER.get());
        simpleBlockItem(ModBlocks.FLUID_PUMP.get());

        withExistingParent("solar_panel", modLoc("block/solar_panel_full"));
    }
}
