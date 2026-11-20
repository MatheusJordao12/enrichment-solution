package com.enrichmentsolution.mod.client.renderer;

import com.enrichmentsolution.mod.common.block.ConveyorBeltBlock;
import com.enrichmentsolution.mod.common.block.entity.ConveyorBeltBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the item on a {@link ConveyorBeltBlockEntity} lying flat like an Item Frame
 * and sliding smoothly along the belt's travel direction.
 */
public class ConveyorBeltRenderer implements BlockEntityRenderer<ConveyorBeltBlockEntity> {

    public ConveyorBeltRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            ConveyorBeltBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack stack = blockEntity.getStoredItem();
        if (stack.isEmpty() || blockEntity.getLevel() == null) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.hasProperty(ConveyorBeltBlock.FACING)
                ? state.getValue(ConveyorBeltBlock.FACING)
                : Direction.NORTH;
        Direction moveDir = blockEntity.isReverse() ? facing.getOpposite() : facing;

        float t = Mth.lerp(partialTick, blockEntity.getPrevProgress(), blockEntity.getProgress());
        float offset = t - 0.5F;

        double x = 0.5D + moveDir.getStepX() * offset;
        double y = 15.25D / 16.0D;
        double z = 0.5D + moveDir.getStepZ() * offset;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        // Lay flat on the belt like a horizontal Item Frame
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(facing.toYRot()));
        poseStack.scale(0.5F, 0.5F, 0.5F);

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0
        );

        poseStack.popPose();
    }
}
