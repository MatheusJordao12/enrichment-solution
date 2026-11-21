package com.enrichmentsolution.mod.gametest;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.block.entity.EnergyCellBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.EnrichmentChamberBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.HeatExchangerBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.OilRefineryBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.PcbPrinterBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.PelletizerControllerBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.StampingPressBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.TankBlockEntity;
import com.enrichmentsolution.mod.registry.ModBlocks;
import com.enrichmentsolution.mod.registry.ModFluids;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(EnrichmentSolutionMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModGameTests {

    private static final String EMPTY_TEMPLATE = "empty_7x5x7";

    private ModGameTests() {
    }

    private static void fillEnergy(GameTestHelper helper, IEnergyStorage storage, int targetAmount) {
        helper.assertTrue(storage != null, "Energy storage should not be null");
        int remaining = targetAmount;
        for (int i = 0; i < 1000 && remaining > 0; i++) {
            int accepted = storage.receiveEnergy(remaining, false);
            if (accepted <= 0) {
                break;
            }
            remaining -= accepted;
        }
        helper.assertTrue(storage.getEnergyStored() >= targetAmount,
                "Expected at least " + targetAmount + " FE stored, got " + storage.getEnergyStored());
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void enrichment_processes_raw_uranium(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ENRICHMENT_CHAMBER.get());

        EnrichmentChamberBlockEntity be = helper.getBlockEntity(pos);
        fillEnergy(helper, be.getEnergyStorage(null), 10_000);

        IItemHandler items = be.getItemHandler(null);
        helper.assertTrue(items != null, "Item handler should not be null");
        ItemStack remainder = items.insertItem(
                EnrichmentChamberBlockEntity.SLOT_INPUT,
                new ItemStack(ModItems.RAW_URANIUM.get(), 1),
                false
        );
        helper.assertTrue(remainder.isEmpty(), "Raw uranium should fit in input slot");

        helper.succeedWhen(() -> {
            ItemStack out = items.getStackInSlot(EnrichmentChamberBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(
                    out.is(ModItems.YELLOWCAKE.get()) && out.getCount() >= 1,
                    "Expected yellowcake in output slot, found: " + out
            );
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void refinery_processes_crude_oil(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.OIL_REFINERY.get());
        helper.setBlock(pos.east(), ModBlocks.DISTILLATION_TOWER.get());

        OilRefineryBlockEntity be = helper.getBlockEntity(pos);
        fillEnergy(helper, be.getEnergyStorage(null), 10_000);

        IFluidHandler fluids = be.getFluidHandler(null);
        helper.assertTrue(fluids != null, "Fluid handler should not be null");
        int filled = fluids.fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled >= 100, "Expected at least 100 mB crude oil filled, got " + filled);

        helper.succeedWhen(() -> {
            FluidStack out = fluids.getFluidInTank(1);
            helper.assertTrue(
                    out.getFluid().isSame(ModFluids.LIQUID_ISOBUTANE.get()) && out.getAmount() >= 50,
                    "Expected >= 50 mB liquid_isobutane in output tank, found: " + out.getAmount()
            );
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 60)
    public static void refinery_requires_distillation_tower(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.OIL_REFINERY.get());

        OilRefineryBlockEntity be = helper.getBlockEntity(pos);
        fillEnergy(helper, be.getEnergyStorage(null), 10_000);

        IFluidHandler fluids = be.getFluidHandler(null);
        helper.assertTrue(fluids != null, "Fluid handler should not be null");
        int filled = fluids.fill(new FluidStack(ModFluids.CRUDE_OIL.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled >= 100, "Expected at least 100 mB crude oil filled, got " + filled);

        helper.runAfterDelay(35, () -> {
            FluidStack out = fluids.getFluidInTank(1);
            helper.assertTrue(
                    out.isEmpty(),
                    "Expected no output without a Distillation Tower, found: " + out.getAmount()
            );
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 150)
    public static void stamping_press_processes_plate(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.STAMPING_PRESS.get());

        StampingPressBlockEntity be = helper.getBlockEntity(pos);
        fillEnergy(helper, be.getEnergyStorage(null), 10_000);

        IItemHandler items = be.getItemHandler(null);
        helper.assertTrue(items != null, "Item handler should not be null");
        ItemStack remainder = items.insertItem(0, new ItemStack(ModItems.ALUMINIUM_INGOT.get(), 1), false);
        helper.assertTrue(remainder.isEmpty(), "Aluminium ingot should fit in input slot");

        helper.succeedWhen(() -> {
            ItemStack out = items.getStackInSlot(StampingPressBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(
                    out.is(ModItems.PRESSED_ALUMINIUM.get()) && out.getCount() >= 1,
                    "Expected pressed_aluminium in output slot, found: " + out
            );
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 150)
    public static void pcb_printer_prints_circuit(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.PCB_PRINTER.get());

        PcbPrinterBlockEntity be = helper.getBlockEntity(pos);
        IEnergyStorage energy = be.getEnergyStorage(null);
        helper.assertTrue(energy != null, "Energy storage should not be null");
        fillEnergy(helper, energy, energy.getMaxEnergyStored());

        IItemHandler items = be.getItemHandler(null);
        helper.assertTrue(items != null, "Item handler should not be null");
        helper.assertTrue(items.insertItem(PcbPrinterBlockEntity.SLOT_INPUT_0, new ItemStack(Items.REDSTONE, 1), false).isEmpty(),
                "Redstone should fit in slot 0");
        helper.assertTrue(items.insertItem(PcbPrinterBlockEntity.SLOT_INPUT_1, new ItemStack(Items.IRON_INGOT, 1), false).isEmpty(),
                "Iron ingot should fit in slot 1");
        helper.assertTrue(items.insertItem(PcbPrinterBlockEntity.SLOT_INPUT_2, new ItemStack(ModItems.ALUMINIUM_INGOT.get(), 1), false).isEmpty(),
                "Aluminium ingot should fit in slot 2");

        helper.assertTrue(be.startPrinting(), "PCB Printer should start printing basic_circuit");
        helper.onEachTick(() -> energy.receiveEnergy(energy.getMaxEnergyStored(), false));

        helper.succeedWhen(() -> {
            ItemStack out = items.getStackInSlot(PcbPrinterBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(
                    out.is(ModItems.BASIC_CIRCUIT.get()) && out.getCount() >= 1,
                    "Expected basic_circuit in output slot, found: " + out
            );
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void heat_exchanger_boils_isobutane(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.HEAT_EXCHANGER.get());

        HeatExchangerBlockEntity be = helper.getBlockEntity(pos);
        int waterFilled = be.getWaterTank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(waterFilled > 0, "Expected water tank to accept water");

        int isoFilled = be.getLiquidIsobutaneTank().fill(
                new FluidStack(ModFluids.LIQUID_ISOBUTANE.get(), 1000),
                IFluidHandler.FluidAction.EXECUTE
        );
        helper.assertTrue(isoFilled > 0, "Expected liquid isobutane tank to accept fluid");

        helper.succeedWhen(() -> {
            FluidStack gas = be.getGaseousIsobutaneTank().getFluid();
            helper.assertTrue(
                    gas.getFluid().isSame(ModFluids.GASEOUS_ISOBUTANE.get()) && gas.getAmount() > 0,
                    "Expected gaseous_isobutane output > 0, found: " + gas.getAmount()
            );
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void fluid_pipe_network_transfers_fluid(GameTestHelper helper) {
        BlockPos sourceTankPos = new BlockPos(0, 1, 1);
        BlockPos outputPortPos = new BlockPos(1, 1, 1);
        BlockPos pipe1Pos = new BlockPos(2, 1, 1);
        BlockPos pipe2Pos = new BlockPos(3, 1, 1);
        BlockPos pipe3Pos = new BlockPos(4, 1, 1);
        BlockPos inputPortPos = new BlockPos(5, 1, 1);
        BlockPos targetTankPos = new BlockPos(6, 1, 1);

        helper.setBlock(sourceTankPos, ModBlocks.TANK_BLOCK.get());
        helper.setBlock(outputPortPos, ModBlocks.TANK_WATER_OUTPUT.get());
        helper.setBlock(pipe1Pos, ModBlocks.BASIC_FLUID_PIPE.get());
        helper.setBlock(pipe2Pos, ModBlocks.BASIC_FLUID_PIPE.get());
        helper.setBlock(pipe3Pos, ModBlocks.BASIC_FLUID_PIPE.get());
        helper.setBlock(inputPortPos, ModBlocks.TANK_WATER_INPUT.get());
        helper.setBlock(targetTankPos, ModBlocks.TANK_BLOCK.get());

        TankBlockEntity sourceTank = helper.getBlockEntity(sourceTankPos);
        int filled = sourceTank.getMasterTank().fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled >= 1000, "Expected source tank to accept at least 1000 mB water");

        helper.succeedWhen(() -> {
            TankBlockEntity targetTank = helper.getBlockEntity(targetTankPos);
            FluidStack targetFluid = targetTank.getMasterTank().getFluid();
            helper.assertTrue(
                    targetFluid.getFluid().isSame(Fluids.WATER) && targetFluid.getAmount() > 0,
                    "Expected water to arrive in target tank, found: " + targetFluid.getAmount()
            );
        });
    }

    @GameTest(template = "empty_16x10x16", timeoutTicks = 200)
    public static void factory_stress_benchmark(GameTestHelper helper) {
        // 1. 108-block Tank (6x3x6) at x=0..5, y=1..3, z=0..5
        int tankBlocks = 0;
        for (int x = 0; x < 6; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 0; z < 6; z++) {
                    helper.setBlock(new BlockPos(x, y, z), ModBlocks.TANK_BLOCK.get());
                    tankBlocks++;
                }
            }
        }
        helper.assertTrue(tankBlocks >= 100, "Expected >= 100 tank blocks, placed " + tankBlocks);

        TankBlockEntity sourceTank = helper.getBlockEntity(new BlockPos(0, 1, 0));
        int waterFilled = sourceTank.getMasterTank().fill(new FluidStack(Fluids.WATER, 500_000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(waterFilled >= 100_000, "Expected >= 100,000 mB water in 108-block tank, got " + waterFilled);

        // 2. Output port + 139-pipe serpentine network + target tank
        helper.setBlock(new BlockPos(6, 1, 0), ModBlocks.TANK_WATER_OUTPUT.get());
        int conduitCount = 0;

        // Layer y=1 serpentine across x=7..15, z=0..14
        for (int row = 0; row < 8; row++) {
            int z = row * 2;
            for (int x = 7; x <= 15; x++) {
                helper.setBlock(new BlockPos(x, 1, z), ModBlocks.BASIC_FLUID_PIPE.get());
                conduitCount++;
            }
            if (row < 7) {
                int turnX = (row % 2 == 0) ? 15 : 7;
                helper.setBlock(new BlockPos(turnX, 1, z + 1), ModBlocks.BASIC_FLUID_PIPE.get());
                conduitCount++;
            }
        }
        // Step up at (7, 2, 14) since row 7 (z=14) ends at x=7
        helper.setBlock(new BlockPos(7, 2, 14), ModBlocks.BASIC_FLUID_PIPE.get());
        conduitCount++;

        // Layer y=3 serpentine from z=14 back to z=4
        for (int row = 0; row < 6; row++) {
            int z = 14 - row * 2;
            for (int x = 7; x <= 15; x++) {
                helper.setBlock(new BlockPos(x, 3, z), ModBlocks.BASIC_FLUID_PIPE.get());
                conduitCount++;
            }
            if (row < 5) {
                int turnX = (row % 2 == 0) ? 15 : 7;
                helper.setBlock(new BlockPos(turnX, 3, z - 1), ModBlocks.BASIC_FLUID_PIPE.get());
                conduitCount++;
            }
        }

        BlockPos targetTankPos = new BlockPos(7, 3, 2);
        helper.setBlock(targetTankPos, ModBlocks.TANK_BLOCK.get());
        helper.setBlock(new BlockPos(7, 3, 3), ModBlocks.TANK_WATER_INPUT.get());

        // 3. Ultimate Energy Cell at (0, 6, 0) pushing DOWN into 101-cable serpentine on y=5
        helper.setBlock(new BlockPos(0, 6, 0), ModBlocks.ULTIMATE_ENERGY_CELL.get());
        EnergyCellBlockEntity energyCell = helper.getBlockEntity(new BlockPos(0, 6, 0));
        fillEnergy(helper, energyCell.getEnergyStorage(Direction.UP), 10_000_000);

        helper.setBlock(new BlockPos(0, 5, 0), ModBlocks.ENERGY_CABLE.get());
        conduitCount++;
        for (int row = 0; row < 6; row++) {
            int z = row * 2;
            int startX = (row == 0) ? 1 : 0;
            for (int x = startX; x <= 15; x++) {
                helper.setBlock(new BlockPos(x, 5, z), ModBlocks.ENERGY_CABLE.get());
                conduitCount++;
            }
            if (row < 5) {
                int turnX = (row % 2 == 0) ? 15 : 0;
                helper.setBlock(new BlockPos(turnX, 5, z + 1), ModBlocks.ENERGY_CABLE.get());
                conduitCount++;
            }
        }
        helper.assertTrue(conduitCount >= 200, "Expected >= 200 conduits, placed " + conduitCount);

        // 4. 30 active machines on y=6 directly above the energy cable network
        int machineCount = 0;
        for (int x = 1; x <= 15; x++) {
            BlockPos enrichPos = new BlockPos(x, 6, 0);
            helper.setBlock(enrichPos, ModBlocks.ENRICHMENT_CHAMBER.get());
            EnrichmentChamberBlockEntity enrichBe = helper.getBlockEntity(enrichPos);
            enrichBe.getItemHandler(null).insertItem(
                    EnrichmentChamberBlockEntity.SLOT_INPUT,
                    new ItemStack(ModItems.RAW_URANIUM.get(), 64),
                    false
            );
            machineCount++;

            BlockPos pressPos = new BlockPos(x, 6, 2);
            helper.setBlock(pressPos, ModBlocks.STAMPING_PRESS.get());
            StampingPressBlockEntity pressBe = helper.getBlockEntity(pressPos);
            pressBe.getItemHandler(null).insertItem(
                    0,
                    new ItemStack(ModItems.ALUMINIUM_INGOT.get(), 64),
                    false
            );
            machineCount++;
        }
        helper.assertTrue(machineCount >= 30, "Expected >= 30 machines, placed " + machineCount);

        final int totalConduits = conduitCount;
        final int totalMachines = machineCount;
        final int totalTankBlocks = tankBlocks;
        final int benchmarkTicks = 100;

        AtomicInteger tickCount = new AtomicInteger(0);

        helper.onEachTick(() -> {
            int tick = tickCount.incrementAndGet();
            if (tick == benchmarkTicks) {
                long avgTickNanos = helper.getLevel().getServer().getAverageTickTimeNanos();
                double avgMsPerTick = avgTickNanos / 1_000_000.0;
                double estTps = Math.min(20.0, 1000.0 / Math.max(avgMsPerTick, 0.001));
                EnrichmentSolutionMod.LOGGER.info(
                        "Factory stress benchmark ({} conduits, {} machines, {} tank blocks, {} ticks): avg={} ms/tick, est={} TPS",
                        totalConduits,
                        totalMachines,
                        totalTankBlocks,
                        benchmarkTicks,
                        String.format("%.3f", avgMsPerTick),
                        String.format("%.2f", estTps)
                );

                helper.assertTrue(
                        avgMsPerTick < 50.0,
                        String.format("Average tick time %.3f ms/tick exceeded 50.0 ms/tick (20 TPS budget)", avgMsPerTick)
                );

                TankBlockEntity targetTank = helper.getBlockEntity(targetTankPos);
                FluidStack targetFluid = targetTank.getMasterTank().getFluid();
                helper.assertTrue(
                        targetFluid.getFluid().isSame(Fluids.WATER) && targetFluid.getAmount() > 0,
                        "Expected fluid to reach target tank during stress benchmark, found: " + targetFluid.getAmount()
                );

                helper.succeed();
            }
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 40)
    public static void plutonium_extractor_extracts_ore(GameTestHelper helper) {
        BlockPos pos1 = new BlockPos(1, 1, 1);
        BlockPos pos2 = new BlockPos(2, 1, 1);
        helper.setBlock(pos1, ModBlocks.OXIDATED_PLUTONIUM_ORE.get());
        helper.setBlock(pos2, ModBlocks.OXIDATED_PLUTONIUM_ORE.get());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack extractor = new ItemStack(ModItems.PLUTONIUM_EXTRACTOR.get());
        helper.assertTrue(
                !com.enrichmentsolution.mod.common.item.PlutoniumExtractorItem.isFilled(extractor),
                "Expected newly created Plutonium Extractor to start empty"
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, extractor);

        // 1. Right-click ore -> fills extractor, turns ore into cobblestone, nothing in inventory yet
        BlockPos absPos1 = helper.absolutePos(pos1);
        BlockHitResult hit1 = new BlockHitResult(Vec3.atCenterOf(absPos1), Direction.UP, absPos1, false);
        InteractionResult result1 = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit1));
        helper.assertTrue(result1.consumesAction(), "Expected Plutonium Extractor useOn to succeed");
        helper.assertBlockPresent(Blocks.COBBLESTONE, pos1);
        helper.assertTrue(
                com.enrichmentsolution.mod.common.item.PlutoniumExtractorItem.isFilled(player.getMainHandItem()),
                "Expected Plutonium Extractor to be filled after extraction"
        );
        helper.assertTrue(
                !com.enrichmentsolution.mod.common.event.RadiationEvents.hasExposedRadioactivePlutonium(player),
                "Expected filled Plutonium Extractor to isolate plutonium radiation"
        );

        // 2. Trying to extract another ore while already filled should leave ore untouched
        BlockPos absPos2 = helper.absolutePos(pos2);
        BlockHitResult hit2 = new BlockHitResult(Vec3.atCenterOf(absPos2), Direction.UP, absPos2, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit2));
        helper.assertBlockPresent(ModBlocks.OXIDATED_PLUTONIUM_ORE.get(), pos2);

        // 3. Shift + Right-click with Isolated Backpack in offhand -> deposits into backpack
        ItemStack backpack = new ItemStack(ModItems.ISOLATED_BACKPACK.get());
        player.setItemInHand(InteractionHand.OFF_HAND, backpack);
        player.setShiftKeyDown(true);
        var depositBackpackRes = player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(depositBackpackRes.getResult().consumesAction(), "Expected Shift+Right-click deposit into backpack to succeed");
        helper.assertTrue(
                !com.enrichmentsolution.mod.common.item.PlutoniumExtractorItem.isFilled(player.getMainHandItem()),
                "Expected Plutonium Extractor to be empty after depositing into backpack"
        );
        ItemContainerContents bpContents = player.getOffhandItem().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        boolean inBackpack = bpContents.stream().anyMatch(s -> s.is(ModItems.RAW_OXIDATED_PLUTONIUM.get()) && s.getCount() >= 1);
        helper.assertTrue(inBackpack, "Expected raw_oxidated_plutonium inside offhand Isolated Backpack");

        // 4. Extract second ore, remove offhand backpack, Shift + Right-click -> deposits into player inventory
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        InteractionResult result2 = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit2));
        helper.assertTrue(result2.consumesAction(), "Expected second extraction to succeed");
        helper.assertBlockPresent(Blocks.COBBLESTONE, pos2);

        player.setShiftKeyDown(true);
        var depositInvRes = player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(depositInvRes.getResult().consumesAction(), "Expected Shift+Right-click deposit into inventory to succeed");
        helper.assertTrue(
                !com.enrichmentsolution.mod.common.item.PlutoniumExtractorItem.isFilled(player.getMainHandItem()),
                "Expected Plutonium Extractor to be empty after depositing into inventory"
        );

        boolean hasDrop = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.RAW_OXIDATED_PLUTONIUM.get()) && stack.getCount() >= 1) {
                hasDrop = true;
                break;
            }
        }
        helper.assertTrue(hasDrop, "Expected player inventory to contain raw_oxidated_plutonium");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 40)
    public static void isolated_backpack_isolates_plutonium_radiation(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.invulnerableTime = 0;

        // Exposed plutonium in normal inventory should deal radiation damage
        player.getInventory().setItem(0, new ItemStack(ModItems.RAW_OXIDATED_PLUTONIUM.get()));
        float healthBeforeExposed = player.getHealth();
        player.tickCount = 20;
        player.tick();
        helper.assertTrue(
                player.getHealth() < healthBeforeExposed,
                "Expected exposed raw_oxidated_plutonium to deal radiation damage, health=" + player.getHealth()
        );

        // Storing plutonium inside Isolated Backpack should deal 0 radiation damage
        player.getInventory().clearContent();
        player.setHealth(player.getMaxHealth());
        player.invulnerableTime = 0;

        ItemStack backpack = new ItemStack(ModItems.ISOLATED_BACKPACK.get());
        backpack.set(DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(ModItems.RAW_OXIDATED_PLUTONIUM.get()))));
        player.getInventory().setItem(0, backpack);

        float healthBeforeIsolated = player.getHealth();
        player.tickCount = 40;
        player.tick();
        helper.assertTrue(
                player.getHealth() == healthBeforeIsolated,
                "Expected isolated plutonium in backpack to deal 0 damage, health=" + player.getHealth()
        );

        // Also verify IsolatedBackpackMenu quickMoveStack moves plutonium into the backpack cleanly
        player.getInventory().clearContent();
        ItemStack menuBackpack = new ItemStack(ModItems.ISOLATED_BACKPACK.get());
        player.getInventory().setItem(0, menuBackpack);
        player.getInventory().setItem(1, new ItemStack(ModItems.RAW_OXIDATED_PLUTONIUM.get()));
        var menu = new com.enrichmentsolution.mod.common.menu.IsolatedBackpackMenu(0, player.getInventory(), menuBackpack, 0);
        // Slot 27 + (9..35) = main inv, slot 27 + 27 + 1 = hotbar slot 1 (index 55)
        menu.quickMoveStack(player, 27 + 27 + 1);
        menu.removed(player);
        helper.assertTrue(
                !com.enrichmentsolution.mod.common.event.RadiationEvents.hasExposedRadioactivePlutonium(player),
                "Expected no exposed plutonium after moving into backpack via IsolatedBackpackMenu"
        );

        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 40)
    public static void hammer_x_speed_and_energy(GameTestHelper helper) {
        ItemStack hammer = new ItemStack(ModItems.HAMMER_X.get());
        var stone = Blocks.STONE.defaultBlockState();

        helper.assertTrue(
                hammer.getDestroySpeed(stone) == 1.0F,
                "Expected unpowered Hammer-X destroy speed to be 1.0F, got " + hammer.getDestroySpeed(stone)
        );

        com.enrichmentsolution.mod.common.item.HammerXItem.setStoredEnergy(hammer, 10_000);
        helper.assertTrue(
                hammer.getDestroySpeed(stone) == com.enrichmentsolution.mod.common.item.HammerXItem.POWERED_MINING_SPEED,
                "Expected powered Hammer-X destroy speed to be 25.0F, got " + hammer.getDestroySpeed(stone)
        );
        helper.assertTrue(
                com.enrichmentsolution.mod.common.item.HammerXItem.get3x3Positions(new BlockPos(5, 5, 5), Direction.UP).size() == 8,
                "Expected 8 neighbor positions for 3x3 hammer mining"
        );

        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 300)
    public static void pelletizer_multiblock_test(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(3, 1, 1);
        Direction facing = Direction.NORTH;
        Direction backDir = facing.getOpposite();
        Direction rightDir = facing.getClockWise();

        for (int back = 0; back <= 3; back++) {
            for (int y = 0; y <= 2; y++) {
                for (int x = -1; x <= 1; x++) {
                    BlockPos pos = controllerPos.relative(rightDir, x).relative(backDir, back).above(y);
                    if (x == 0 && y == 0 && back == 0) {
                        helper.setBlock(pos, ModBlocks.PELLETIZER_CONTROLLER.get().defaultBlockState()
                                .setValue(com.enrichmentsolution.mod.common.block.PelletizerControllerBlock.FACING, facing));
                    } else if (x == 0 && y == 2 && back == 1) {
                        helper.setBlock(pos, ModBlocks.CRUSHER_MODULE.get());
                    } else if (x == 0 && y == 2 && back == 2) {
                        helper.setBlock(pos, ModBlocks.HYDRAULIC_PRESS_HEAD.get());
                    } else if (x == 0 && y == 2 && back == 3) {
                        helper.setBlock(pos, ModBlocks.SINTERING_HEATER.get());
                    } else {
                        helper.setBlock(pos, ModBlocks.REACTOR_CASING.get());
                    }
                }
            }
        }

        PelletizerControllerBlockEntity be = helper.getBlockEntity(controllerPos);
        helper.assertTrue(be.isStructureValid(), "Expected 3x3x4 pelletizer structure to be valid");
        fillEnergy(helper, be.getEnergyStorage(null), 50_000);

        IItemHandler items = be.getItemHandler(null);
        helper.assertTrue(items != null, "Item handler should not be null");
        ItemStack remainder = items.insertItem(
                PelletizerControllerBlockEntity.SLOT_INPUT,
                new ItemStack(ModItems.ENRICHED_URANIUM.get(), 2),
                false
        );
        helper.assertTrue(remainder.isEmpty(), "Enriched uranium should fit in pelletizer input slot");

        helper.succeedWhen(() -> {
            ItemStack out = items.getStackInSlot(PelletizerControllerBlockEntity.SLOT_OUTPUT);
            helper.assertTrue(
                    out.is(ModItems.URANIUM_PELLET.get()) && out.getCount() >= 4,
                    "Expected 4x uranium_pellet in output slot, found: " + out
            );
        });
    }
}
