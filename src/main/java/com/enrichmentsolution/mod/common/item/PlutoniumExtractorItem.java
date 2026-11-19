package com.enrichmentsolution.mod.common.item;

import com.enrichmentsolution.mod.registry.ModBlocks;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Handheld extractor that harvests {@link ModItems#RAW_OXIDATED_PLUTONIUM} from
 * Oxidated Plutonium Ore blocks, holding 1 unit safely inside its claws until deposited:
 * <ul>
 *     <li>Shift + Right-click with an {@link IsolatedBackpackItem} in the other hand deposits into that backpack.</li>
 *     <li>Shift + Right-click without an Isolated Backpack in the other hand deposits into the player's inventory.</li>
 *     <li>Right-clicking onto an Isolated Backpack in the inventory (bundle-style) deposits into that backpack.</li>
 * </ul>
 */
public class PlutoniumExtractorItem extends Item {

    public PlutoniumExtractorItem(Properties properties) {
        super(properties);
    }

    public static boolean isFilled(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.EXTRACTOR_FILLED.get()));
    }

    public static void setFilled(ItemStack stack, boolean filled) {
        if (filled) {
            stack.set(ModDataComponents.EXTRACTOR_FILLED.get(), true);
        } else {
            stack.remove(ModDataComponents.EXTRACTOR_FILLED.get());
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();

        if (player != null && player.isShiftKeyDown() && isFilled(stack)) {
            return deposit(level, player, ctx.getHand(), stack);
        }

        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);

        BlockState replacement;
        if (state.is(ModBlocks.OXIDATED_PLUTONIUM_ORE.get())) {
            replacement = Blocks.COBBLESTONE.defaultBlockState();
        } else if (state.is(ModBlocks.DEEPSLATE_OXIDATED_PLUTONIUM_ORE.get())) {
            replacement = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        } else {
            return InteractionResult.PASS;
        }

        if (isFilled(stack)) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(
                        Component.translatable("message.enrichment_solution.plutonium_extractor.full")
                                .withStyle(ChatFormatting.YELLOW),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }

        if (!level.isClientSide) {
            level.setBlock(pos, replacement, Block.UPDATE_ALL);
            setFilled(stack, true);
            if (player != null) {
                player.getInventory().setChanged();
            }

            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 1.2F);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && isFilled(stack)) {
            InteractionResult result = deposit(level, player, hand, stack);
            if (result.consumesAction()) {
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !isFilled(stack)) {
            return false;
        }
        ItemStack target = slot.getItem();
        if (!(target.getItem() instanceof IsolatedBackpackItem)) {
            return false;
        }
        return depositIntoBackpackInInventory(stack, target, slot, player);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !isFilled(stack) || !slot.allowModification(player)) {
            return false;
        }
        if (!(other.getItem() instanceof IsolatedBackpackItem)) {
            return false;
        }
        return depositIntoBackpackInInventory(stack, other, slot, player);
    }

    private static boolean depositIntoBackpackInInventory(ItemStack extractorStack, ItemStack backpackStack, Slot slot, Player player) {
        ItemStack plutonium = new ItemStack(ModItems.RAW_OXIDATED_PLUTONIUM.get());
        if (tryInsertIntoBackpack(backpackStack, plutonium)) {
            setFilled(extractorStack, false);
            slot.setChanged();
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
            if (!player.level().isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.enrichment_solution.plutonium_extractor.deposited_backpack")
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
            }
            return true;
        }
        if (!player.level().isClientSide) {
            player.displayClientMessage(
                    Component.translatable("message.enrichment_solution.plutonium_extractor.backpack_full")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
        return true;
    }

    private static InteractionResult deposit(Level level, Player player, InteractionHand hand, ItemStack extractorStack) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);
        ItemStack plutonium = new ItemStack(ModItems.RAW_OXIDATED_PLUTONIUM.get());

        if (otherStack.getItem() instanceof IsolatedBackpackItem) {
            if (tryInsertIntoBackpack(otherStack, plutonium)) {
                setFilled(extractorStack, false);
                player.getInventory().setChanged();
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 1.0F);
                player.displayClientMessage(
                        Component.translatable("message.enrichment_solution.plutonium_extractor.deposited_backpack")
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
                return InteractionResult.CONSUME;
            } else {
                player.displayClientMessage(
                        Component.translatable("message.enrichment_solution.plutonium_extractor.backpack_full")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResult.CONSUME;
            }
        }

        setFilled(extractorStack, false);
        if (!player.getInventory().add(plutonium)) {
            player.drop(plutonium, false);
        }
        player.getInventory().setChanged();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.0F);
        player.displayClientMessage(
                Component.translatable("message.enrichment_solution.plutonium_extractor.deposited_inventory")
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
        return InteractionResult.CONSUME;
    }

    public static boolean tryInsertIntoBackpack(ItemStack backpackStack, ItemStack toInsert) {
        if (!(backpackStack.getItem() instanceof IsolatedBackpackItem) || toInsert.isEmpty()) {
            return false;
        }
        ItemContainerContents contents = backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.copyInto(items);

        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack existing = items.get(slot);
            if (!existing.isEmpty()
                    && ItemStack.isSameItemSameComponents(existing, toInsert)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toMove = Math.min(space, toInsert.getCount());
                existing.grow(toMove);
                toInsert.shrink(toMove);
                backpackStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
                if (toInsert.isEmpty()) {
                    return true;
                }
            }
        }

        for (int slot = 0; slot < items.size(); slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, toInsert.copy());
                toInsert.setCount(0);
                backpackStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
                return true;
            }
        }

        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (isFilled(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.enrichment_solution.plutonium_extractor.status_filled")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.enrichment_solution.plutonium_extractor.status_empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltipComponents.add(Component.translatable("tooltip.enrichment_solution.plutonium_extractor.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
