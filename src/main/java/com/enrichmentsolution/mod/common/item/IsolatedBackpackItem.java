package com.enrichmentsolution.mod.common.item;

import com.enrichmentsolution.mod.common.menu.IsolatedBackpackMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Portable 27-slot (3x9) lead-lined backpack that stores items in {@code DataComponents.CONTAINER}
 * and isolates radioactive plutonium items inside so the carrier takes no radiation damage.
 */
public class IsolatedBackpackItem extends Item {

    public IsolatedBackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int slotIndex = hand == InteractionHand.MAIN_HAND
                    ? player.getInventory().selected
                    : Inventory.SLOT_OFFHAND;

            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, playerInventory, p) ->
                                    new IsolatedBackpackMenu(containerId, playerInventory, stack, slotIndex),
                            stack.getHoverName()
                    ),
                    buf -> buf.writeVarInt(slotIndex)
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.enrichment_solution.isolated_backpack.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
