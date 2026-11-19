package com.enrichmentsolution.mod.datagen;

import com.enrichmentsolution.mod.registry.ModBlocks;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;

/**
 * Generates block loot tables for all mod blocks.
 * Liquid blocks use {@code .noLootTable()} in {@link ModBlocks} and are skipped automatically.
 */
public class ModBlockLootTableProvider extends BlockLootSubProvider {

    public ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        // Simple self-drop blocks
        dropSelf(ModBlocks.MACHINE_CASING.get());
        dropSelf(ModBlocks.ENRICHMENT_CHAMBER.get());
        dropSelf(ModBlocks.REACTOR_CASING.get());
        dropSelf(ModBlocks.REACTOR_CORE.get());
        dropSelf(ModBlocks.SOLAR_PANEL.get());
        dropSelf(ModBlocks.ROBOT_CHARGER.get());
        dropSelf(ModBlocks.PCB_PRINTER.get());
        dropSelf(ModBlocks.STAMPING_PRESS.get());
        dropSelf(ModBlocks.TANK_BLOCK.get());
        dropSelf(ModBlocks.TANK_WATER_INPUT.get());
        dropSelf(ModBlocks.TANK_WATER_OUTPUT.get());
        dropSelf(ModBlocks.OIL_REFINERY.get());
        dropSelf(ModBlocks.DISTILLATION_TOWER.get());
        dropSelf(ModBlocks.HEAT_EXCHANGER.get());
        dropSelf(ModBlocks.GEOTHERMAL_GENERATOR.get());
        dropSelf(ModBlocks.CONDENSER.get());
        dropSelf(ModBlocks.FLUID_PUMP.get());

        // Energy cables
        dropSelf(ModBlocks.ENERGY_CABLE.get());
        dropSelf(ModBlocks.ADVANCED_ENERGY_CABLE.get());
        dropSelf(ModBlocks.ULTIMATE_ENERGY_CABLE.get());
        dropSelf(ModBlocks.ULTRA_ENERGY_CABLE.get());

        // Fluid & thermal pipes
        dropSelf(ModBlocks.BASIC_FLUID_PIPE.get());
        dropSelf(ModBlocks.ADVANCED_FLUID_PIPE.get());
        dropSelf(ModBlocks.ULTIMATE_FLUID_PIPE.get());
        dropSelf(ModBlocks.ULTRA_FLUID_PIPE.get());
        dropSelf(ModBlocks.ISOLATED_THERMAL_PIPE.get());
        dropSelf(ModBlocks.REINFORCED_THERMAL_PIPE.get());
        dropSelf(ModBlocks.VACUUM_THERMAL_PIPE.get());
        dropSelf(ModBlocks.CRYOGENIC_THERMAL_PIPE.get());

        // Energy cells preserve stored_energy component
        addEnergyCellDrop(ModBlocks.ENERGY_CELL.get());
        addEnergyCellDrop(ModBlocks.ADVANCED_ENERGY_CELL.get());
        addEnergyCellDrop(ModBlocks.ULTIMATE_ENERGY_CELL.get());
        addEnergyCellDrop(ModBlocks.ULTRA_ENERGY_CELL.get());

        // Ores (silk touch -> block, otherwise raw ore with fortune)
        add(ModBlocks.STEEL_ORE.get(), block -> createOreDrop(block, ModItems.RAW_STEEL.get()));
        add(ModBlocks.DEEPSLATE_STEEL_ORE.get(), block -> createOreDrop(block, ModItems.RAW_STEEL.get()));
        add(ModBlocks.URANIUM_ORE.get(), block -> createOreDrop(block, ModItems.RAW_URANIUM.get()));
        add(ModBlocks.DEEPSLATE_URANIUM_ORE.get(), block -> createOreDrop(block, ModItems.RAW_URANIUM.get()));
        add(ModBlocks.BAUXITE_ORE.get(), block -> createOreDrop(block, ModItems.RAW_BAUXITE.get()));
        add(ModBlocks.DEEPSLATE_BAUXITE_ORE.get(), block -> createOreDrop(block, ModItems.RAW_BAUXITE.get()));
        add(ModBlocks.ALUMINIUM_ORE.get(), block -> createOreDrop(block, ModItems.RAW_ALUMINIUM.get()));
        add(ModBlocks.DEEPSLATE_ALUMINIUM_ORE.get(), block -> createOreDrop(block, ModItems.RAW_ALUMINIUM.get()));
        add(ModBlocks.METEOROTITE_ORE.get(), block -> createOreDrop(block, ModItems.RAW_METEOROTITE.get()));
        add(ModBlocks.UNSTABLE_OLD_URANIUM_ORE.get(), block -> createOreDrop(block, ModItems.RAW_UNSTABLE_OLD_URANIUM.get()));
        add(ModBlocks.DEEPSLATE_UNSTABLE_OLD_URANIUM_ORE.get(), block -> createOreDrop(block, ModItems.RAW_UNSTABLE_OLD_URANIUM.get()));
        add(ModBlocks.OXIDATED_PLUTONIUM_ORE.get(), noDrop());
        add(ModBlocks.DEEPSLATE_OXIDATED_PLUTONIUM_ORE.get(), noDrop());
    }

    private void addEnergyCellDrop(Block cellBlock) {
        add(cellBlock, block -> LootTable.lootTable()
                .withPool(applyExplosionCondition(block, LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(block)
                                .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                        .include(ModDataComponents.STORED_ENERGY.get()))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
