package com.enrichmentsolution.mod.client.renderer;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.block.PcbPrinterBlock;
import com.enrichmentsolution.mod.common.block.entity.PcbPrinterBlockEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Renders the moving pen head and conditional PCB board on the PCB Printer.
 */
public class PcbPrinterRenderer implements BlockEntityRenderer<PcbPrinterBlockEntity> {

    public static final ModelResourceLocation PEN_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/pcb_printer_pen")
    );
    public static final ModelResourceLocation PCB_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/pcb_printer_pcb")
    );

    public PcbPrinterRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            PcbPrinterBlockEntity blockEntity,
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
        var state = blockEntity.getBlockState();
        Direction facing = state.hasProperty(PcbPrinterBlock.FACING) ? state.getValue(PcbPrinterBlock.FACING) : Direction.NORTH;

        float rotY = switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        // Render PCB on the bed only while printing (after Start is clicked)
        if (blockEntity.isPrinting()) {
            BakedModel pcbModel = mc.getModelManager().getModel(PCB_MODEL);
            blockRenderer.getModelRenderer().renderModel(
                    poseStack.last(),
                    bufferSource.getBuffer(RenderType.cutout()),
                    state,
                    pcbModel,
                    1.0F, 1.0F, 1.0F,
                    packedLight,
                    packedOverlay
            );
        }

        // Render moving pen group
        poseStack.pushPose();
        if (blockEntity.isPrinting()) {
            float time = level.getGameTime() + partialTick;
            float scanX = (0.5F + 0.5F * Mth.sin(time * 0.28F)) * (5.5F / 16.0F);
            poseStack.translate(scanX, 0.0F, 0.0F);
        }
        BakedModel penModel = mc.getModelManager().getModel(PEN_MODEL);
        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                state,
                penModel,
                1.0F, 1.0F, 1.0F,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PcbPrinterBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(0.5D, 1.0D, 0.5D);
    }
}
