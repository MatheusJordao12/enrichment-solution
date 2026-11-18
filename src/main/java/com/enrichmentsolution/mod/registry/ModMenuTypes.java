package com.enrichmentsolution.mod.registry;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.menu.CondenserMenu;
import com.enrichmentsolution.mod.common.menu.EnergyCellMenu;
import com.enrichmentsolution.mod.common.menu.EnrichmentChamberMenu;
import com.enrichmentsolution.mod.common.menu.FluidPumpMenu;
import com.enrichmentsolution.mod.common.menu.GeothermalGeneratorMenu;
import com.enrichmentsolution.mod.common.menu.HeatExchangerMenu;
import com.enrichmentsolution.mod.common.menu.OilRefineryMenu;
import com.enrichmentsolution.mod.common.menu.PcbPrinterMenu;
import com.enrichmentsolution.mod.common.menu.QuadEnrichmentMenu;
import com.enrichmentsolution.mod.common.menu.RobotMenu;
import com.enrichmentsolution.mod.common.menu.SolarPanelMenu;
import com.enrichmentsolution.mod.common.menu.StampingPressMenu;
import com.enrichmentsolution.mod.common.menu.TankMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EnrichmentSolutionMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<EnrichmentChamberMenu>> ENRICHMENT_CHAMBER =
            MENUS.register("enrichment_chamber", () -> IMenuTypeExtension.create(EnrichmentChamberMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<QuadEnrichmentMenu>> QUAD_ENRICHMENT_CHAMBER =
            MENUS.register("quad_enrichment_chamber", () -> IMenuTypeExtension.create(QuadEnrichmentMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SolarPanelMenu>> SOLAR_PANEL =
            MENUS.register("solar_panel", () -> IMenuTypeExtension.create(SolarPanelMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RobotMenu>> ROBOT =
            MENUS.register("robot", () -> IMenuTypeExtension.create(RobotMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyCellMenu>> ENERGY_CELL =
            MENUS.register("energy_cell", () -> IMenuTypeExtension.create(EnergyCellMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PcbPrinterMenu>> PCB_PRINTER =
            MENUS.register("pcb_printer", () -> IMenuTypeExtension.create(PcbPrinterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StampingPressMenu>> STAMPING_PRESS =
            MENUS.register("stamping_press", () -> IMenuTypeExtension.create(StampingPressMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TankMenu>> TANK =
            MENUS.register("tank", () -> IMenuTypeExtension.create(TankMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<OilRefineryMenu>> OIL_REFINERY =
            MENUS.register("oil_refinery", () -> IMenuTypeExtension.create(OilRefineryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<HeatExchangerMenu>> HEAT_EXCHANGER =
            MENUS.register("heat_exchanger", () -> IMenuTypeExtension.create(HeatExchangerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<GeothermalGeneratorMenu>> GEOTHERMAL_GENERATOR =
            MENUS.register("geothermal_generator", () -> IMenuTypeExtension.create(GeothermalGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CondenserMenu>> CONDENSER =
            MENUS.register("condenser", () -> IMenuTypeExtension.create(CondenserMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidPumpMenu>> FLUID_PUMP =
            MENUS.register("fluid_pump", () -> IMenuTypeExtension.create(FluidPumpMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    private ModMenuTypes() {}
}
