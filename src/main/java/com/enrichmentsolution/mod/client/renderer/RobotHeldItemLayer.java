package com.enrichmentsolution.mod.client.renderer;

import com.enrichmentsolution.mod.client.model.RobotModel;
import com.enrichmentsolution.mod.common.entity.RobotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class RobotHeldItemLayer extends RenderLayer<RobotEntity, RobotModel> {

    private final ItemInHandRenderer itemInHandRenderer;

    public RobotHeldItemLayer(RenderLayerParent<RobotEntity, RobotModel> parent, ItemInHandRenderer itemInHandRenderer) {
        super(parent);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, RobotEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack stack = entity.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        getParentModel().translateToRightHand(poseStack);
        // Position at the bottom-front of the right arm (16px down from shoulder pivot)
        poseStack.translate(0.0F, 0.92F, -0.15F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        itemInHandRenderer.renderItem(entity, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
