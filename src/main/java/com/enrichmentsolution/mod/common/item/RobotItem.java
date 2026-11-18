package com.enrichmentsolution.mod.common.item;

import com.enrichmentsolution.mod.common.block.RobotChargerBlock;
import com.enrichmentsolution.mod.common.entity.RobotEntity;
import com.enrichmentsolution.mod.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class RobotItem extends Item {

    public static final String TAG_INVENTORY = "RobotInventory";
    public static final String TAG_AUTO_RECHARGER = "AutoRecharger";
    public static final String TAG_CHARGER_X = "ChargerX";
    public static final String TAG_CHARGER_Y = "ChargerY";
    public static final String TAG_CHARGER_Z = "ChargerZ";

    public RobotItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (level.getBlockState(clickedPos).getBlock() instanceof RobotChargerBlock) {
            if (!level.isClientSide) {
                CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
                CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
                tag.putInt(TAG_CHARGER_X, clickedPos.getX());
                tag.putInt(TAG_CHARGER_Y, clickedPos.getY());
                tag.putInt(TAG_CHARGER_Z, clickedPos.getZ());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                level.playSound(null, clickedPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.3F);
                if (player != null) {
                    player.displayClientMessage(Component.translatable(
                            "message.enrichment_solution.robot.charger_linked",
                            clickedPos.getX(), clickedPos.getY(), clickedPos.getZ()
                    ), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Direction face = context.getClickedFace();
        BlockState state = level.getBlockState(clickedPos);
        BlockPos spawnPos = state.getCollisionShape(level, clickedPos).isEmpty()
                ? clickedPos
                : clickedPos.relative(face);

        RobotEntity robot = ModEntities.ROBOT.get().create(serverLevel);
        if (robot == null) {
            return InteractionResult.FAIL;
        }

        double x = spawnPos.getX() + 0.5D;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5D;
        float yaw = player != null ? player.getYRot() + 180.0F : 0.0F;
        robot.moveTo(x, y, z, yaw, 0.0F);
        robot.setYHeadRot(yaw);

        if (player != null) {
            robot.setOwnerUUID(player.getUUID());
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(TAG_INVENTORY)) {
                robot.getInventory().deserializeNBT(serverLevel.registryAccess(), tag.getCompound(TAG_INVENTORY));
                if (robot.getInventory().getSlots() != RobotEntity.TOTAL_SLOTS) {
                    ItemStack[] preserved = new ItemStack[Math.min(robot.getInventory().getSlots(), RobotEntity.TOTAL_SLOTS)];
                    for (int i = 0; i < preserved.length; i++) {
                        preserved[i] = robot.getInventory().getStackInSlot(i);
                    }
                    robot.getInventory().setSize(RobotEntity.TOTAL_SLOTS);
                    for (int i = 0; i < preserved.length; i++) {
                        robot.getInventory().setStackInSlot(i, preserved[i]);
                    }
                }
                robot.syncMainHandPickaxe();
            }
            if (tag.contains(TAG_AUTO_RECHARGER)) {
                robot.setAutoRecharger(tag.getBoolean(TAG_AUTO_RECHARGER));
            }
            if (tag.contains(TAG_CHARGER_X) && tag.contains(TAG_CHARGER_Y) && tag.contains(TAG_CHARGER_Z)) {
                robot.setChargerPos(new BlockPos(
                        tag.getInt(TAG_CHARGER_X),
                        tag.getInt(TAG_CHARGER_Y),
                        tag.getInt(TAG_CHARGER_Z)
                ));
            }
        }

        robot.startSummonBoot();
        serverLevel.addFreshEntity(robot);

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return;
        }
        CompoundTag tag = customData.copyTag();
        if (tag.getBoolean(TAG_AUTO_RECHARGER)) {
            tooltipComponents.add(Component.translatable("tooltip.enrichment_solution.robot.auto_recharger")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (tag.contains(TAG_CHARGER_X) && tag.contains(TAG_CHARGER_Y) && tag.contains(TAG_CHARGER_Z)) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.enrichment_solution.robot.charger_linked",
                    tag.getInt(TAG_CHARGER_X),
                    tag.getInt(TAG_CHARGER_Y),
                    tag.getInt(TAG_CHARGER_Z)
            ).withStyle(ChatFormatting.GRAY));
        }
    }
}
