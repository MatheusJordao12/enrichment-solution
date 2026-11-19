package com.enrichmentsolution.mod.registry;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.block.entity.BeltDriveBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.CondenserBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.ConveyorBeltBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.EnergyCableBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.EnergyCellBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.EnrichmentChamberBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.FluidPipeBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.FluidPumpBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.GeothermalGeneratorBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.HeatExchangerBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.OilRefineryBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.PcbPrinterBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.ReactorCoreBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.RobotChargerBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.SolarPanelBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.StampingPressBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.TankBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.TankPortBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnrichmentSolutionMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnrichmentChamberBlockEntity>> ENRICHMENT_CHAMBER =
            BLOCK_ENTITIES.register("enrichment_chamber", () -> BlockEntityType.Builder
                    .of(EnrichmentChamberBlockEntity::new, ModBlocks.ENRICHMENT_CHAMBER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorCoreBlockEntity>> REACTOR_CORE =
            BLOCK_ENTITIES.register("reactor_core", () -> BlockEntityType.Builder
                    .of(ReactorCoreBlockEntity::new, ModBlocks.REACTOR_CORE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL =
            BLOCK_ENTITIES.register("solar_panel", () -> BlockEntityType.Builder
                    .of(SolarPanelBlockEntity::new, ModBlocks.SOLAR_PANEL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCableBlockEntity>> ENERGY_CABLE =
            BLOCK_ENTITIES.register("energy_cable", () -> BlockEntityType.Builder
                    .of(EnergyCableBlockEntity::new,
                            ModBlocks.ENERGY_CABLE.get(),
                            ModBlocks.ADVANCED_ENERGY_CABLE.get(),
                            ModBlocks.ULTIMATE_ENERGY_CABLE.get(),
                            ModBlocks.ULTRA_ENERGY_CABLE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCellBlockEntity>> ENERGY_CELL =
            BLOCK_ENTITIES.register("energy_cell", () -> BlockEntityType.Builder
                    .of(EnergyCellBlockEntity::new,
                            ModBlocks.ENERGY_CELL.get(),
                            ModBlocks.ADVANCED_ENERGY_CELL.get(),
                            ModBlocks.ULTIMATE_ENERGY_CELL.get(),
                            ModBlocks.ULTRA_ENERGY_CELL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RobotChargerBlockEntity>> ROBOT_CHARGER =
            BLOCK_ENTITIES.register("robot_charger", () -> BlockEntityType.Builder
                    .of(RobotChargerBlockEntity::new, ModBlocks.ROBOT_CHARGER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PcbPrinterBlockEntity>> PCB_PRINTER =
            BLOCK_ENTITIES.register("pcb_printer", () -> BlockEntityType.Builder
                    .of(PcbPrinterBlockEntity::new, ModBlocks.PCB_PRINTER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StampingPressBlockEntity>> STAMPING_PRESS =
            BLOCK_ENTITIES.register("stamping_press", () -> BlockEntityType.Builder
                    .of(StampingPressBlockEntity::new, ModBlocks.STAMPING_PRESS.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK_BLOCK =
            BLOCK_ENTITIES.register("tank_block", () -> BlockEntityType.Builder
                    .of(TankBlockEntity::new, ModBlocks.TANK_BLOCK.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankPortBlockEntity>> TANK_PORT =
            BLOCK_ENTITIES.register("tank_port", () -> BlockEntityType.Builder
                    .of(TankPortBlockEntity::new,
                            ModBlocks.TANK_WATER_INPUT.get(),
                            ModBlocks.TANK_WATER_OUTPUT.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilRefineryBlockEntity>> OIL_REFINERY =
            BLOCK_ENTITIES.register("oil_refinery", () -> BlockEntityType.Builder
                    .of(OilRefineryBlockEntity::new, ModBlocks.OIL_REFINERY.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatExchangerBlockEntity>> HEAT_EXCHANGER =
            BLOCK_ENTITIES.register("heat_exchanger", () -> BlockEntityType.Builder
                    .of(HeatExchangerBlockEntity::new, ModBlocks.HEAT_EXCHANGER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeothermalGeneratorBlockEntity>> GEOTHERMAL_GENERATOR =
            BLOCK_ENTITIES.register("geothermal_generator", () -> BlockEntityType.Builder
                    .of(GeothermalGeneratorBlockEntity::new, ModBlocks.GEOTHERMAL_GENERATOR.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CondenserBlockEntity>> CONDENSER =
            BLOCK_ENTITIES.register("condenser", () -> BlockEntityType.Builder
                    .of(CondenserBlockEntity::new, ModBlocks.CONDENSER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE =
            BLOCK_ENTITIES.register("fluid_pipe", () -> BlockEntityType.Builder
                    .of(FluidPipeBlockEntity::new,
                            ModBlocks.BASIC_FLUID_PIPE.get(),
                            ModBlocks.ADVANCED_FLUID_PIPE.get(),
                            ModBlocks.ULTIMATE_FLUID_PIPE.get(),
                            ModBlocks.ULTRA_FLUID_PIPE.get(),
                            ModBlocks.ISOLATED_THERMAL_PIPE.get(),
                            ModBlocks.REINFORCED_THERMAL_PIPE.get(),
                            ModBlocks.VACUUM_THERMAL_PIPE.get(),
                            ModBlocks.CRYOGENIC_THERMAL_PIPE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPumpBlockEntity>> FLUID_PUMP =
            BLOCK_ENTITIES.register("fluid_pump", () -> BlockEntityType.Builder
                    .of(FluidPumpBlockEntity::new, ModBlocks.FLUID_PUMP.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeltDriveBlockEntity>> BELT_DRIVE =
            BLOCK_ENTITIES.register("belt_drive", () -> BlockEntityType.Builder
                    .of(BeltDriveBlockEntity::new, ModBlocks.BELT_DRIVE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBeltBlockEntity>> CONVEYOR_BELT =
            BLOCK_ENTITIES.register("conveyor_belt", () -> BlockEntityType.Builder
                    .of(ConveyorBeltBlockEntity::new, ModBlocks.CONVEYOR_BELT.get())
                    .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    private ModBlockEntities() {}
}
