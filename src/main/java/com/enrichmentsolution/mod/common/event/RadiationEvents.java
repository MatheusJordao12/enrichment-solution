package com.enrichmentsolution.mod.common.event;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.registry.ModBlocks;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * Deals periodic radiation damage to survival/adventure players carrying exposed
 * radioactive plutonium items outside of an {@code IsolatedBackpackItem}.
 */
@EventBusSubscriber(modid = EnrichmentSolutionMod.MOD_ID)
public final class RadiationEvents {

    private static final int RADIATION_INTERVAL_TICKS = 20;
    private static final float RADIATION_DAMAGE = 2.0F;

    private RadiationEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.tickCount % RADIATION_INTERVAL_TICKS != 0) {
            return;
        }
        if (hasExposedRadioactivePlutonium(player)) {
            player.hurt(player.damageSources().magic(), RADIATION_DAMAGE);
            player.displayClientMessage(
                    Component.translatable("message.enrichment_solution.radiation_warning").withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    public static boolean hasExposedRadioactivePlutonium(Player player) {
        Inventory inv = player.getInventory();
        return containsRadioactive(inv.items)
                || containsRadioactive(inv.offhand)
                || containsRadioactive(inv.armor);
    }

    private static boolean containsRadioactive(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && isRadioactivePlutonium(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRadioactivePlutonium(ItemStack stack) {
        return stack.is(ModItems.RAW_OXIDATED_PLUTONIUM.get())
                || stack.is(ModItems.OXIDATED_PLUTONIUM_INGOT.get())
                || stack.is(ModBlocks.OXIDATED_PLUTONIUM_ORE.asItem())
                || stack.is(ModBlocks.DEEPSLATE_OXIDATED_PLUTONIUM_ORE.asItem());
    }
}
