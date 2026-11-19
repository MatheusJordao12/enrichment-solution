package com.enrichmentsolution.mod.registry;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.item.GuideBookItem;
import com.enrichmentsolution.mod.common.item.HiTechGogglesItem;
import com.enrichmentsolution.mod.common.item.RobotItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnrichmentSolutionMod.MOD_ID);

    public static final DeferredItem<Item> RAW_URANIUM = ITEMS.register("raw_uranium",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> URANIUM_INGOT = ITEMS.register("uranium_ingot",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ENRICHED_URANIUM = ITEMS.register("enriched_uranium",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SOLAR_UPGRADE_TEMPLATE = ITEMS.register("solar_upgrade_template",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> AUTO_RECHARGER_UPGRADE_TEMPLATE = ITEMS.register("auto_recharger_upgrade_template",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SLOT_UPGRADE_TIER_1 = ITEMS.register("slot_upgrade_tier_1",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SLOT_UPGRADE_TIER_2 = ITEMS.register("slot_upgrade_tier_2",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SLOT_UPGRADE_TIER_3 = ITEMS.register("slot_upgrade_tier_3",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROBOT_CIRCUIT = ITEMS.register("robot_circuit",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> BASIC_CIRCUIT = ITEMS.register("basic_circuit",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ADVANCED_CIRCUIT = ITEMS.register("advanced_circuit",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ULTIMATE_CIRCUIT = ITEMS.register("ultimate_circuit",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ULTRA_CIRCUIT = ITEMS.register("ultra_circuit",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> HI_TECH_GOGGLES = ITEMS.register("hi_tech_goggles",
            () -> new HiTechGogglesItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> WHEEL = ITEMS.register("wheel",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROBOT_CASING = ITEMS.register("robot_casing",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PRESSED_ALUMINIUM = ITEMS.register("pressed_aluminium",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MECHANICAL_EYES = ITEMS.register("mechanical_eyes",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RAW_STEEL = ITEMS.register("raw_steel",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.register("steel_ingot",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RAW_BAUXITE = ITEMS.register("raw_bauxite",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> BAUXITE_INGOT = ITEMS.register("bauxite_ingot",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RAW_ALUMINIUM = ITEMS.register("raw_aluminium",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ALUMINIUM_INGOT = ITEMS.register("aluminium_ingot",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RAW_METEOROTITE = ITEMS.register("raw_meteorotite",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> METEOROTITE_INGOT = ITEMS.register("meteorotite_ingot",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RAW_UNSTABLE_OLD_URANIUM = ITEMS.register("raw_unstable_old_uranium",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> UNSTABLE_OLD_URANIUM_INGOT = ITEMS.register("unstable_old_uranium_ingot",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RAW_OXIDATED_PLUTONIUM = ITEMS.register("raw_oxidated_plutonium",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> OXIDATED_PLUTONIUM_INGOT = ITEMS.register("oxidated_plutonium_ingot",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROBOT = ITEMS.register("robot",
            () -> new RobotItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CRUDE_OIL_BUCKET = ITEMS.register("crude_oil_bucket",
            () -> new BucketItem(ModFluids.CRUDE_OIL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<Item> LIQUID_ISOBUTANE_BUCKET = ITEMS.register("liquid_isobutane_bucket",
            () -> new BucketItem(ModFluids.LIQUID_ISOBUTANE.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<Item> CONFIGURATOR = ITEMS.register("configurator",
            () -> new com.enrichmentsolution.mod.common.item.ConfiguratorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> PLUTONIUM_EXTRACTOR = ITEMS.register("plutonium_extractor",
            () -> new com.enrichmentsolution.mod.common.item.PlutoniumExtractorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> ISOLATED_BACKPACK = ITEMS.register("isolated_backpack",
            () -> new com.enrichmentsolution.mod.common.item.IsolatedBackpackItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> HAMMER_X = ITEMS.register("hammer_x",
            () -> new com.enrichmentsolution.mod.common.item.HammerXItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> ENRICHMENT_GUIDE = ITEMS.register("enrichment_guide",
            () -> new GuideBookItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private ModItems() {}
}
