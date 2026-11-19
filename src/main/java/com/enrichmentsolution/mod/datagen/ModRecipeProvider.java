package com.enrichmentsolution.mod.datagen;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.recipe.MachineItemInput;
import com.enrichmentsolution.mod.common.recipe.MachineRecipe;
import com.enrichmentsolution.mod.common.recipe.MachineRecipeKind;
import com.enrichmentsolution.mod.registry.ModBlocks;
import com.enrichmentsolution.mod.registry.ModFluids;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generates crafting, smelting, blasting, and {@link MachineRecipe} JSONs.
 */
public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        buildCraftingRecipes(output);
        buildSmeltingRecipes(output);
        buildMachineRecipes(output);
    }

    private void buildCraftingRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MACHINE_CASING.get())
                .pattern("SGS")
                .pattern("GIG")
                .pattern("SGS")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('G', Items.GLASS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ENRICHMENT_CHAMBER.get())
                .pattern("IRI")
                .pattern("SUS")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('S', ModItems.STEEL_INGOT.get())
                .define('U', ModItems.URANIUM_INGOT.get())
                .unlockedBy("has_uranium_ingot", has(ModItems.URANIUM_INGOT.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PCB_PRINTER.get())
                .pattern("SGS")
                .pattern("AMA")
                .pattern("RCR")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('G', Items.GLASS)
                .define('A', ModItems.ALUMINIUM_INGOT.get())
                .define('M', ModBlocks.MACHINE_CASING.get())
                .define('R', Items.REDSTONE)
                .define('C', ModBlocks.ENERGY_CABLE.get())
                .unlockedBy("has_machine_casing", has(ModBlocks.MACHINE_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.STAMPING_PRESS.get())
                .pattern("SPS")
                .pattern("AMA")
                .pattern("SCS")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('P', Items.PISTON)
                .define('A', ModItems.ALUMINIUM_INGOT.get())
                .define('M', ModBlocks.MACHINE_CASING.get())
                .define('C', ModBlocks.ENERGY_CABLE.get())
                .unlockedBy("has_machine_casing", has(ModBlocks.MACHINE_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.OIL_REFINERY.get())
                .pattern("SBS")
                .pattern("AMA")
                .pattern("SCS")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('B', Items.BUCKET)
                .define('A', ModItems.ALUMINIUM_INGOT.get())
                .define('M', ModBlocks.MACHINE_CASING.get())
                .define('C', ModBlocks.ENERGY_CABLE.get())
                .unlockedBy("has_machine_casing", has(ModBlocks.MACHINE_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DISTILLATION_TOWER.get())
                .pattern("SGS")
                .pattern("AGA")
                .pattern("SPS")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('G', Items.GLASS)
                .define('A', ModItems.ALUMINIUM_INGOT.get())
                .define('P', ModBlocks.BASIC_FLUID_PIPE.get())
                .unlockedBy("has_oil_refinery", has(ModBlocks.OIL_REFINERY.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.HEAT_EXCHANGER.get())
                .pattern("SCS")
                .pattern("GMG")
                .pattern("SCS")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GLASS)
                .define('M', ModBlocks.MACHINE_CASING.get())
                .unlockedBy("has_machine_casing", has(ModBlocks.MACHINE_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.GEOTHERMAL_GENERATOR.get())
                .pattern("SCS")
                .pattern("AMA")
                .pattern("RPR")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('C', Items.COPPER_INGOT)
                .define('A', ModItems.PRESSED_ALUMINIUM.get())
                .define('M', ModBlocks.MACHINE_CASING.get())
                .define('R', Items.REDSTONE)
                .define('P', ModBlocks.ENERGY_CABLE.get())
                .unlockedBy("has_machine_casing", has(ModBlocks.MACHINE_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CONDENSER.get())
                .pattern("SCS")
                .pattern("IMI")
                .pattern("SCS")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('C', Items.COPPER_INGOT)
                .define('I', Items.BLUE_ICE)
                .define('M', ModBlocks.MACHINE_CASING.get())
                .unlockedBy("has_machine_casing", has(ModBlocks.MACHINE_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FLUID_PUMP.get())
                .pattern("SBS")
                .pattern("AMA")
                .pattern("HCH")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('B', Items.BUCKET)
                .define('A', ModItems.ALUMINIUM_INGOT.get())
                .define('M', ModBlocks.MACHINE_CASING.get())
                .define('H', Items.HOPPER)
                .define('C', ModBlocks.ENERGY_CABLE.get())
                .unlockedBy("has_machine_casing", has(ModBlocks.MACHINE_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ROBOT_CHARGER.get())
                .pattern("ACA")
                .pattern("SMS")
                .pattern("RRR")
                .define('A', ModItems.ALUMINIUM_INGOT.get())
                .define('C', ModBlocks.ADVANCED_ENERGY_CABLE.get())
                .define('S', ModItems.STEEL_INGOT.get())
                .define('M', ModBlocks.MACHINE_CASING.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_machine_casing", has(ModBlocks.MACHINE_CASING.get()))
                .save(output);

        // Tanks
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.TANK_BLOCK.get(), 4)
                .pattern("SGS")
                .pattern("G G")
                .pattern("SGS")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('G', Items.GLASS)
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.TANK_WATER_INPUT.get())
                .pattern(" S ")
                .pattern("HTH")
                .pattern(" B ")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('H', Items.HOPPER)
                .define('T', ModBlocks.TANK_BLOCK.get())
                .define('B', Items.BUCKET)
                .unlockedBy("has_tank_block", has(ModBlocks.TANK_BLOCK.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.TANK_WATER_OUTPUT.get())
                .pattern(" B ")
                .pattern("PTP")
                .pattern(" S ")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('P', ModItems.PRESSED_ALUMINIUM.get())
                .define('T', ModBlocks.TANK_BLOCK.get())
                .define('B', Items.BUCKET)
                .unlockedBy("has_tank_block", has(ModBlocks.TANK_BLOCK.get()))
                .save(output);

        // Energy cables
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ENERGY_CABLE.get(), 6)
                .pattern("III")
                .pattern("RRR")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output);

        tierUpgrade8(output, ModBlocks.ADVANCED_ENERGY_CABLE.get(), ModBlocks.ENERGY_CABLE.get(), ModItems.ADVANCED_CIRCUIT.get());
        tierUpgrade8(output, ModBlocks.ULTIMATE_ENERGY_CABLE.get(), ModBlocks.ADVANCED_ENERGY_CABLE.get(), ModItems.ULTIMATE_CIRCUIT.get());
        tierUpgrade8(output, ModBlocks.ULTRA_ENERGY_CABLE.get(), ModBlocks.ULTIMATE_ENERGY_CABLE.get(), ModItems.ULTRA_CIRCUIT.get());

        // Fluid pipes
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BASIC_FLUID_PIPE.get(), 6)
                .pattern("CCC")
                .pattern("GGG")
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(output);

        tierUpgrade8(output, ModBlocks.ADVANCED_FLUID_PIPE.get(), ModBlocks.BASIC_FLUID_PIPE.get(), ModItems.ADVANCED_CIRCUIT.get());
        tierUpgrade8(output, ModBlocks.ULTIMATE_FLUID_PIPE.get(), ModBlocks.ADVANCED_FLUID_PIPE.get(), ModItems.ULTIMATE_CIRCUIT.get());
        tierUpgrade8(output, ModBlocks.ULTRA_FLUID_PIPE.get(), ModBlocks.ULTIMATE_FLUID_PIPE.get(), ModItems.ULTRA_CIRCUIT.get());

        // Thermal pipes
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ISOLATED_THERMAL_PIPE.get(), 8)
                .pattern("GWG")
                .pattern("PPP")
                .pattern("GWG")
                .define('G', Items.GLASS)
                .define('W', Items.WHITE_WOOL)
                .define('P', ModBlocks.BASIC_FLUID_PIPE.get())
                .unlockedBy("has_basic_fluid_pipe", has(ModBlocks.BASIC_FLUID_PIPE.get()))
                .save(output);

        tierUpgrade8(output, ModBlocks.REINFORCED_THERMAL_PIPE.get(), ModBlocks.ISOLATED_THERMAL_PIPE.get(), ModItems.ADVANCED_CIRCUIT.get());
        tierUpgrade8(output, ModBlocks.VACUUM_THERMAL_PIPE.get(), ModBlocks.REINFORCED_THERMAL_PIPE.get(), ModItems.ULTIMATE_CIRCUIT.get());
        tierUpgrade8(output, ModBlocks.CRYOGENIC_THERMAL_PIPE.get(), ModBlocks.VACUUM_THERMAL_PIPE.get(), ModItems.ULTRA_CIRCUIT.get());

        // Energy cells
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ENERGY_CELL.get())
                .pattern("SRS")
                .pattern("CMC")
                .pattern("SRS")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('R', Items.REDSTONE)
                .define('C', ModBlocks.ENERGY_CABLE.get())
                .define('M', ModBlocks.MACHINE_CASING.get())
                .unlockedBy("has_machine_casing", has(ModBlocks.MACHINE_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ADVANCED_ENERGY_CELL.get())
                .pattern("ARA")
                .pattern("CEC")
                .pattern("ARA")
                .define('A', ModItems.ALUMINIUM_INGOT.get())
                .define('R', Items.REDSTONE_BLOCK)
                .define('C', ModBlocks.ADVANCED_ENERGY_CABLE.get())
                .define('E', ModBlocks.ENERGY_CELL.get())
                .unlockedBy("has_energy_cell", has(ModBlocks.ENERGY_CELL.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ULTIMATE_ENERGY_CELL.get())
                .pattern("UDU")
                .pattern("CEC")
                .pattern("UDU")
                .define('U', ModItems.URANIUM_INGOT.get())
                .define('D', Items.DIAMOND)
                .define('C', ModBlocks.ULTIMATE_ENERGY_CABLE.get())
                .define('E', ModBlocks.ADVANCED_ENERGY_CELL.get())
                .unlockedBy("has_advanced_energy_cell", has(ModBlocks.ADVANCED_ENERGY_CELL.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ULTRA_ENERGY_CELL.get())
                .pattern("POP")
                .pattern("CEC")
                .pattern("POP")
                .define('P', ModItems.OXIDATED_PLUTONIUM_INGOT.get())
                .define('O', ModItems.ENRICHED_URANIUM.get())
                .define('C', ModBlocks.ULTRA_ENERGY_CABLE.get())
                .define('E', ModBlocks.ULTIMATE_ENERGY_CELL.get())
                .unlockedBy("has_ultimate_energy_cell", has(ModBlocks.ULTIMATE_ENERGY_CELL.get()))
                .save(output);

        // Equipment & Robot parts
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CONFIGURATOR.get())
                .pattern(" I ")
                .pattern("ICI")
                .pattern(" S ")
                .define('I', Items.IRON_INGOT)
                .define('C', ModItems.BASIC_CIRCUIT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_basic_circuit", has(ModItems.BASIC_CIRCUIT.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.PLUTONIUM_EXTRACTOR.get())
                .pattern(" S ")
                .pattern("ACA")
                .pattern(" R ")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('A', ModItems.PRESSED_ALUMINIUM.get())
                .define('C', ModItems.ULTIMATE_CIRCUIT.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_ultimate_circuit", has(ModItems.ULTIMATE_CIRCUIT.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.ISOLATED_BACKPACK.get())
                .pattern("LAL")
                .pattern("SCS")
                .pattern("LAL")
                .define('L', Items.LEATHER)
                .define('A', ModItems.PRESSED_ALUMINIUM.get())
                .define('S', ModItems.STEEL_INGOT.get())
                .define('C', Items.CHEST)
                .unlockedBy("has_pressed_aluminium", has(ModItems.PRESSED_ALUMINIUM.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.HI_TECH_GOGGLES.get())
                .pattern("GCG")
                .pattern("ACA")
                .pattern("III")
                .define('G', Items.GLASS)
                .define('C', Items.COPPER_INGOT)
                .define('A', ModItems.ADVANCED_CIRCUIT.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_advanced_circuit", has(ModItems.ADVANCED_CIRCUIT.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.AUTO_RECHARGER_UPGRADE_TEMPLATE.get())
                .pattern("ARA")
                .pattern("DSD")
                .pattern("ACA")
                .define('A', ModItems.ALUMINIUM_INGOT.get())
                .define('R', Items.REDSTONE_BLOCK)
                .define('D', Items.DIAMOND)
                .define('S', ModItems.STEEL_INGOT.get())
                .define('C', ModBlocks.ENERGY_CABLE.get())
                .unlockedBy("has_aluminium_ingot", has(ModItems.ALUMINIUM_INGOT.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WHEEL.get(), 2)
                .pattern(" S ")
                .pattern("SIS")
                .pattern(" S ")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROBOT_CASING.get())
                .pattern("SGS")
                .pattern("SMS")
                .pattern("AAA")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('G', Items.GLASS_PANE)
                .define('M', ModBlocks.MACHINE_CASING.get())
                .define('A', ModItems.ALUMINIUM_INGOT.get())
                .unlockedBy("has_machine_casing", has(ModBlocks.MACHINE_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROBOT.get())
                .pattern("SES")
                .pattern("ACA")
                .pattern("WMW")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('E', ModItems.MECHANICAL_EYES.get())
                .define('A', ModItems.ALUMINIUM_INGOT.get())
                .define('C', ModItems.ROBOT_CIRCUIT.get())
                .define('W', ModItems.WHEEL.get())
                .define('M', ModItems.ROBOT_CASING.get())
                .unlockedBy("has_robot_casing", has(ModItems.ROBOT_CASING.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.HAMMER_X.get())
                .pattern("SDS")
                .pattern("SCS")
                .pattern(" S ")
                .define('S', ModItems.STEEL_INGOT.get())
                .define('D', Items.DIAMOND)
                .define('C', ModItems.ULTRA_CIRCUIT.get())
                .unlockedBy("has_ultra_circuit", has(ModItems.ULTRA_CIRCUIT.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SLOT_UPGRADE_TIER_1.get())
                .pattern("ASA")
                .pattern("SCS")
                .pattern("ASA")
                .define('A', ModItems.ALUMINIUM_INGOT.get())
                .define('S', ModItems.STEEL_INGOT.get())
                .define('C', Items.CHEST)
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SLOT_UPGRADE_TIER_2.get())
                .pattern("GCG")
                .pattern("GUG")
                .pattern("GCG")
                .define('G', Items.GOLD_INGOT)
                .define('C', ModItems.ADVANCED_CIRCUIT.get())
                .define('U', ModItems.SLOT_UPGRADE_TIER_1.get())
                .unlockedBy("has_slot_upgrade_tier_1", has(ModItems.SLOT_UPGRADE_TIER_1.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SLOT_UPGRADE_TIER_3.get())
                .pattern("DCD")
                .pattern("DUD")
                .pattern("DCD")
                .define('D', Items.DIAMOND)
                .define('C', ModItems.ULTIMATE_CIRCUIT.get())
                .define('U', ModItems.SLOT_UPGRADE_TIER_2.get())
                .unlockedBy("has_slot_upgrade_tier_2", has(ModItems.SLOT_UPGRADE_TIER_2.get()))
                .save(output);
    }

    private void tierUpgrade8(RecipeOutput output, ItemLike result, ItemLike previous, ItemLike circuit) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, 8)
                .pattern("PPP")
                .pattern("PCP")
                .pattern("PPP")
                .define('P', previous)
                .define('C', circuit)
                .unlockedBy("has_circuit", has(circuit))
                .save(output);
    }

    private void buildSmeltingRecipes(RecipeOutput output) {
        smeltAndBlast(output, ModItems.ALUMINIUM_INGOT.get(), "aluminium_ingot",
                ModBlocks.ALUMINIUM_ORE.get(), ModBlocks.DEEPSLATE_ALUMINIUM_ORE.get(), ModItems.RAW_ALUMINIUM.get());

        smeltAndBlast(output, ModItems.BAUXITE_INGOT.get(), "bauxite_ingot",
                ModBlocks.BAUXITE_ORE.get(), ModBlocks.DEEPSLATE_BAUXITE_ORE.get(), ModItems.RAW_BAUXITE.get());

        smeltAndBlast(output, ModItems.METEOROTITE_INGOT.get(), "meteorotite_ingot",
                ModBlocks.METEOROTITE_ORE.get(), ModItems.RAW_METEOROTITE.get());

        smeltAndBlast(output, ModItems.OXIDATED_PLUTONIUM_INGOT.get(), "oxidated_plutonium_ingot",
                ModBlocks.OXIDATED_PLUTONIUM_ORE.get(), ModBlocks.DEEPSLATE_OXIDATED_PLUTONIUM_ORE.get(), ModItems.RAW_OXIDATED_PLUTONIUM.get());

        smeltAndBlast(output, ModItems.STEEL_INGOT.get(), "steel_ingot",
                ModBlocks.STEEL_ORE.get(), ModItems.RAW_STEEL.get());

        smeltAndBlast(output, ModItems.UNSTABLE_OLD_URANIUM_INGOT.get(), "unstable_old_uranium_ingot",
                ModBlocks.UNSTABLE_OLD_URANIUM_ORE.get(), ModBlocks.DEEPSLATE_UNSTABLE_OLD_URANIUM_ORE.get(), ModItems.RAW_UNSTABLE_OLD_URANIUM.get());

        smeltAndBlast(output, ModItems.URANIUM_INGOT.get(), "uranium_ingot",
                ModBlocks.URANIUM_ORE.get(), ModBlocks.DEEPSLATE_URANIUM_ORE.get(), ModItems.RAW_URANIUM.get());
    }

    private void smeltAndBlast(RecipeOutput output, ItemLike result, String resultName, ItemLike... inputs) {
        for (ItemLike input : inputs) {
            String inputName = getItemName(input);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, result, 0.7F, 200)
                    .unlockedBy(getHasName(input), has(input))
                    .save(output, id(resultName + "_from_smelting_" + inputName));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, 0.7F, 100)
                    .unlockedBy(getHasName(input), has(input))
                    .save(output, id(resultName + "_from_blasting_" + inputName));
        }
    }

    private void buildMachineRecipes(RecipeOutput output) {
        // Enrichment Chamber
        output.accept(
                id("enrichment/raw_uranium"),
                new MachineRecipe(
                        MachineRecipeKind.ENRICHMENT,
                        List.of(input(ModItems.RAW_URANIUM.get())),
                        FluidStack.EMPTY,
                        new ItemStack(ModItems.ENRICHED_URANIUM.get()),
                        FluidStack.EMPTY,
                        40,
                        100
                ),
                null
        );

        // PCB Printer
        output.accept(
                id("pcb/basic_circuit"),
                new MachineRecipe(
                        MachineRecipeKind.PCB_PRINTING,
                        List.of(
                                input(Items.REDSTONE),
                                input(Items.IRON_INGOT),
                                input(ModItems.ALUMINIUM_INGOT.get())
                        ),
                        FluidStack.EMPTY,
                        new ItemStack(ModItems.BASIC_CIRCUIT.get()),
                        FluidStack.EMPTY,
                        20,
                        100
                ),
                null
        );

        output.accept(
                id("pcb/advanced_circuit"),
                new MachineRecipe(
                        MachineRecipeKind.PCB_PRINTING,
                        List.of(
                                input(Items.GOLD_INGOT),
                                input(ModItems.ALUMINIUM_INGOT.get()),
                                input(ModItems.BASIC_CIRCUIT.get())
                        ),
                        FluidStack.EMPTY,
                        new ItemStack(ModItems.ADVANCED_CIRCUIT.get()),
                        FluidStack.EMPTY,
                        20,
                        100
                ),
                null
        );

        output.accept(
                id("pcb/ultimate_circuit"),
                new MachineRecipe(
                        MachineRecipeKind.PCB_PRINTING,
                        List.of(
                                input(Items.DIAMOND),
                                input(ModItems.ADVANCED_CIRCUIT.get()),
                                input(Items.GOLD_INGOT)
                        ),
                        FluidStack.EMPTY,
                        new ItemStack(ModItems.ULTIMATE_CIRCUIT.get()),
                        FluidStack.EMPTY,
                        20,
                        100
                ),
                null
        );

        output.accept(
                id("pcb/ultra_circuit"),
                new MachineRecipe(
                        MachineRecipeKind.PCB_PRINTING,
                        List.of(
                                input(Items.DIAMOND),
                                input(ModItems.ULTIMATE_CIRCUIT.get()),
                                input(Items.NETHERITE_INGOT)
                        ),
                        FluidStack.EMPTY,
                        new ItemStack(ModItems.ULTRA_CIRCUIT.get()),
                        FluidStack.EMPTY,
                        20,
                        100
                ),
                null
        );

        output.accept(
                id("pcb/robot_circuit"),
                new MachineRecipe(
                        MachineRecipeKind.PCB_PRINTING,
                        List.of(
                                input(ModItems.STEEL_INGOT.get(), 2),
                                input(ModItems.ALUMINIUM_INGOT.get(), 2),
                                input(Items.REDSTONE, 2)
                        ),
                        FluidStack.EMPTY,
                        new ItemStack(ModItems.ROBOT_CIRCUIT.get()),
                        FluidStack.EMPTY,
                        20,
                        100
                ),
                null
        );

        // Stamping Press
        output.accept(
                id("pressing/pressed_aluminium"),
                new MachineRecipe(
                        MachineRecipeKind.STAMPING,
                        List.of(input(ModItems.ALUMINIUM_INGOT.get())),
                        FluidStack.EMPTY,
                        new ItemStack(ModItems.PRESSED_ALUMINIUM.get()),
                        FluidStack.EMPTY,
                        25,
                        80
                ),
                null
        );

        output.accept(
                id("pressing/mechanical_eyes"),
                new MachineRecipe(
                        MachineRecipeKind.STAMPING,
                        List.of(
                                input(ModItems.PRESSED_ALUMINIUM.get()),
                                input(Items.GLOWSTONE_DUST),
                                input(ModItems.ROBOT_CIRCUIT.get()),
                                input(Items.GLASS_PANE)
                        ),
                        FluidStack.EMPTY,
                        new ItemStack(ModItems.MECHANICAL_EYES.get()),
                        FluidStack.EMPTY,
                        25,
                        80
                ),
                null
        );

        // Oil Refinery
        output.accept(
                id("refining/crude_oil_to_liquid_isobutane"),
                new MachineRecipe(
                        MachineRecipeKind.REFINING,
                        List.of(),
                        new FluidStack(ModFluids.CRUDE_OIL.get(), 100),
                        ItemStack.EMPTY,
                        new FluidStack(ModFluids.LIQUID_ISOBUTANE.get(), 50),
                        40,
                        25
                ),
                null
        );
    }

    private static MachineItemInput input(ItemLike item) {
        return input(item, 1);
    }

    private static MachineItemInput input(ItemLike item, int count) {
        return new MachineItemInput(Ingredient.of(item), count);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, path);
    }
}
