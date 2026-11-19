package com.enrichmentsolution.mod.datagen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.registry.ModBlocks;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Generates the 13 progression advancements for Enrichment Solution.
 */
public class ModAdvancementProvider implements AdvancementProvider.AdvancementGenerator {

    @Override
    public void generate(
            HolderLookup.Provider registries,
            Consumer<AdvancementHolder> saver,
            ExistingFileHelper existingFileHelper
    ) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(
                        ModBlocks.MACHINE_CASING.get(),
                        title("root"),
                        description("root"),
                        ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "textures/block/machine_casing.png"),
                        AdvancementType.TASK,
                        true,
                        false,
                        false
                )
                .addCriterion("get", InventoryChangeTrigger.TriggerInstance.hasItems(ModBlocks.MACHINE_CASING.get()))
                .save(saver, id("root"), existingFileHelper);

        AdvancementHolder enrichmentChamber = itemAdvancement(
                "enrichment_chamber",
                ModBlocks.ENRICHMENT_CHAMBER.get(),
                root,
                AdvancementType.TASK,
                saver,
                existingFileHelper
        );

        AdvancementHolder enrichedUranium = itemAdvancement(
                "enriched_uranium",
                ModItems.ENRICHED_URANIUM.get(),
                enrichmentChamber,
                AdvancementType.TASK,
                saver,
                existingFileHelper
        );

        itemAdvancement(
                "reactor_core",
                ModBlocks.REACTOR_CORE.get(),
                enrichedUranium,
                AdvancementType.GOAL,
                saver,
                existingFileHelper
        );

        AdvancementHolder pcbPrinter = itemAdvancement(
                "pcb_printer",
                ModBlocks.PCB_PRINTER.get(),
                enrichmentChamber,
                AdvancementType.TASK,
                saver,
                existingFileHelper
        );

        AdvancementHolder firstCircuit = itemAdvancement(
                "first_circuit",
                ModItems.BASIC_CIRCUIT.get(),
                pcbPrinter,
                AdvancementType.TASK,
                saver,
                existingFileHelper
        );

        itemAdvancement(
                "hi_tech_goggles",
                ModItems.HI_TECH_GOGGLES.get(),
                firstCircuit,
                AdvancementType.TASK,
                saver,
                existingFileHelper
        );

        AdvancementHolder advancedCircuit = itemAdvancement(
                "advanced_circuit",
                ModItems.ADVANCED_CIRCUIT.get(),
                firstCircuit,
                AdvancementType.TASK,
                saver,
                existingFileHelper
        );

        AdvancementHolder ultimateCircuit = itemAdvancement(
                "ultimate_circuit",
                ModItems.ULTIMATE_CIRCUIT.get(),
                advancedCircuit,
                AdvancementType.TASK,
                saver,
                existingFileHelper
        );

        itemAdvancement(
                "ultra_circuit",
                ModItems.ULTRA_CIRCUIT.get(),
                ultimateCircuit,
                AdvancementType.GOAL,
                saver,
                existingFileHelper
        );

        AdvancementHolder oilRefinery = itemAdvancement(
                "oil_refinery",
                ModBlocks.OIL_REFINERY.get(),
                root,
                AdvancementType.TASK,
                saver,
                existingFileHelper
        );

        AdvancementHolder heatExchanger = itemAdvancement(
                "heat_exchanger",
                ModBlocks.HEAT_EXCHANGER.get(),
                oilRefinery,
                AdvancementType.TASK,
                saver,
                existingFileHelper
        );

        itemAdvancement(
                "geothermal_generator",
                ModBlocks.GEOTHERMAL_GENERATOR.get(),
                heatExchanger,
                AdvancementType.TASK,
                saver,
                existingFileHelper
        );
    }

    private static AdvancementHolder itemAdvancement(
            String name,
            ItemLike iconAndTrigger,
            @Nullable AdvancementHolder parent,
            AdvancementType type,
            Consumer<AdvancementHolder> saver,
            ExistingFileHelper existingFileHelper
    ) {
        Advancement.Builder builder = Advancement.Builder.advancement();
        if (parent != null) {
            builder.parent(parent);
        }
        return builder
                .display(
                        iconAndTrigger,
                        title(name),
                        description(name),
                        null,
                        type,
                        true,
                        true,
                        false
                )
                .addCriterion("get", InventoryChangeTrigger.TriggerInstance.hasItems(iconAndTrigger))
                .save(saver, id(name), existingFileHelper);
    }

    private static Component title(String name) {
        return Component.translatable("advancement." + EnrichmentSolutionMod.MOD_ID + "." + name + ".title");
    }

    private static Component description(String name) {
        return Component.translatable("advancement." + EnrichmentSolutionMod.MOD_ID + "." + name + ".description");
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, name);
    }
}
