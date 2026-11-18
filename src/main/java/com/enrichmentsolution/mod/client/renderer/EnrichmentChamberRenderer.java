package com.enrichmentsolution.mod.client.renderer;

import com.enrichmentsolution.mod.common.block.EnrichmentChamberBlock;
import com.enrichmentsolution.mod.common.block.EnrichmentChamberBlock.QuadRole;
import com.enrichmentsolution.mod.common.block.entity.EnrichmentChamberBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class EnrichmentChamberRenderer implements BlockEntityRenderer<EnrichmentChamberBlockEntity> {

    private static final float FORM_ANIM_TICKS = 22.0F;

    public EnrichmentChamberRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            EnrichmentChamberBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        var state = be.getBlockState();
        if (state.getValue(EnrichmentChamberBlock.QUAD_ROLE) != QuadRole.MASTER) {
            return;
        }
        long startTick = be.getQuadFormAnimStartTick();
        if (startTick < 0L || be.getLevel() == null) {
            return;
        }
        float elapsed = (be.getLevel().getGameTime() - startTick) + partialTick;
        if (elapsed < 0.0F || elapsed > FORM_ANIM_TICKS) {
            return;
        }

        float t = elapsed / FORM_ANIM_TICKS;
        float pulse = (float) Math.sin(t * Math.PI);
        float expand = 0.25F * (1.0F - t);

        Direction facing = state.getValue(EnrichmentChamberBlock.FACING);
        Direction right = facing.getClockWise();

        double minX = Math.min(0.0, right.getStepX()) - expand;
        double maxX = Math.max(1.0, 1.0 + right.getStepX()) + expand;
        double minY = 0.0 - expand;
        double maxY = 2.0 + expand;
        double minZ = Math.min(0.0, right.getStepZ()) - expand;
        double maxZ = Math.max(1.0, 1.0 + right.getStepZ()) + expand;

        poseStack.pushPose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(
                poseStack,
                consumer,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                0.35F, 1.0F, 0.45F, pulse
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(EnrichmentChamberBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2.0);
    }
}
