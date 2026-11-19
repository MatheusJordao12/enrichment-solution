package com.enrichmentsolution.mod.common.item;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * High-speed 3x3 FE-powered mining hammer (Diamond harvest tier, Diamond + Efficiency IV speed).
 * Sneaking mines a single 1x1 block for precision. Consumes FE per block instead of durability.
 */
public class HammerXItem extends PickaxeItem {

    public static final int ENERGY_CAPACITY = 256_000;
    public static final int ENERGY_TRANSFER = 4_000;
    public static final int ENERGY_PER_BLOCK = 100;

    /** Diamond pickaxe base speed (8.0F) + Efficiency IV bonus (4^2 + 1 = 17.0F). */
    public static final float POWERED_MINING_SPEED = 25.0F;

    private static final ThreadLocal<Boolean> MINING_AOE = ThreadLocal.withInitial(() -> false);

    public HammerXItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 4.0F, -2.8F)));
    }

    public static int getStoredEnergy(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0), 0, ENERGY_CAPACITY);
    }

    public static void setStoredEnergy(ItemStack stack, int energy) {
        int clamped = Mth.clamp(energy, 0, ENERGY_CAPACITY);
        if (clamped == 0) {
            stack.remove(ModDataComponents.STORED_ENERGY.get());
        } else {
            stack.set(ModDataComponents.STORED_ENERGY.get(), clamped);
        }
    }

    public static boolean hasEnergy(ItemStack stack, int amount) {
        return getStoredEnergy(stack) >= amount;
    }

    public static void consumeEnergy(ItemStack stack, int amount) {
        setStoredEnergy(stack, getStoredEnergy(stack) - amount);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!hasEnergy(stack, ENERGY_PER_BLOCK)) {
            return 1.0F;
        }
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? POWERED_MINING_SPEED : 1.0F;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getStoredEnergy(stack) / (float) ENERGY_CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00D4FF;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
            if (!(miningEntity instanceof Player player && player.isCreative())) {
                consumeEnergy(stack, ENERGY_PER_BLOCK);
            }
        }
        return true;
    }

    @EventBusSubscriber(modid = EnrichmentSolutionMod.MOD_ID)
    public static final class Events {
        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            if (MINING_AOE.get()) {
                return;
            }
            Player player = event.getPlayer();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof HammerXItem hammer) || player.isShiftKeyDown()) {
                return;
            }
            if (!player.isCreative() && !hasEnergy(stack, ENERGY_PER_BLOCK)) {
                return;
            }

            ServerLevel level = serverPlayer.serverLevel();
            BlockPos pos = event.getPos();
            BlockState centerState = event.getState();
            if (!centerState.is(BlockTags.MINEABLE_WITH_PICKAXE) || !hammer.isCorrectToolForDrops(stack, centerState)) {
                return;
            }
            float centerHardness = centerState.getDestroySpeed(level, pos);
            if (centerHardness < 0.0F) {
                return;
            }

            Direction face = getHitFace(level, player);
            List<BlockPos> targets = get3x3Positions(pos, face);

            MINING_AOE.set(true);
            try {
                for (BlockPos targetPos : targets) {
                    if (!player.isCreative() && !hasEnergy(stack, ENERGY_PER_BLOCK * 2)) {
                        break;
                    }
                    BlockState targetState = level.getBlockState(targetPos);
                    if (targetState.isAir()) {
                        continue;
                    }
                    float targetHardness = targetState.getDestroySpeed(level, targetPos);
                    if (targetHardness < 0.0F || (centerHardness > 0.0F && targetHardness > centerHardness * 3.0F)) {
                        continue;
                    }
                    if (!targetState.is(BlockTags.MINEABLE_WITH_PICKAXE) || !hammer.isCorrectToolForDrops(stack, targetState)) {
                        continue;
                    }
                    serverPlayer.gameMode.destroyBlock(targetPos);
                }
            } finally {
                MINING_AOE.set(false);
            }
        }
    }

    private static Direction getHitFace(Level level, Player player) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getDirection();
        }
        return Direction.orderedByNearest(player)[0].getOpposite();
    }

    public static List<BlockPos> get3x3Positions(BlockPos center, Direction face) {
        List<BlockPos> positions = new ArrayList<>(8);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                BlockPos offset = switch (face.getAxis()) {
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                    case X -> center.offset(0, b, a);
                };
                positions.add(offset);
            }
        }
        return positions;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        int stored = getStoredEnergy(stack);
        tooltipComponents.add(Component.translatable(
                "tooltip.enrichment_solution.energy",
                String.format("%,d", stored),
                String.format("%,d", ENERGY_CAPACITY)
        ).withStyle(stored >= ENERGY_PER_BLOCK ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltipComponents.add(Component.translatable("tooltip.enrichment_solution.hammer_x.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
