package com.enrichmentsolution.mod.client.model;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.entity.RobotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RobotModel extends HierarchicalModel<RobotEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "robot"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart jetpack;

    public RobotModel(ModelPart root) {
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.head = this.body.getChild("head");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.jetpack = this.body.getChild("jetpack");
    }

    public void translateToRightHand(PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.body.translateAndRotate(poseStack);
        this.rightArm.translateAndRotate(poseStack);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition wheels = root.addOrReplaceChild("wheels", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, 0.0F));

        wheels.addOrReplaceChild("right_wheel", CubeListBuilder.create()
                        .texOffs(58, 25).addBox(-1.0F, -3.0F, -3.0F, 2.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(58, 55).addBox(0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(6.0F, 0.0F, 0.0F));

        wheels.addOrReplaceChild("left_wheel", CubeListBuilder.create()
                        .texOffs(76, 25).addBox(-1.0F, -3.0F, -3.0F, 2.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(66, 55).addBox(-1.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-6.0F, 0.0F, 0.0F));

        wheels.addOrReplaceChild("axle", CubeListBuilder.create()
                        .texOffs(58, 49).addBox(-5.0F, -1.0F, -1.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                        .texOffs(94, 0).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -4.0F, 0.0F));

        waist.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                        .texOffs(94, 19).addBox(-5.0F, -6.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -0.75F, -0.8F, 0.6981F, 0.0F, 0.0F));

        waist.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                        .texOffs(94, 8).addBox(-5.0F, -6.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -0.75F, 1.0F, -0.7854F, 0.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7.0F, -22.0F, -7.0F, 14.0F, 22.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -8.0F, 0.5F));

        body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(58, 39).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 38).addBox(-7.0F, -15.0F, -5.0F, 14.0F, 13.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -22.0F, 0.5F));

        body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(58, 0).addBox(-1.5F, -2.0F, -2.5F, 3.0F, 18.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(8.5F, -19.25F, 0.0F));

        body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(76, 0).addBox(-1.5F, -2.0F, -2.5F, 3.0F, 18.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-8.5F, -19.25F, 0.0F));

        body.addOrReplaceChild("jetpack", CubeListBuilder.create()
                        .texOffs(0, 74).addBox(-5.0F, -19.0F, 6.5F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 74).addBox(1.0F, -19.0F, 6.5F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 74).addBox(-2.0F, -14.5F, 6.5F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(RobotEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        if (entity.isCombatTransforming()) {
            this.animate(entity.combatTransformAnimationState, RobotAnimations.COMBAT_TRANSFORM, ageInTicks, 1.0F);
            float elapsedTicks = entity.combatTransformAnimationState.getAccumulatedTime() / 50.0F;
            this.jetpack.visible = elapsedTicks >= 10.0F;
            return;
        }

        if (entity.isDetransforming()) {
            this.animate(entity.detransformAnimationState, RobotAnimations.DETRANSFORM, ageInTicks, 1.0F);
            float elapsedTicks = entity.detransformAnimationState.getAccumulatedTime() / 50.0F;
            this.jetpack.visible = elapsedTicks < 30.0F;
            return;
        }

        if (entity.isBooting()) {
            this.jetpack.visible = false;
            this.animate(entity.summonBootAnimationState, RobotAnimations.SUMMON_BOOT, ageInTicks, 1.0F);
            return;
        }

        if (!entity.isPowered()) {
            this.jetpack.visible = entity.hasSword();
            if (entity.powerOffAnimationState.isStarted()) {
                this.animate(entity.powerOffAnimationState, RobotAnimations.POWER_OFF, ageInTicks, 1.0F);
            } else {
                this.body.xRot = 30.0F * Mth.DEG_TO_RAD;
                this.head.xRot = 24.0F * Mth.DEG_TO_RAD;
                this.rightArm.xRot = -25.0F * Mth.DEG_TO_RAD;
                this.leftArm.xRot = -25.0F * Mth.DEG_TO_RAD;
            }
            return;
        }

        this.jetpack.visible = entity.hasSword();

        if (entity.isDocked()) {
            this.jetpack.visible = entity.hasSword();
            this.animate(entity.dockAnimationState, RobotAnimations.DOCK, ageInTicks, 1.0F);
            return;
        }

        if (entity.isFlying()) {
            this.jetpack.visible = true;
            this.animate(entity.flyAnimationState, RobotAnimations.FLY, ageInTicks, 1.0F);
            return;
        }

        this.head.yRot = Mth.clamp(netHeadYaw, -35.0F, 35.0F) * Mth.DEG_TO_RAD;
        this.head.xRot = Mth.clamp(headPitch, -25.0F, 25.0F) * Mth.DEG_TO_RAD;

        this.animateWalk(RobotAnimations.WALK, limbSwing, limbSwingAmount, 2.0F, 2.5F);
        this.animate(entity.pickupAnimationState, RobotAnimations.PICKUP, ageInTicks, 1.0F);
        this.animate(entity.mineAnimationState, RobotAnimations.MINE, ageInTicks, 1.0F);
        this.animate(entity.attackAnimationState, RobotAnimations.ATTACK, ageInTicks, 1.0F);
    }
}
