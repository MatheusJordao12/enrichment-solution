package com.enrichmentsolution.mod.registry;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, EnrichmentSolutionMod.MOD_ID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, EnrichmentSolutionMod.MOD_ID);

    // FluidTypes
    public static final DeferredHolder<FluidType, FluidType> CRUDE_OIL_TYPE = FLUID_TYPES.register("crude_oil",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.enrichment_solution.crude_oil")
                    .density(1200)
                    .viscosity(4000)
                    .temperature(300)
                    .canSwim(true)
                    .canDrown(true)
                    .supportsBoating(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<FluidType, FluidType> LIQUID_ISOBUTANE_TYPE = FLUID_TYPES.register("liquid_isobutane",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.enrichment_solution.liquid_isobutane")
                    .density(560)
                    .viscosity(400)
                    .temperature(260)
                    .canSwim(true)
                    .canDrown(true)
                    .supportsBoating(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<FluidType, FluidType> GASEOUS_ISOBUTANE_TYPE = FLUID_TYPES.register("gaseous_isobutane",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.enrichment_solution.gaseous_isobutane")
                    .density(-500)
                    .viscosity(200)
                    .temperature(310)
                    .canSwim(false)
                    .canDrown(false)));

    // Fluids (Source & Flowing)
    public static final DeferredHolder<Fluid, BaseFlowingFluid> CRUDE_OIL = FLUIDS.register("crude_oil",
            () -> new BaseFlowingFluid.Source(crudeOilProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid> FLOWING_CRUDE_OIL = FLUIDS.register("flowing_crude_oil",
            () -> new BaseFlowingFluid.Flowing(crudeOilProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid> LIQUID_ISOBUTANE = FLUIDS.register("liquid_isobutane",
            () -> new BaseFlowingFluid.Source(liquidIsobutaneProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid> FLOWING_LIQUID_ISOBUTANE = FLUIDS.register("flowing_liquid_isobutane",
            () -> new BaseFlowingFluid.Flowing(liquidIsobutaneProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid> GASEOUS_ISOBUTANE = FLUIDS.register("gaseous_isobutane",
            () -> new BaseFlowingFluid.Source(gaseousIsobutaneProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid> FLOWING_GASEOUS_ISOBUTANE = FLUIDS.register("flowing_gaseous_isobutane",
            () -> new BaseFlowingFluid.Flowing(gaseousIsobutaneProperties()));

    private static BaseFlowingFluid.Properties crudeOilProperties() {
        return new BaseFlowingFluid.Properties(CRUDE_OIL_TYPE, CRUDE_OIL, FLOWING_CRUDE_OIL)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(25)
                .explosionResistance(100.0F)
                .block(ModBlocks.CRUDE_OIL_BLOCK)
                .bucket(ModItems.CRUDE_OIL_BUCKET);
    }

    private static BaseFlowingFluid.Properties liquidIsobutaneProperties() {
        return new BaseFlowingFluid.Properties(LIQUID_ISOBUTANE_TYPE, LIQUID_ISOBUTANE, FLOWING_LIQUID_ISOBUTANE)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .explosionResistance(100.0F)
                .block(ModBlocks.LIQUID_ISOBUTANE_BLOCK)
                .bucket(ModItems.LIQUID_ISOBUTANE_BUCKET);
    }

    private static BaseFlowingFluid.Properties gaseousIsobutaneProperties() {
        return new BaseFlowingFluid.Properties(GASEOUS_ISOBUTANE_TYPE, GASEOUS_ISOBUTANE, FLOWING_GASEOUS_ISOBUTANE)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5);
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }

    private ModFluids() {}
}
