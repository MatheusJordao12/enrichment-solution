package com.enrichmentsolution.mod.common.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * In-game Guide Book item that opens the client-side {@code GuideBookScreen} when right-clicked.
 * Uses a client-registered opener callback so dedicated servers never load client screen classes.
 */
public class GuideBookItem extends Item {

    @Nullable
    private static volatile Runnable screenOpener;

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    /**
     * Registers the client-side screen opener. Must only be called from client setup.
     */
    public static void setScreenOpener(@Nullable Runnable opener) {
        screenOpener = opener;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            Runnable opener = screenOpener;
            if (opener != null) {
                opener.run();
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
