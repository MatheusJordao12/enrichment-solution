package com.enrichmentsolution.mod.client.render;

import com.enrichmentsolution.mod.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the hi-tech goggles on the player head.
 * <p>
 * The model is authored in head space (8x8x8 head centred on the origin), so after moving to the
 * head pivot the model origin must land at the head centre. {@code ItemRenderer} shifts every item
 * model by -0.5 blocks, so we offset by +0.5 (plus -0.25 for the pivot-to-centre distance):
 * {@code translate(0.5, 0.25, 0.5)}.
 */
public class GogglesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public GogglesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!stack.is(ModItems.HI_TECH_GOGGLES.get())) {
            return;
        }
        pose.pushPose();
        this.getParentModel().head.translateAndRotate(pose);
        // LivingEntityRenderer mirrors the entity model (scale -1,-1,1); undo it so the item model
        // is drawn upright, and move down from the neck pivot to the head centre.
        pose.translate(0.0D, -0.25D, 0.0D);
        pose.scale(-1.0F, -1.0F, 1.0F);
        // ItemRenderer shifts every item model by -0.5 blocks.
        pose.translate(0.5D, 0.5D, 0.5D);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                pose,
                buffer,
                player.level(),
                player.getId()
        );
        pose.popPose();
    }
}
