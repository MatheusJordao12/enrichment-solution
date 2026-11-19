package com.enrichmentsolution.mod.common.event;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Grants each player one Enrichment Solution Guide Book the first time they join a world.
 */
@EventBusSubscriber(modid = EnrichmentSolutionMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PlayerJoinHandler {

    private static final String RECEIVED_GUIDE_KEY = EnrichmentSolutionMod.MOD_ID + ":received_guide_book";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persisted = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(RECEIVED_GUIDE_KEY)) {
            return;
        }

        ItemStack guideStack = new ItemStack(ModItems.ENRICHMENT_GUIDE.get());
        if (!player.getInventory().add(guideStack)) {
            player.drop(guideStack, false);
        }

        persisted.putBoolean(RECEIVED_GUIDE_KEY, true);
        persistentData.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private PlayerJoinHandler() {}
}
