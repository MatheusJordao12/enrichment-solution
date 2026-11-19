package com.enrichmentsolution.mod.datagen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates mining tool and harvest level tags for all mod blocks.
 */
public class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, EnrichmentSolutionMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(
                        ModBlocks.MACHINE_CASING.get(),
                        ModBlocks.ENRICHMENT_CHAMBER.get(),
                        ModBlocks.REACTOR_CASING.get(),
                        ModBlocks.REACTOR_CORE.get(),
                        ModBlocks.SOLAR_PANEL.get(),
                        ModBlocks.ENERGY_CABLE.get(),
                        ModBlocks.ADVANCED_ENERGY_CABLE.get(),
                        ModBlocks.ULTIMATE_ENERGY_CABLE.get(),
                        ModBlocks.ULTRA_ENERGY_CABLE.get(),
                        ModBlocks.ENERGY_CELL.get(),
                        ModBlocks.ADVANCED_ENERGY_CELL.get(),
                        ModBlocks.ULTIMATE_ENERGY_CELL.get(),
                        ModBlocks.ULTRA_ENERGY_CELL.get(),
                        ModBlocks.ROBOT_CHARGER.get(),
                        ModBlocks.PCB_PRINTER.get(),
                        ModBlocks.STAMPING_PRESS.get(),
                        ModBlocks.TANK_BLOCK.get(),
                        ModBlocks.TANK_WATER_INPUT.get(),
                        ModBlocks.TANK_WATER_OUTPUT.get(),
                        ModBlocks.OIL_REFINERY.get(),
                        ModBlocks.DISTILLATION_TOWER.get(),
                        ModBlocks.HEAT_EXCHANGER.get(),
                        ModBlocks.GEOTHERMAL_GENERATOR.get(),
                        ModBlocks.CONDENSER.get(),
                        ModBlocks.FLUID_PUMP.get(),
                        ModBlocks.BASIC_FLUID_PIPE.get(),
                        ModBlocks.ADVANCED_FLUID_PIPE.get(),
                        ModBlocks.ULTIMATE_FLUID_PIPE.get(),
                        ModBlocks.ULTRA_FLUID_PIPE.get(),
                        ModBlocks.ISOLATED_THERMAL_PIPE.get(),
                        ModBlocks.REINFORCED_THERMAL_PIPE.get(),
                        ModBlocks.VACUUM_THERMAL_PIPE.get(),
                        ModBlocks.CRYOGENIC_THERMAL_PIPE.get(),
                        ModBlocks.STEEL_ORE.get(),
                        ModBlocks.DEEPSLATE_STEEL_ORE.get(),
                        ModBlocks.URANIUM_ORE.get(),
                        ModBlocks.DEEPSLATE_URANIUM_ORE.get(),
                        ModBlocks.BAUXITE_ORE.get(),
                        ModBlocks.DEEPSLATE_BAUXITE_ORE.get(),
                        ModBlocks.ALUMINIUM_ORE.get(),
                        ModBlocks.DEEPSLATE_ALUMINIUM_ORE.get(),
                        ModBlocks.METEOROTITE_ORE.get(),
                        ModBlocks.UNSTABLE_OLD_URANIUM_ORE.get(),
                        ModBlocks.DEEPSLATE_UNSTABLE_OLD_URANIUM_ORE.get(),
                        ModBlocks.OXIDATED_PLUTONIUM_ORE.get(),
                        ModBlocks.DEEPSLATE_OXIDATED_PLUTONIUM_ORE.get()
                );

        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(
                        ModBlocks.MACHINE_CASING.get(),
                        ModBlocks.ENRICHMENT_CHAMBER.get(),
                        ModBlocks.REACTOR_CASING.get(),
                        ModBlocks.REACTOR_CORE.get(),
                        ModBlocks.SOLAR_PANEL.get(),
                        ModBlocks.ENERGY_CELL.get(),
                        ModBlocks.ADVANCED_ENERGY_CELL.get(),
                        ModBlocks.ULTIMATE_ENERGY_CELL.get(),
                        ModBlocks.ULTRA_ENERGY_CELL.get(),
                        ModBlocks.ROBOT_CHARGER.get(),
                        ModBlocks.PCB_PRINTER.get(),
                        ModBlocks.STAMPING_PRESS.get(),
                        ModBlocks.TANK_BLOCK.get(),
                        ModBlocks.TANK_WATER_INPUT.get(),
                        ModBlocks.TANK_WATER_OUTPUT.get(),
                        ModBlocks.OIL_REFINERY.get(),
                        ModBlocks.DISTILLATION_TOWER.get(),
                        ModBlocks.HEAT_EXCHANGER.get(),
                        ModBlocks.GEOTHERMAL_GENERATOR.get(),
                        ModBlocks.CONDENSER.get(),
                        ModBlocks.FLUID_PUMP.get(),
                        ModBlocks.STEEL_ORE.get(),
                        ModBlocks.DEEPSLATE_STEEL_ORE.get(),
                        ModBlocks.URANIUM_ORE.get(),
                        ModBlocks.DEEPSLATE_URANIUM_ORE.get(),
                        ModBlocks.DEEPSLATE_BAUXITE_ORE.get(),
                        ModBlocks.DEEPSLATE_ALUMINIUM_ORE.get()
                );

        tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.DEEPSLATE_UNSTABLE_OLD_URANIUM_ORE.get());
    }
}
