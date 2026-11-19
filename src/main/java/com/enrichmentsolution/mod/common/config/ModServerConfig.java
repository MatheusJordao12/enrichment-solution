package com.enrichmentsolution.mod.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side balance and capacity configuration for Enrichment Solution.
 * All defaults match the original hardcoded machine, generator, reactor, and conduit values.
 */
public final class ModServerConfig {

    public static final ModConfigSpec SPEC;

    // Enrichment Chamber
    public static final ModConfigSpec.IntValue ENRICHMENT_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue ENRICHMENT_MAX_RECEIVE;
    public static final ModConfigSpec.IntValue ENRICHMENT_DEFAULT_PROCESS_TIME;
    public static final ModConfigSpec.IntValue ENRICHMENT_DEFAULT_QUAD_PROCESS_TIME;
    public static final ModConfigSpec.DoubleValue ENRICHMENT_ENERGY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ENRICHMENT_TIME_MULTIPLIER;

    // Oil Refinery
    public static final ModConfigSpec.IntValue OIL_REFINERY_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue OIL_REFINERY_MAX_RECEIVE;
    public static final ModConfigSpec.IntValue OIL_REFINERY_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue OIL_REFINERY_AUTO_PUSH_RATE;
    public static final ModConfigSpec.IntValue OIL_REFINERY_DEFAULT_PROCESS_TIME;
    public static final ModConfigSpec.DoubleValue OIL_REFINERY_ENERGY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue OIL_REFINERY_TIME_MULTIPLIER;

    // Stamping Press
    public static final ModConfigSpec.IntValue STAMPING_PRESS_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue STAMPING_PRESS_MAX_RECEIVE;
    public static final ModConfigSpec.IntValue STAMPING_PRESS_DEFAULT_PROCESS_TICKS;
    public static final ModConfigSpec.DoubleValue STAMPING_PRESS_ENERGY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue STAMPING_PRESS_TIME_MULTIPLIER;

    // PCB Printer
    public static final ModConfigSpec.IntValue PCB_PRINTER_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue PCB_PRINTER_MAX_RECEIVE;
    public static final ModConfigSpec.IntValue PCB_PRINTER_DEFAULT_PRINT_TICKS;
    public static final ModConfigSpec.DoubleValue PCB_PRINTER_ENERGY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue PCB_PRINTER_TIME_MULTIPLIER;

    // Geothermal Generator
    public static final ModConfigSpec.IntValue GEOTHERMAL_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue GEOTHERMAL_MAX_EXTRACT;
    public static final ModConfigSpec.IntValue GEOTHERMAL_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue GEOTHERMAL_MAX_PROCESS_MB_PER_TICK;
    public static final ModConfigSpec.IntValue GEOTHERMAL_FE_PER_MB;

    // Solar Panel
    public static final ModConfigSpec.IntValue SOLAR_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue SOLAR_MAX_EXTRACT;
    public static final ModConfigSpec.IntValue SOLAR_FE_PER_TICK_CLEAR;
    public static final ModConfigSpec.IntValue SOLAR_FE_PER_TICK_RAIN;
    public static final ModConfigSpec.IntValue SOLAR_ADV_FE_PER_TICK_CLEAR;
    public static final ModConfigSpec.IntValue SOLAR_ADV_FE_PER_TICK_RAIN;

    // Condenser
    public static final ModConfigSpec.IntValue CONDENSER_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue CONDENSER_MAX_CONDENSE_PER_TICK;
    public static final ModConfigSpec.IntValue CONDENSER_MAX_PUSH_PER_TICK;
    public static final ModConfigSpec.IntValue CONDENSER_GAS_PER_WATER_MB;

    // Fluid Pump
    public static final ModConfigSpec.IntValue FLUID_PUMP_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue FLUID_PUMP_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue FLUID_PUMP_MAX_RECEIVE;
    public static final ModConfigSpec.IntValue FLUID_PUMP_ENERGY_PER_PUMP;
    public static final ModConfigSpec.IntValue FLUID_PUMP_INTERVAL;
    public static final ModConfigSpec.IntValue FLUID_PUMP_MAX_SCAN_DEPTH;
    public static final ModConfigSpec.IntValue FLUID_PUMP_HORIZONTAL_RADIUS;
    public static final ModConfigSpec.IntValue FLUID_PUMP_MAX_TRACE_NODES;
    public static final ModConfigSpec.IntValue FLUID_PUMP_AUTO_PUSH_RATE;

    // Reactor Core
    public static final ModConfigSpec.IntValue REACTOR_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue REACTOR_MAX_EXTRACT;
    public static final ModConfigSpec.IntValue REACTOR_FE_PER_TICK;
    public static final ModConfigSpec.IntValue REACTOR_FUEL_BURN_TIME;
    public static final ModConfigSpec.IntValue REACTOR_MAX_HEAT;

    // Heat Exchanger
    public static final ModConfigSpec.IntValue HEAT_EXCHANGER_MAX_SPAN;
    public static final ModConfigSpec.IntValue HEAT_EXCHANGER_ISOBUTANE_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue HEAT_EXCHANGER_WATER_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue HEAT_EXCHANGER_MIN_BOIL_RATE;
    public static final ModConfigSpec.IntValue HEAT_EXCHANGER_MAX_BOIL_RATE;
    public static final ModConfigSpec.IntValue HEAT_EXCHANGER_MAX_PUSH_RATE;

    // Multiblock Tank
    public static final ModConfigSpec.IntValue TANK_CAPACITY_PER_BLOCK;
    public static final ModConfigSpec.IntValue TANK_MAX_SPAN;

    // Robot Charger
    public static final ModConfigSpec.IntValue ROBOT_CHARGER_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue ROBOT_CHARGER_MAX_RECEIVE;
    public static final ModConfigSpec.IntValue ROBOT_CHARGER_CHARGE_RATE_PER_TICK;

    // Energy Cells
    public static final ModConfigSpec.IntValue ENERGY_CELL_BASIC_CAPACITY;
    public static final ModConfigSpec.IntValue ENERGY_CELL_BASIC_TRANSFER;
    public static final ModConfigSpec.IntValue ENERGY_CELL_ADVANCED_CAPACITY;
    public static final ModConfigSpec.IntValue ENERGY_CELL_ADVANCED_TRANSFER;
    public static final ModConfigSpec.IntValue ENERGY_CELL_ULTIMATE_CAPACITY;
    public static final ModConfigSpec.IntValue ENERGY_CELL_ULTIMATE_TRANSFER;
    public static final ModConfigSpec.IntValue ENERGY_CELL_ULTRA_CAPACITY;
    public static final ModConfigSpec.IntValue ENERGY_CELL_ULTRA_TRANSFER;

    // Conduits (Energy Cables & Fluid Pipes)
    public static final ModConfigSpec.IntValue CONDUIT_MAX_NETWORK_SCAN;
    public static final ModConfigSpec.IntValue FLUID_PIPE_EMPTY_UNLOCK_TICKS;

    public static final ModConfigSpec.IntValue CABLE_BASIC_CAPACITY;
    public static final ModConfigSpec.IntValue CABLE_BASIC_TRANSFER;
    public static final ModConfigSpec.IntValue CABLE_ADVANCED_CAPACITY;
    public static final ModConfigSpec.IntValue CABLE_ADVANCED_TRANSFER;
    public static final ModConfigSpec.IntValue CABLE_ULTIMATE_CAPACITY;
    public static final ModConfigSpec.IntValue CABLE_ULTIMATE_TRANSFER;
    public static final ModConfigSpec.IntValue CABLE_ULTRA_CAPACITY;
    public static final ModConfigSpec.IntValue CABLE_ULTRA_TRANSFER;

    public static final ModConfigSpec.IntValue FLUID_PIPE_BASIC_CAPACITY;
    public static final ModConfigSpec.IntValue FLUID_PIPE_BASIC_TRANSFER;
    public static final ModConfigSpec.DoubleValue FLUID_PIPE_BASIC_RETENTION;
    public static final ModConfigSpec.IntValue FLUID_PIPE_ADVANCED_CAPACITY;
    public static final ModConfigSpec.IntValue FLUID_PIPE_ADVANCED_TRANSFER;
    public static final ModConfigSpec.DoubleValue FLUID_PIPE_ADVANCED_RETENTION;
    public static final ModConfigSpec.IntValue FLUID_PIPE_ULTIMATE_CAPACITY;
    public static final ModConfigSpec.IntValue FLUID_PIPE_ULTIMATE_TRANSFER;
    public static final ModConfigSpec.DoubleValue FLUID_PIPE_ULTIMATE_RETENTION;
    public static final ModConfigSpec.IntValue FLUID_PIPE_ULTRA_CAPACITY;
    public static final ModConfigSpec.IntValue FLUID_PIPE_ULTRA_TRANSFER;
    public static final ModConfigSpec.DoubleValue FLUID_PIPE_ULTRA_RETENTION;

    public static final ModConfigSpec.IntValue THERMAL_PIPE_ISOLATED_CAPACITY;
    public static final ModConfigSpec.IntValue THERMAL_PIPE_ISOLATED_TRANSFER;
    public static final ModConfigSpec.DoubleValue THERMAL_PIPE_ISOLATED_RETENTION;
    public static final ModConfigSpec.IntValue THERMAL_PIPE_REINFORCED_CAPACITY;
    public static final ModConfigSpec.IntValue THERMAL_PIPE_REINFORCED_TRANSFER;
    public static final ModConfigSpec.DoubleValue THERMAL_PIPE_REINFORCED_RETENTION;
    public static final ModConfigSpec.IntValue THERMAL_PIPE_VACUUM_CAPACITY;
    public static final ModConfigSpec.IntValue THERMAL_PIPE_VACUUM_TRANSFER;
    public static final ModConfigSpec.DoubleValue THERMAL_PIPE_VACUUM_RETENTION;
    public static final ModConfigSpec.IntValue THERMAL_PIPE_CRYOGENIC_CAPACITY;
    public static final ModConfigSpec.IntValue THERMAL_PIPE_CRYOGENIC_TRANSFER;
    public static final ModConfigSpec.DoubleValue THERMAL_PIPE_CRYOGENIC_RETENTION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Enrichment Chamber settings").push("enrichment_chamber");
        ENRICHMENT_ENERGY_CAPACITY = builder
                .comment("Internal energy buffer capacity in FE.")
                .defineInRange("energyCapacity", 50_000, 1_000, 100_000_000);
        ENRICHMENT_MAX_RECEIVE = builder
                .comment("Maximum FE received per tick.")
                .defineInRange("maxReceive", 2_000, 1, 10_000_000);
        ENRICHMENT_DEFAULT_PROCESS_TIME = builder
                .comment("Fallback GUI process time in ticks when no recipe matches.")
                .defineInRange("defaultProcessTime", 100, 1, 72_000);
        ENRICHMENT_DEFAULT_QUAD_PROCESS_TIME = builder
                .comment("Fallback GUI process time in ticks for Quad Enrichment Chamber when no recipe matches.")
                .defineInRange("defaultQuadProcessTime", 50, 1, 72_000);
        ENRICHMENT_ENERGY_MULTIPLIER = builder
                .comment("Multiplier applied to recipe FE/tick cost for Enrichment Chamber operations.")
                .defineInRange("energyMultiplier", 1.0D, 0.0D, 100.0D);
        ENRICHMENT_TIME_MULTIPLIER = builder
                .comment("Multiplier applied to recipe processing time (ticks) for Enrichment Chamber operations.")
                .defineInRange("timeMultiplier", 1.0D, 0.05D, 100.0D);
        builder.pop();

        builder.comment("Oil Refinery settings").push("oil_refinery");
        OIL_REFINERY_ENERGY_CAPACITY = builder
                .comment("Internal energy buffer capacity in FE.")
                .defineInRange("energyCapacity", 50_000, 1_000, 100_000_000);
        OIL_REFINERY_MAX_RECEIVE = builder
                .comment("Maximum FE received per tick.")
                .defineInRange("maxReceive", 2_000, 1, 10_000_000);
        OIL_REFINERY_TANK_CAPACITY = builder
                .comment("Capacity of input and output fluid tanks in mB.")
                .defineInRange("tankCapacity", 16_000, 1_000, 10_000_000);
        OIL_REFINERY_AUTO_PUSH_RATE = builder
                .comment("Maximum mB pushed per tick from the output tank to adjacent handlers.")
                .defineInRange("autoPushRate", 100, 1, 100_000);
        OIL_REFINERY_DEFAULT_PROCESS_TIME = builder
                .comment("Fallback GUI process time in ticks when no recipe matches.")
                .defineInRange("defaultProcessTime", 25, 1, 72_000);
        OIL_REFINERY_ENERGY_MULTIPLIER = builder
                .comment("Multiplier applied to recipe FE/tick cost for Oil Refinery operations.")
                .defineInRange("energyMultiplier", 1.0D, 0.0D, 100.0D);
        OIL_REFINERY_TIME_MULTIPLIER = builder
                .comment("Multiplier applied to recipe processing time (ticks) for Oil Refinery operations.")
                .defineInRange("timeMultiplier", 1.0D, 0.05D, 100.0D);
        builder.pop();

        builder.comment("Stamping Press settings").push("stamping_press");
        STAMPING_PRESS_ENERGY_CAPACITY = builder
                .comment("Internal energy buffer capacity in FE.")
                .defineInRange("energyCapacity", 100_000, 1_000, 100_000_000);
        STAMPING_PRESS_MAX_RECEIVE = builder
                .comment("Maximum FE received per tick.")
                .defineInRange("maxReceive", 2_000, 1, 10_000_000);
        STAMPING_PRESS_DEFAULT_PROCESS_TICKS = builder
                .comment("Fallback GUI process time in ticks when no recipe matches.")
                .defineInRange("defaultProcessTicks", 80, 1, 72_000);
        STAMPING_PRESS_ENERGY_MULTIPLIER = builder
                .comment("Multiplier applied to recipe FE/tick cost for Stamping Press operations.")
                .defineInRange("energyMultiplier", 1.0D, 0.0D, 100.0D);
        STAMPING_PRESS_TIME_MULTIPLIER = builder
                .comment("Multiplier applied to recipe processing time (ticks) for Stamping Press operations.")
                .defineInRange("timeMultiplier", 1.0D, 0.05D, 100.0D);
        builder.pop();

        builder.comment("PCB Printer settings").push("pcb_printer");
        PCB_PRINTER_ENERGY_CAPACITY = builder
                .comment("Internal energy buffer capacity in FE.")
                .defineInRange("energyCapacity", 100, 100, 100_000_000);
        PCB_PRINTER_MAX_RECEIVE = builder
                .comment("Maximum FE received per tick.")
                .defineInRange("maxReceive", 2_000, 1, 10_000_000);
        PCB_PRINTER_DEFAULT_PRINT_TICKS = builder
                .comment("Fallback GUI print time in ticks when no printing recipe is pending.")
                .defineInRange("defaultPrintTicks", 100, 1, 72_000);
        PCB_PRINTER_ENERGY_MULTIPLIER = builder
                .comment("Multiplier applied to recipe FE/tick cost for PCB Printer operations.")
                .defineInRange("energyMultiplier", 1.0D, 0.0D, 100.0D);
        PCB_PRINTER_TIME_MULTIPLIER = builder
                .comment("Multiplier applied to recipe print time (ticks) for PCB Printer operations.")
                .defineInRange("timeMultiplier", 1.0D, 0.05D, 100.0D);
        builder.pop();

        builder.comment("Geothermal Generator settings").push("geothermal_generator");
        GEOTHERMAL_ENERGY_CAPACITY = builder
                .comment("Internal energy buffer capacity in FE.")
                .defineInRange("energyCapacity", 200_000, 1_000, 100_000_000);
        GEOTHERMAL_MAX_EXTRACT = builder
                .comment("Maximum FE pushed/extracted per tick.")
                .defineInRange("maxExtract", 2_000, 1, 10_000_000);
        GEOTHERMAL_TANK_CAPACITY = builder
                .comment("Capacity of input and exhaust gas tanks in mB.")
                .defineInRange("tankCapacity", 16_000, 1_000, 10_000_000);
        GEOTHERMAL_MAX_PROCESS_MB_PER_TICK = builder
                .comment("Maximum mB of Gaseous Isobutane processed per tick.")
                .defineInRange("maxProcessMbPerTick", 50, 1, 100_000);
        GEOTHERMAL_FE_PER_MB = builder
                .comment("FE generated per mB of Gaseous Isobutane.")
                .defineInRange("fePerMb", 20, 1, 100_000);
        builder.pop();

        builder.comment("Solar Panel settings").push("solar_panel");
        SOLAR_ENERGY_CAPACITY = builder
                .comment("Internal energy buffer capacity in FE.")
                .defineInRange("energyCapacity", 150_000, 1_000, 100_000_000);
        SOLAR_MAX_EXTRACT = builder
                .comment("Maximum FE pushed/extracted per tick.")
                .defineInRange("maxExtract", 1_000, 1, 10_000_000);
        SOLAR_FE_PER_TICK_CLEAR = builder
                .comment("Basic Solar Panel FE generated per tick in clear daylight.")
                .defineInRange("fePerTickClear", 60, 0, 100_000);
        SOLAR_FE_PER_TICK_RAIN = builder
                .comment("Basic Solar Panel FE generated per tick during rain.")
                .defineInRange("fePerTickRain", 15, 0, 100_000);
        SOLAR_ADV_FE_PER_TICK_CLEAR = builder
                .comment("Advanced Solar Panel FE generated per tick in clear daylight.")
                .defineInRange("advancedFePerTickClear", 150, 0, 100_000);
        SOLAR_ADV_FE_PER_TICK_RAIN = builder
                .comment("Advanced Solar Panel FE generated per tick during rain.")
                .defineInRange("advancedFePerTickRain", 40, 0, 100_000);
        builder.pop();

        builder.comment("Condenser settings").push("condenser");
        CONDENSER_TANK_CAPACITY = builder
                .comment("Capacity of gas, water, and liquid tanks in mB.")
                .defineInRange("tankCapacity", 16_000, 1_000, 10_000_000);
        CONDENSER_MAX_CONDENSE_PER_TICK = builder
                .comment("Maximum mB of Gaseous Isobutane condensed per tick.")
                .defineInRange("maxCondensePerTick", 50, 1, 100_000);
        CONDENSER_MAX_PUSH_PER_TICK = builder
                .comment("Maximum mB of Liquid Isobutane pushed to adjacent handlers per tick.")
                .defineInRange("maxPushPerTick", 200, 1, 100_000);
        CONDENSER_GAS_PER_WATER_MB = builder
                .comment("mB of Gaseous Isobutane condensed per 1 mB of cooling water.")
                .defineInRange("gasPerWaterMb", 5, 1, 1_000);
        builder.pop();

        builder.comment("Fluid Pump settings").push("fluid_pump");
        FLUID_PUMP_TANK_CAPACITY = builder
                .comment("Internal output tank capacity in mB.")
                .defineInRange("tankCapacity", 16_000, 1_000, 10_000_000);
        FLUID_PUMP_ENERGY_CAPACITY = builder
                .comment("Internal energy buffer capacity in FE.")
                .defineInRange("energyCapacity", 100_000, 1_000, 100_000_000);
        FLUID_PUMP_MAX_RECEIVE = builder
                .comment("Maximum FE received per tick.")
                .defineInRange("maxReceive", 1_000, 1, 10_000_000);
        FLUID_PUMP_ENERGY_PER_PUMP = builder
                .comment("FE consumed per pump operation.")
                .defineInRange("energyPerPump", 200, 0, 1_000_000);
        FLUID_PUMP_INTERVAL = builder
                .comment("Ticks between pump operations.")
                .defineInRange("pumpInterval", 10, 1, 1_200);
        FLUID_PUMP_MAX_SCAN_DEPTH = builder
                .comment("Maximum downward scan depth in blocks.")
                .defineInRange("maxScanDepth", 64, 1, 256);
        FLUID_PUMP_HORIZONTAL_RADIUS = builder
                .comment("Horizontal pool scan radius in blocks.")
                .defineInRange("horizontalRadius", 8, 1, 32);
        FLUID_PUMP_MAX_TRACE_NODES = builder
                .comment("Maximum fluid nodes visited per upstream trace.")
                .defineInRange("maxTraceNodes", 512, 16, 4_096);
        FLUID_PUMP_AUTO_PUSH_RATE = builder
                .comment("Maximum mB pushed per tick to adjacent fluid handlers.")
                .defineInRange("autoPushRate", 500, 1, 100_000);
        builder.pop();

        builder.comment("Reactor Core settings").push("reactor_core");
        REACTOR_ENERGY_CAPACITY = builder
                .comment("Internal energy buffer capacity in FE.")
                .defineInRange("energyCapacity", 1_000_000, 1_000, 1_000_000_000);
        REACTOR_MAX_EXTRACT = builder
                .comment("Maximum FE pushed/extracted per tick.")
                .defineInRange("maxExtract", 10_000, 1, 10_000_000);
        REACTOR_FE_PER_TICK = builder
                .comment("FE generated per tick while burning fuel.")
                .defineInRange("fePerTick", 512, 1, 1_000_000);
        REACTOR_FUEL_BURN_TIME = builder
                .comment("Burn duration in ticks per unit of fuel.")
                .defineInRange("fuelBurnTime", 1_200, 1, 720_000);
        REACTOR_MAX_HEAT = builder
                .comment("Maximum core heat level.")
                .defineInRange("maxHeat", 1_000, 100, 100_000);
        builder.pop();

        builder.comment("Heat Exchanger multiblock settings").push("heat_exchanger");
        HEAT_EXCHANGER_MAX_SPAN = builder
                .comment("Maximum multiblock span along any axis in blocks.")
                .defineInRange("maxSpan", 20, 1, 64);
        HEAT_EXCHANGER_ISOBUTANE_TANK_CAPACITY = builder
                .comment("Isobutane tank capacity per block in mB.")
                .defineInRange("isobutaneTankCapacity", 16_000, 1_000, 10_000_000);
        HEAT_EXCHANGER_WATER_TANK_CAPACITY = builder
                .comment("Water tank capacity per block in mB.")
                .defineInRange("waterTankCapacity", 8_000, 1_000, 10_000_000);
        HEAT_EXCHANGER_MIN_BOIL_RATE = builder
                .comment("Minimum boil rate per block in mB/t.")
                .defineInRange("minBoilRate", 5, 1, 10_000);
        HEAT_EXCHANGER_MAX_BOIL_RATE = builder
                .comment("Maximum boil rate per block in mB/t.")
                .defineInRange("maxBoilRate", 50, 1, 100_000);
        HEAT_EXCHANGER_MAX_PUSH_RATE = builder
                .comment("Maximum gas push rate per block in mB/t.")
                .defineInRange("maxPushRate", 200, 1, 100_000);
        builder.pop();

        builder.comment("Multiblock Tank settings").push("tank");
        TANK_CAPACITY_PER_BLOCK = builder
                .comment("Fluid storage capacity per tank block in mB.")
                .defineInRange("capacityPerBlock", 200_000, 1_000, 10_000_000);
        TANK_MAX_SPAN = builder
                .comment("Maximum multiblock span along any axis in blocks.")
                .defineInRange("maxSpan", 20, 1, 64);
        builder.pop();

        builder.comment("Robot Charger settings").push("robot_charger");
        ROBOT_CHARGER_ENERGY_CAPACITY = builder
                .comment("Internal energy buffer capacity in FE.")
                .defineInRange("energyCapacity", 500_000, 1_000, 100_000_000);
        ROBOT_CHARGER_MAX_RECEIVE = builder
                .comment("Maximum FE received per tick.")
                .defineInRange("maxReceive", 5_000, 1, 10_000_000);
        ROBOT_CHARGER_CHARGE_RATE_PER_TICK = builder
                .comment("Maximum FE transferred to a robot per tick.")
                .defineInRange("chargeRatePerTick", 5_000, 1, 10_000_000);
        builder.pop();

        builder.comment("Energy Cell tier settings").push("energy_cells");
        ENERGY_CELL_BASIC_CAPACITY = builder.defineInRange("basicCapacity", 1_000_000, 1_000, 1_000_000_000);
        ENERGY_CELL_BASIC_TRANSFER = builder.defineInRange("basicTransferRate", 2_000, 1, 100_000_000);
        ENERGY_CELL_ADVANCED_CAPACITY = builder.defineInRange("advancedCapacity", 4_000_000, 1_000, 1_000_000_000);
        ENERGY_CELL_ADVANCED_TRANSFER = builder.defineInRange("advancedTransferRate", 8_000, 1, 100_000_000);
        ENERGY_CELL_ULTIMATE_CAPACITY = builder.defineInRange("ultimateCapacity", 16_000_000, 1_000, 1_000_000_000);
        ENERGY_CELL_ULTIMATE_TRANSFER = builder.defineInRange("ultimateTransferRate", 32_000, 1, 100_000_000);
        ENERGY_CELL_ULTRA_CAPACITY = builder.defineInRange("ultraCapacity", 64_000_000, 1_000, 1_000_000_000);
        ENERGY_CELL_ULTRA_TRANSFER = builder.defineInRange("ultraTransferRate", 128_000, 1, 100_000_000);
        builder.pop();

        builder.comment("Energy Cable and Fluid Pipe conduit settings").push("conduits");
        CONDUIT_MAX_NETWORK_SCAN = builder
                .comment("Maximum conduit blocks scanned per network BFS.")
                .defineInRange("maxNetworkScan", 256, 16, 4_096);
        FLUID_PIPE_EMPTY_UNLOCK_TICKS = builder
                .comment("Ticks an empty fluid pipe waits before unlocking its fluid filter.")
                .defineInRange("fluidPipeEmptyUnlockTicks", 40, 1, 1_200);

        CABLE_BASIC_CAPACITY = builder.defineInRange("cableBasicCapacity", 4_000, 100, 100_000_000);
        CABLE_BASIC_TRANSFER = builder.defineInRange("cableBasicTransferRate", 2_000, 1, 100_000_000);
        CABLE_ADVANCED_CAPACITY = builder.defineInRange("cableAdvancedCapacity", 16_000, 100, 100_000_000);
        CABLE_ADVANCED_TRANSFER = builder.defineInRange("cableAdvancedTransferRate", 8_000, 1, 100_000_000);
        CABLE_ULTIMATE_CAPACITY = builder.defineInRange("cableUltimateCapacity", 64_000, 100, 100_000_000);
        CABLE_ULTIMATE_TRANSFER = builder.defineInRange("cableUltimateTransferRate", 32_000, 1, 100_000_000);
        CABLE_ULTRA_CAPACITY = builder.defineInRange("cableUltraCapacity", 256_000, 100, 100_000_000);
        CABLE_ULTRA_TRANSFER = builder.defineInRange("cableUltraTransferRate", 128_000, 1, 100_000_000);

        FLUID_PIPE_BASIC_CAPACITY = builder.defineInRange("fluidPipeBasicCapacity", 1_000, 100, 10_000_000);
        FLUID_PIPE_BASIC_TRANSFER = builder.defineInRange("fluidPipeBasicTransferRate", 200, 1, 10_000_000);
        FLUID_PIPE_BASIC_RETENTION = builder.defineInRange("fluidPipeBasicHeatRetention", 0.9600D, 0.0D, 1.0D);
        FLUID_PIPE_ADVANCED_CAPACITY = builder.defineInRange("fluidPipeAdvancedCapacity", 4_000, 100, 10_000_000);
        FLUID_PIPE_ADVANCED_TRANSFER = builder.defineInRange("fluidPipeAdvancedTransferRate", 1_000, 1, 10_000_000);
        FLUID_PIPE_ADVANCED_RETENTION = builder.defineInRange("fluidPipeAdvancedHeatRetention", 0.9700D, 0.0D, 1.0D);
        FLUID_PIPE_ULTIMATE_CAPACITY = builder.defineInRange("fluidPipeUltimateCapacity", 16_000, 100, 10_000_000);
        FLUID_PIPE_ULTIMATE_TRANSFER = builder.defineInRange("fluidPipeUltimateTransferRate", 4_000, 1, 10_000_000);
        FLUID_PIPE_ULTIMATE_RETENTION = builder.defineInRange("fluidPipeUltimateHeatRetention", 0.9800D, 0.0D, 1.0D);
        FLUID_PIPE_ULTRA_CAPACITY = builder.defineInRange("fluidPipeUltraCapacity", 64_000, 100, 10_000_000);
        FLUID_PIPE_ULTRA_TRANSFER = builder.defineInRange("fluidPipeUltraTransferRate", 16_000, 1, 10_000_000);
        FLUID_PIPE_ULTRA_RETENTION = builder.defineInRange("fluidPipeUltraHeatRetention", 0.9850D, 0.0D, 1.0D);

        THERMAL_PIPE_ISOLATED_CAPACITY = builder.defineInRange("thermalPipeIsolatedCapacity", 2_000, 100, 10_000_000);
        THERMAL_PIPE_ISOLATED_TRANSFER = builder.defineInRange("thermalPipeIsolatedTransferRate", 500, 1, 10_000_000);
        THERMAL_PIPE_ISOLATED_RETENTION = builder.defineInRange("thermalPipeIsolatedHeatRetention", 0.9950D, 0.0D, 1.0D);
        THERMAL_PIPE_REINFORCED_CAPACITY = builder.defineInRange("thermalPipeReinforcedCapacity", 8_000, 100, 10_000_000);
        THERMAL_PIPE_REINFORCED_TRANSFER = builder.defineInRange("thermalPipeReinforcedTransferRate", 2_000, 1, 10_000_000);
        THERMAL_PIPE_REINFORCED_RETENTION = builder.defineInRange("thermalPipeReinforcedHeatRetention", 0.9975D, 0.0D, 1.0D);
        THERMAL_PIPE_VACUUM_CAPACITY = builder.defineInRange("thermalPipeVacuumCapacity", 32_000, 100, 10_000_000);
        THERMAL_PIPE_VACUUM_TRANSFER = builder.defineInRange("thermalPipeVacuumTransferRate", 8_000, 1, 10_000_000);
        THERMAL_PIPE_VACUUM_RETENTION = builder.defineInRange("thermalPipeVacuumHeatRetention", 0.9990D, 0.0D, 1.0D);
        THERMAL_PIPE_CRYOGENIC_CAPACITY = builder.defineInRange("thermalPipeCryogenicCapacity", 128_000, 100, 10_000_000);
        THERMAL_PIPE_CRYOGENIC_TRANSFER = builder.defineInRange("thermalPipeCryogenicTransferRate", 32_000, 1, 10_000_000);
        THERMAL_PIPE_CRYOGENIC_RETENTION = builder.defineInRange("thermalPipeCryogenicHeatRetention", 1.0D, 0.0D, 1.0D);
        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Safely reads an integer config option, falling back to its default before the config is loaded.
     */
    public static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /**
     * Safely reads a double config option, falling back to its default before the config is loaded.
     */
    public static double get(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /**
     * Scales a base FE/tick cost by a configured multiplier.
     */
    public static int scaleEnergy(int baseEnergyPerTick, ModConfigSpec.DoubleValue multiplier) {
        return Math.max(0, (int) Math.round(baseEnergyPerTick * get(multiplier)));
    }

    /**
     * Scales a base duration in ticks by a configured multiplier (minimum 1 tick).
     */
    public static int scaleTicks(int baseTicks, ModConfigSpec.DoubleValue multiplier) {
        return Math.max(1, (int) Math.round(baseTicks * get(multiplier)));
    }

    private ModServerConfig() {}
}
