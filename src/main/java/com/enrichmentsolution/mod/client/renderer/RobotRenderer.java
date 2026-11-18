package com.enrichmentsolution.mod.client.renderer;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.client.model.RobotModel;
import com.enrichmentsolution.mod.common.entity.RobotEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RobotRenderer extends MobRenderer<RobotEntity, RobotModel> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/entity/robot.png");
    private static final ResourceLocation OFF_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/entity/robot_off.png");
    private static final ResourceLocation WAKE_BLUE_MOUTH_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/entity/robot_wake_blue_mouth.png");
    private static final ResourceLocation WAKE_MOUTH_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/entity/robot_wake_mouth.png");
    private static final ResourceLocation COMBAT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EnrichmentSolutionMod.MOD_ID, "textures/entity/robot_combat.png");

    public RobotRenderer(EntityRendererProvider.Context context) {
        super(context, new RobotModel(context.bakeLayer(RobotModel.LAYER_LOCATION)), 0.55F);
        this.addLayer(new RobotHeldItemLayer(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(RobotEntity entity) {
        if (entity.isBooting()) {
            int elapsed = RobotEntity.BOOT_DURATION - entity.getBootTicks();
            if (elapsed < 18) {
                return OFF_TEXTURE;
            }
            if (elapsed < 32) {
                return WAKE_BLUE_MOUTH_TEXTURE;
            }
            return TEXTURE;
        }
        if (entity.isCombatTransforming()) {
            float elapsed = entity.combatTransformAnimationState.getAccumulatedTime() / 50.0F;
            if (elapsed < 32.0F) {
                return OFF_TEXTURE;
            }
            if (elapsed < 46.0F) {
                return WAKE_MOUTH_TEXTURE;
            }
            return COMBAT_TEXTURE;
        }
        if (entity.isDetransforming()) {
            float elapsed = entity.detransformAnimationState.getAccumulatedTime() / 50.0F;
            if (elapsed < 32.0F) {
                return OFF_TEXTURE;
            }
            if (elapsed < 46.0F) {
                return WAKE_BLUE_MOUTH_TEXTURE;
            }
            return TEXTURE;
        }
        if (!entity.isPowered()) {
            return OFF_TEXTURE;
        }
        return entity.hasSword() ? COMBAT_TEXTURE : TEXTURE;
    }
}
