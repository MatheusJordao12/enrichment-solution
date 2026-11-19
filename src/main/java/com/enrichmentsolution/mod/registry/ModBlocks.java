package com.enrichmentsolution.mod.registry;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.block.BeltDriveBlock;
import com.enrichmentsolution.mod.common.block.CableTier;
import com.enrichmentsolution.mod.common.block.CondenserBlock;
import com.enrichmentsolution.mod.common.block.ConveyorBeltBlock;
import com.enrichmentsolution.mod.common.block.DistillationTowerBlock;
import com.enrichmentsolution.mod.common.block.EnergyCableBlock;
import com.enrichmentsolution.mod.common.block.EnergyCellBlock;
import com.enrichmentsolution.mod.common.block.EnergyCellTier;
import com.enrichmentsolution.mod.common.block.EnrichmentChamberBlock;
import com.enrichmentsolution.mod.common.block.FluidConduitTier;
import com.enrichmentsolution.mod.common.block.FluidPipeBlock;
import com.enrichmentsolution.mod.common.block.FluidPipeTier;
import com.enrichmentsolution.mod.common.block.FluidPumpBlock;
import com.enrichmentsolution.mod.common.block.GeothermalGeneratorBlock;
import com.enrichmentsolution.mod.common.block.HeatExchangerBlock;
import com.enrichmentsolution.mod.common.block.OilRefineryBlock;
import com.enrichmentsolution.mod.common.block.OxidatedPlutoniumOreBlock;
import com.enrichmentsolution.mod.common.block.PcbPrinterBlock;
import com.enrichmentsolution.mod.common.block.ReactorCoreBlock;
import com.enrichmentsolution.mod.common.block.RobotChargerBlock;
import com.enrichmentsolution.mod.common.block.SolarPanelBlock;
import com.enrichmentsolution.mod.common.block.StampingPressBlock;
import com.enrichmentsolution.mod.common.block.TankBlock;
import com.enrichmentsolution.mod.common.block.TankPortBlock;
import com.enrichmentsolution.mod.common.block.ThermalPipeTier;
import com.enrichmentsolution.mod.common.item.EnergyCellBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnrichmentSolutionMod.MOD_ID);

    public static final DeferredBlock<Block> MACHINE_CASING = registerBlock("machine_casing",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final DeferredBlock<Block> STEEL_ORE = registerBlock("steel_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> DEEPSLATE_STEEL_ORE = registerBlock("deepslate_steel_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<Block> BAUXITE_ORE = registerBlock("bauxite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> DEEPSLATE_BAUXITE_ORE = registerBlock("deepslate_bauxite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<Block> ALUMINIUM_ORE = registerBlock("aluminium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> DEEPSLATE_ALUMINIUM_ORE = registerBlock("deepslate_aluminium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<Block> METEOROTITE_ORE = registerBlock("meteorotite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(4.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.BASALT)));

    public static final DeferredBlock<Block> UNSTABLE_OLD_URANIUM_ORE = registerBlock("unstable_old_uranium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5F, 4.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> DEEPSLATE_UNSTABLE_OLD_URANIUM_ORE = registerBlock("deepslate_unstable_old_uranium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(5.0F, 4.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<Block> OXIDATED_PLUTONIUM_ORE = registerBlock("oxidated_plutonium_ore",
            () -> new OxidatedPlutoniumOreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(4.0F, 5.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    public static final DeferredBlock<Block> DEEPSLATE_OXIDATED_PLUTONIUM_ORE = registerBlock("deepslate_oxidated_plutonium_ore",
            () -> new OxidatedPlutoniumOreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(5.5F, 5.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)
                    .noOcclusion()));

    public static final DeferredBlock<Block> URANIUM_ORE = registerBlock("uranium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> DEEPSLATE_URANIUM_ORE = registerBlock("deepslate_uranium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<Block> ENRICHMENT_CHAMBER = registerBlock("enrichment_chamber",
            () -> new EnrichmentChamberBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(EnrichmentChamberBlock.LIT) ? 10 : 0)));

    public static final DeferredBlock<Block> REACTOR_CASING = registerBlock("reactor_casing",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(6.0F, 12.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final DeferredBlock<Block> REACTOR_CORE = registerBlock("reactor_core",
            () -> new ReactorCoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(6.5F, 12.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(ReactorCoreBlock.ACTIVE) ? 14 : 2)));

    public static final DeferredBlock<Block> SOLAR_PANEL = registerBlock("solar_panel",
            () -> new SolarPanelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.0F, 5.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<Block> ENERGY_CABLE = registerCable("energy_cable", CableTier.BASIC);
    public static final DeferredBlock<Block> ADVANCED_ENERGY_CABLE = registerCable("advanced_energy_cable", CableTier.ADVANCED);
    public static final DeferredBlock<Block> ULTIMATE_ENERGY_CABLE = registerCable("ultimate_energy_cable", CableTier.ULTIMATE);
    public static final DeferredBlock<Block> ULTRA_ENERGY_CABLE = registerCable("ultra_energy_cable", CableTier.ULTRA);

    public static final DeferredBlock<Block> BASIC_FLUID_PIPE = registerFluidPipe("basic_fluid_pipe", FluidPipeTier.BASIC);
    public static final DeferredBlock<Block> ADVANCED_FLUID_PIPE = registerFluidPipe("advanced_fluid_pipe", FluidPipeTier.ADVANCED);
    public static final DeferredBlock<Block> ULTIMATE_FLUID_PIPE = registerFluidPipe("ultimate_fluid_pipe", FluidPipeTier.ULTIMATE);
    public static final DeferredBlock<Block> ULTRA_FLUID_PIPE = registerFluidPipe("ultra_fluid_pipe", FluidPipeTier.ULTRA);

    public static final DeferredBlock<Block> ISOLATED_THERMAL_PIPE = registerFluidPipe("isolated_thermal_pipe", ThermalPipeTier.ISOLATED);
    public static final DeferredBlock<Block> REINFORCED_THERMAL_PIPE = registerFluidPipe("reinforced_thermal_pipe", ThermalPipeTier.REINFORCED);
    public static final DeferredBlock<Block> VACUUM_THERMAL_PIPE = registerFluidPipe("vacuum_thermal_pipe", ThermalPipeTier.VACUUM);
    public static final DeferredBlock<Block> CRYOGENIC_THERMAL_PIPE = registerFluidPipe("cryogenic_thermal_pipe", ThermalPipeTier.CRYOGENIC);

    public static final DeferredBlock<EnergyCellBlock> ENERGY_CELL = registerEnergyCell("energy_cell", EnergyCellTier.BASIC);
    public static final DeferredBlock<EnergyCellBlock> ADVANCED_ENERGY_CELL = registerEnergyCell("advanced_energy_cell", EnergyCellTier.ADVANCED);
    public static final DeferredBlock<EnergyCellBlock> ULTIMATE_ENERGY_CELL = registerEnergyCell("ultimate_energy_cell", EnergyCellTier.ULTIMATE);
    public static final DeferredBlock<EnergyCellBlock> ULTRA_ENERGY_CELL = registerEnergyCell("ultra_energy_cell", EnergyCellTier.ULTRA);

    public static final DeferredBlock<Block> ROBOT_CHARGER = registerBlock("robot_charger",
            () -> new RobotChargerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<Block> PCB_PRINTER = registerBlock("pcb_printer",
            () -> new PcbPrinterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<Block> STAMPING_PRESS = registerBlock("stamping_press",
            () -> new StampingPressBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(StampingPressBlock.LIT) ? 10 : 0)));

    public static final DeferredBlock<Block> TANK_BLOCK = registerBlock("tank_block",
            () -> new TankBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final DeferredBlock<Block> TANK_WATER_INPUT = registerBlock("tank_water_input",
            () -> new TankPortBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL), TankPortBlock.PortMode.INPUT));

    public static final DeferredBlock<Block> TANK_WATER_OUTPUT = registerBlock("tank_water_output",
            () -> new TankPortBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL), TankPortBlock.PortMode.OUTPUT));

    public static final DeferredBlock<Block> OIL_REFINERY = registerBlock("oil_refinery",
            () -> new OilRefineryBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(OilRefineryBlock.LIT) ? 10 : 0)));

    public static final DeferredBlock<Block> DISTILLATION_TOWER = registerBlock("distillation_tower",
            () -> new DistillationTowerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<Block> HEAT_EXCHANGER = registerBlock("heat_exchanger",
            () -> new HeatExchangerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(HeatExchangerBlock.LIT) ? 10 : 0)));

    public static final DeferredBlock<Block> GEOTHERMAL_GENERATOR = registerBlock("geothermal_generator",
            () -> new GeothermalGeneratorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(GeothermalGeneratorBlock.LIT) ? 10 : 0)));

    public static final DeferredBlock<Block> CONDENSER = registerBlock("condenser",
            () -> new CondenserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(CondenserBlock.LIT) ? 10 : 0)));

    public static final DeferredBlock<Block> FLUID_PUMP = registerBlock("fluid_pump",
            () -> new FluidPumpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(FluidPumpBlock.LIT) ? 10 : 0)));

    public static final DeferredBlock<Block> BELT_DRIVE = registerBlock("belt_drive",
            () -> new BeltDriveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<Block> CONVEYOR_BELT = registerBlock("conveyor_belt",
            () -> new ConveyorBeltBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 5.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<LiquidBlock> CRUDE_OIL_BLOCK = BLOCKS.register("crude_oil_block",
            () -> new LiquidBlock(ModFluids.CRUDE_OIL.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                    .mapColor(MapColor.COLOR_BLACK)
                    .noLootTable()));

    public static final DeferredBlock<LiquidBlock> LIQUID_ISOBUTANE_BLOCK = BLOCKS.register("liquid_isobutane_block",
            () -> new LiquidBlock(ModFluids.LIQUID_ISOBUTANE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                    .mapColor(MapColor.ICE)
                    .noLootTable()));

    private static DeferredBlock<Block> registerCable(String name, CableTier tier) {
        return registerBlock(name, () -> new EnergyCableBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5F, 3.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(), tier));
    }

    private static DeferredBlock<Block> registerFluidPipe(String name, FluidConduitTier tier) {
        return registerBlock(name, () -> new FluidPipeBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5F, 3.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion(), tier));
    }

    private static DeferredBlock<EnergyCellBlock> registerEnergyCell(String name, EnergyCellTier tier) {
        DeferredBlock<EnergyCellBlock> registered = BLOCKS.register(name, () -> new EnergyCellBlock(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(4.0F, 6.0F)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL),
                tier));
        ModItems.ITEMS.register(name, () -> new EnergyCellBlockItem(registered.get(), new Item.Properties().stacksTo(1)));
        return registered;
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> registered = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private ModBlocks() {}
}
