package com.enrichmentsolution.mod.client.renderer;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.block.BeltDriveBlock;
import com.enrichmentsolution.mod.common.block.entity.BeltDriveBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the rotating drive shaft inside the {@link BeltDriveBlock} housing.
 */
public class BeltDriveRenderer implements BlockEntityRenderer<BeltDriveBlockEntity> {

    public static final ModelResourceLocation SHAFT_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/belt_drive_shaft")
    );

    public BeltDriveRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            BeltDriveBlockEntity blockEntity,
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

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.hasProperty(BeltDriveBlock.FACING)
                ? state.getValue(BeltDriveBlock.FACING)
                : Direction.NORTH;
        boolean active = state.hasProperty(BeltDriveBlock.ACTIVE)
                && state.getValue(BeltDriveBlock.ACTIVE);

        float rotY = switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> -90.0F;
            case WEST -> 180.0F;
            case NORTH -> 90.0F;
            default -> 0.0F;
        };

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        BakedModel shaftModel = mc.getModelManager().getModel(SHAFT_MODEL);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        if (active) {
            float spinDeg = ((level.getGameTime() + partialTick) * 12.0F) % 360.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(spinDeg));
        }
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                state,
                shaftModel,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }
}
