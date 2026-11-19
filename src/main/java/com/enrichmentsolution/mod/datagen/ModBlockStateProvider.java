package com.enrichmentsolution.mod.datagen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.block.CondenserBlock;
import com.enrichmentsolution.mod.common.block.EnergyCableBlock;
import com.enrichmentsolution.mod.common.block.EnrichmentChamberBlock;
import com.enrichmentsolution.mod.common.block.FluidPipeBlock;
import com.enrichmentsolution.mod.common.block.FluidPumpBlock;
import com.enrichmentsolution.mod.common.block.GeothermalGeneratorBlock;
import com.enrichmentsolution.mod.common.block.HeatExchangerBlock;
import com.enrichmentsolution.mod.common.block.OilRefineryBlock;
import com.enrichmentsolution.mod.common.block.ReactorCoreBlock;
import com.enrichmentsolution.mod.common.block.SolarPanelBlock;
import com.enrichmentsolution.mod.common.block.StampingPressBlock;
import com.enrichmentsolution.mod.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Generates blockstates and block models for all mod blocks.
 * Hand-crafted Blockbench models (pipes, cables, press, solar panel, PCB printer, etc.)
 * are referenced via {@link ExistingFileHelper}.
 */
public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, EnrichmentSolutionMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Simple cube_all blocks
        simpleBlock(ModBlocks.MACHINE_CASING.get());
        simpleBlock(ModBlocks.TANK_BLOCK.get());
        simpleBlock(ModBlocks.TANK_WATER_INPUT.get());
        simpleBlock(ModBlocks.TANK_WATER_OUTPUT.get());

        simpleBlock(ModBlocks.ENERGY_CELL.get());
        simpleBlock(ModBlocks.ADVANCED_ENERGY_CELL.get());
        simpleBlock(ModBlocks.ULTIMATE_ENERGY_CELL.get());
        simpleBlock(ModBlocks.ULTRA_ENERGY_CELL.get());

        simpleBlock(ModBlocks.STEEL_ORE.get());
        simpleBlock(ModBlocks.DEEPSLATE_STEEL_ORE.get());
        simpleBlock(ModBlocks.URANIUM_ORE.get());
        simpleBlock(ModBlocks.DEEPSLATE_URANIUM_ORE.get());
        simpleBlock(ModBlocks.BAUXITE_ORE.get());
        simpleBlock(ModBlocks.DEEPSLATE_BAUXITE_ORE.get());
        simpleBlock(ModBlocks.ALUMINIUM_ORE.get());
        simpleBlock(ModBlocks.DEEPSLATE_ALUMINIUM_ORE.get());
        simpleBlock(ModBlocks.METEOROTITE_ORE.get());
        simpleBlock(ModBlocks.UNSTABLE_OLD_URANIUM_ORE.get());
        simpleBlock(ModBlocks.DEEPSLATE_UNSTABLE_OLD_URANIUM_ORE.get());

        // Reactor casing (uses vanilla netherite_block texture)
        simpleBlock(ModBlocks.REACTOR_CASING.get(),
                models().cubeAll("reactor_casing", mcLoc("block/netherite_block")));

        // Reactor core (active=false / active=true)
        ModelFile reactorOff = models().cubeAll("reactor_core", mcLoc("block/deepslate_emerald_ore"));
        ModelFile reactorOn = models().cubeAll("reactor_core_active", mcLoc("block/emerald_block"));
        getVariantBuilder(ModBlocks.REACTOR_CORE.get())
                .partialState().with(ReactorCoreBlock.ACTIVE, false).modelForState().modelFile(reactorOff).addModel()
                .partialState().with(ReactorCoreBlock.ACTIVE, true).modelForState().modelFile(reactorOn).addModel();

        // Custom-modeled ores
        simpleExistingBlock(ModBlocks.OXIDATED_PLUTONIUM_ORE.get(), "oxidated_plutonium_ore");
        simpleExistingBlock(ModBlocks.DEEPSLATE_OXIDATED_PLUTONIUM_ORE.get(), "deepslate_oxidated_plutonium_ore");

        // Fluid blocks (particle-only models)
        simpleBlock(ModBlocks.CRUDE_OIL_BLOCK.get(),
                models().getBuilder("crude_oil_block")
                        .texture("particle", modLoc("block/fluid/crude_oil_still")));
        simpleBlock(ModBlocks.LIQUID_ISOBUTANE_BLOCK.get(),
                models().getBuilder("liquid_isobutane_block")
                        .texture("particle", modLoc("block/fluid/liquid_isobutane_still")));

        // Orientable lit/unlit machines
        orientableMachine(ModBlocks.CONDENSER.get(), "condenser", CondenserBlock.LIT);
        orientableMachine(ModBlocks.GEOTHERMAL_GENERATOR.get(), "geothermal_generator", GeothermalGeneratorBlock.LIT);
        orientableMachine(ModBlocks.HEAT_EXCHANGER.get(), "heat_exchanger", HeatExchangerBlock.LIT);
        orientableMachine(ModBlocks.OIL_REFINERY.get(), "oil_refinery", OilRefineryBlock.LIT);

        // Fluid pump (custom parent fluid_pump_base)
        ModelFile pumpOff = models().withExistingParent("fluid_pump", modLoc("block/fluid_pump_base"))
                .texture("top", modLoc("block/fluid_pump_top"))
                .texture("front", modLoc("block/fluid_pump_front"))
                .texture("side", modLoc("block/fluid_pump_side"));
        ModelFile pumpOn = models().withExistingParent("fluid_pump_on", modLoc("block/fluid_pump_base"))
                .texture("top", modLoc("block/fluid_pump_top"))
                .texture("front", modLoc("block/fluid_pump_front_on"))
                .texture("side", modLoc("block/fluid_pump_side"));
        horizontalBlock(ModBlocks.FLUID_PUMP.get(),
                state -> state.getValue(FluidPumpBlock.LIT) ? pumpOn : pumpOff);

        // Custom-modeled facing machines
        horizontalBlock(ModBlocks.DISTILLATION_TOWER.get(), existingBlockModel("distillation_tower"));
        horizontalBlock(ModBlocks.PCB_PRINTER.get(), existingBlockModel("pcb_printer"));
        horizontalBlock(ModBlocks.ROBOT_CHARGER.get(), existingBlockModel("robot_charger"));

        ModelFile pressOff = existingBlockModel("stamping_press");
        ModelFile pressOn = existingBlockModel("stamping_press_on");
        horizontalBlock(ModBlocks.STAMPING_PRESS.get(),
                state -> state.getValue(StampingPressBlock.LIT) ? pressOn : pressOff);

        ModelFile solarBasic = existingBlockModel("solar_panel");
        ModelFile solarAdvanced = existingBlockModel("solar_panel_advanced_base");
        horizontalBlock(ModBlocks.SOLAR_PANEL.get(),
                state -> state.getValue(SolarPanelBlock.ADVANCED) ? solarAdvanced : solarBasic);

        // Enrichment chamber (facing + lit + quad_role)
        registerEnrichmentChamber();

        // Multipart cables and pipes
        registerCable(ModBlocks.ENERGY_CABLE.get(), "energy_cable");
        registerCable(ModBlocks.ADVANCED_ENERGY_CABLE.get(), "advanced_energy_cable");
        registerCable(ModBlocks.ULTIMATE_ENERGY_CABLE.get(), "ultimate_energy_cable");
        registerCable(ModBlocks.ULTRA_ENERGY_CABLE.get(), "ultra_energy_cable");

        registerFluidPipe(ModBlocks.BASIC_FLUID_PIPE.get(), "basic_fluid_pipe");
        registerFluidPipe(ModBlocks.ADVANCED_FLUID_PIPE.get(), "advanced_fluid_pipe");
        registerFluidPipe(ModBlocks.ULTIMATE_FLUID_PIPE.get(), "ultimate_fluid_pipe");
        registerFluidPipe(ModBlocks.ULTRA_FLUID_PIPE.get(), "ultra_fluid_pipe");

        registerFluidPipe(ModBlocks.ISOLATED_THERMAL_PIPE.get(), "isolated_thermal_pipe");
        registerFluidPipe(ModBlocks.REINFORCED_THERMAL_PIPE.get(), "reinforced_thermal_pipe");
        registerFluidPipe(ModBlocks.VACUUM_THERMAL_PIPE.get(), "vacuum_thermal_pipe");
        registerFluidPipe(ModBlocks.CRYOGENIC_THERMAL_PIPE.get(), "cryogenic_thermal_pipe");
    }

    private void simpleExistingBlock(Block block, String modelName) {
        simpleBlock(block, existingBlockModel(modelName));
    }

    private ModelFile existingBlockModel(String name) {
        return models().getExistingFile(modLoc("block/" + name));
    }

    private void orientableMachine(Block block, String name, BooleanProperty litProp) {
        ModelFile offModel = models().orientable(name,
                modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front"),
                modLoc("block/" + name + "_top"));
        ModelFile onModel = models().orientable(name + "_on",
                modLoc("block/" + name + "_side"),
                modLoc("block/" + name + "_front_on"),
                modLoc("block/" + name + "_top"));
        horizontalBlock(block, state -> state.getValue(litProp) ? onModel : offModel);
    }

    private void registerEnrichmentChamber() {
        ModelFile singleOff = existingBlockModel("enrichment_chamber");
        ModelFile singleOn = existingBlockModel("enrichment_chamber_on");
        ModelFile quadOff = existingBlockModel("quad_enrichment_chamber_off");
        ModelFile quadOn = existingBlockModel("quad_enrichment_chamber");
        ModelFile quadEmpty = existingBlockModel("quad_enrichment_empty");

        getVariantBuilder(ModBlocks.ENRICHMENT_CHAMBER.get()).forAllStates(state -> {
            EnrichmentChamberBlock.QuadRole role = state.getValue(EnrichmentChamberBlock.QUAD_ROLE);
            if (role == EnrichmentChamberBlock.QuadRole.RIGHT
                    || role == EnrichmentChamberBlock.QuadRole.TOP_LEFT
                    || role == EnrichmentChamberBlock.QuadRole.TOP_RIGHT) {
                return ConfiguredModel.builder().modelFile(quadEmpty).build();
            }
            boolean lit = state.getValue(EnrichmentChamberBlock.LIT);
            ModelFile model = role == EnrichmentChamberBlock.QuadRole.MASTER
                    ? (lit ? quadOn : quadOff)
                    : (lit ? singleOn : singleOff);
            int yRot = ((int) state.getValue(EnrichmentChamberBlock.FACING).toYRot() + 180) % 360;
            return ConfiguredModel.builder().modelFile(model).rotationY(yRot).build();
        });
    }

    private void registerCable(Block block, String name) {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
        builder.part().modelFile(existingBlockModel(name + "_core")).addModel().end();

        ModelFile arm = existingBlockModel(name + "_arm");
        ModelFile machine = existingBlockModel(name + "_machine");
        ModelFile pump = existingBlockModel(name + "_pump");

        for (Direction dir : Direction.values()) {
            var prop = EnergyCableBlock.property(dir);
            addDirectionalPart(builder, dir, arm, prop, EnergyCableBlock.ConnectionType.CABLE);
            addDirectionalPart(builder, dir, machine, prop, EnergyCableBlock.ConnectionType.MACHINE);
            addDirectionalPart(builder, dir, pump, prop, EnergyCableBlock.ConnectionType.PUMP);
        }
    }

    private void registerFluidPipe(Block block, String name) {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
        builder.part().modelFile(existingBlockModel(name + "_core")).addModel().end();

        ModelFile arm = existingBlockModel(name + "_arm");
        ModelFile machine = existingBlockModel(name + "_machine");
        ModelFile pump = existingBlockModel(name + "_pump");

        for (Direction dir : Direction.values()) {
            var prop = FluidPipeBlock.property(dir);
            addDirectionalPart(builder, dir, arm, prop, FluidPipeBlock.ConnectionType.PIPE);
            addDirectionalPart(builder, dir, machine, prop, FluidPipeBlock.ConnectionType.MACHINE);
            addDirectionalPart(builder, dir, pump, prop, FluidPipeBlock.ConnectionType.PUMP);
        }
    }

    private static <T extends Comparable<T>> void addDirectionalPart(
            MultiPartBlockStateBuilder builder,
            Direction dir,
            ModelFile model,
            net.minecraft.world.level.block.state.properties.Property<T> prop,
            T value
    ) {
        var part = builder.part().modelFile(model);
        switch (dir) {
            case EAST -> part.rotationY(90);
            case SOUTH -> part.rotationY(180);
            case WEST -> part.rotationY(270);
            case UP -> part.rotationX(270);
            case DOWN -> part.rotationX(90);
            case NORTH -> {}
        }
        part.addModel().condition(prop, value).end();
    }
}
