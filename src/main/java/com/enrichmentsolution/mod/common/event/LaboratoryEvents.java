package com.enrichmentsolution.mod.common.event;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.world.LaboratorySavedData;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Optional;

/**
 * Places the unique Destroyed Laboratory in the Overworld (2000-2500 blocks from spawn)
 * and adds the Cartographer trade (1 Emerald + Hi-Tech Goggles -> Destroyed Laboratory Map).
 */
@EventBusSubscriber(modid = EnrichmentSolutionMod.MOD_ID)
public final class LaboratoryEvents {

    private LaboratoryEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }
        LaboratorySavedData data = LaboratorySavedData.getOrCreate(serverLevel);
        if (!data.isPlaced()) {
            ChunkPos cp = event.getChunk().getPos();
            if (data.isNearChunk(cp.x, cp.z)) {
                data.tryPlaceIfChunkLoaded(serverLevel);
            }
        }
    }

    @SubscribeEvent
    public static void onInteractVillager(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }
        if (villager.getVillagerData().getProfession() != VillagerProfession.CARTOGRAPHER) {
            return;
        }
        ServerLevel overworld = event.getLevel().getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }

        MerchantOffers offers = villager.getOffers();
        for (MerchantOffer offer : offers) {
            if (LaboratorySavedData.isLabExplorerMap(offer.getResult())) {
                return;
            }
        }

        ItemStack mapStack = LaboratorySavedData.createLabExplorerMap(overworld);
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                Optional.of(new ItemCost(ModItems.HI_TECH_GOGGLES.get(), 1)),
                mapStack,
                1,
                10,
                0.0F
        ));
    }

    @SubscribeEvent
    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getAbstractVillager() instanceof Villager villager)) {
            return;
        }
        if (villager.getVillagerData().getProfession() != VillagerProfession.CARTOGRAPHER) {
            return;
        }
        if (LaboratorySavedData.isLabExplorerMap(event.getMerchantOffer().getResult())) {
            event.getEntity().sendSystemMessage(
                    Component.translatable("message.enrichment_solution.cartographer.lab_map")
            );
        }
    }
}
