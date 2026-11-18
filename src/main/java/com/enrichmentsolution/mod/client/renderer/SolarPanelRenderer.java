package com.enrichmentsolution.mod.client.renderer;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.client.config.ModClientConfig;
import com.enrichmentsolution.mod.common.block.entity.SolarPanelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Animates basic and 2-panel advanced solar panels (sun tracking, sky scanning, and upgrade transformation).
 */
public class SolarPanelRenderer implements BlockEntityRenderer<SolarPanelBlockEntity> {

    public static final ModelResourceLocation HEAD_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/solar_panel_head")
    );
    public static final ModelResourceLocation ADVANCED_LEFT_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/solar_panel_advanced_left")
    );
    public static final ModelResourceLocation ADVANCED_RIGHT_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/solar_panel_advanced_right")
    );

    private static final float PIVOT_X = 8.0F / 16.0F;
    private static final float PIVOT_Y = 8.0F / 16.0F;
    private static final float PIVOT_Z = 7.5F / 16.0F;

    private static int animatedThisFrame = 0;

    public static void resetFrameCounter() {
        animatedThisFrame = 0;
    }

    public SolarPanelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            SolarPanelBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        BlockState state = blockEntity.getBlockState();
        BlockPos pos = blockEntity.getBlockPos();

        float animProgress = blockEntity.getUpgradeAnimationProgress(level, partialTick);
        boolean isTransforming = blockEntity.isAdvanced() && animProgress < 1.0F;

        int limit = ModClientConfig.getLimit().getMaxAnimated();
        boolean shouldAnimate = isTransforming || (limit > 0 && animatedThisFrame < limit);
        if (shouldAnimate && !isTransforming) {
            animatedThisFrame++;
        }

        float yawDeg = 0.0F;
        float tiltZDeg = 0.0F;
        float tiltXDeg = 0.0F;

        if (shouldAnimate) {
            float time = level.getGameTime() + partialTick;
            float phase = ((pos.getX() * 31 + pos.getZ() * 17) & 0xFF) * 0.05F;

            if (level.dimensionType().hasSkyLight() && level.isDay() && level.canSeeSky(pos.above())) {
                float sunAngle = level.getTimeOfDay(partialTick) * Mth.TWO_PI;
                tiltZDeg = Mth.clamp(Mth.sin(sunAngle) * 40.0F, -40.0F, 40.0F);
                tiltXDeg = 22.5F + Mth.sin(time * 0.04F + phase) * 3.0F;
                yawDeg = Mth.cos(time * 0.03F + phase) * 4.0F;
            } else {
                float scan = time * 0.045F + phase;
                yawDeg = Mth.sin(scan) * 28.0F;
                tiltZDeg = Mth.cos(scan * 0.7F) * 24.0F;
                tiltXDeg = 18.0F + Mth.sin(scan * 0.5F) * 12.0F;
            }
        }

        if (!blockEntity.isAdvanced()) {
            BakedModel headModel = mc.getModelManager().getModel(HEAD_MODEL);
            renderPart(poseStack, bufferSource, blockRenderer, state, headModel,
                    0.0F, 0.0F, yawDeg, tiltZDeg, tiltXDeg, packedLight, packedOverlay);
            return;
        }

        // Smooth cubic ease-out for the transformation into 2 panels
        float ease = 1.0F - (float) Math.pow(1.0F - animProgress, 3.0);
        float invEase = 1.0F - ease;

        // Left panel slides from center (-10/16) -> 0 and unfolds from +90° roll; Right slides +10/16 -> 0 and unfolds from -90°
        float leftSlideX = -10.0F / 16.0F * invEase;
        float rightSlideX = 10.0F / 16.0F * invEase;
        float liftY = Mth.sin(animProgress * Mth.PI) * 0.18F;
        float unfoldRoll = 90.0F * invEase;
        float spinYaw = 180.0F * invEase;

        BakedModel leftModel = mc.getModelManager().getModel(ADVANCED_LEFT_MODEL);
        BakedModel rightModel = mc.getModelManager().getModel(ADVANCED_RIGHT_MODEL);

        renderPart(poseStack, bufferSource, blockRenderer, state, leftModel,
                leftSlideX, liftY, yawDeg + spinYaw, tiltZDeg + unfoldRoll, tiltXDeg, packedLight, packedOverlay);
        renderPart(poseStack, bufferSource, blockRenderer, state, rightModel,
                rightSlideX, liftY, yawDeg - spinYaw, tiltZDeg - unfoldRoll, tiltXDeg, packedLight, packedOverlay);
    }

    private void renderPart(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockRenderDispatcher blockRenderer,
            BlockState state,
            BakedModel model,
            float offsetX,
            float offsetY,
            float yawDeg,
            float tiltZDeg,
            float tiltXDeg,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(PIVOT_X + offsetX, PIVOT_Y + offsetY, PIVOT_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
        poseStack.mulPose(Axis.ZP.rotationDegrees(tiltZDeg));
        poseStack.mulPose(Axis.XP.rotationDegrees(tiltXDeg));
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                state,
                model,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }
}
