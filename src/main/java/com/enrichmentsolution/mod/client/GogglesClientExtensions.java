package com.enrichmentsolution.mod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Map;

/**
 * Suppresses the vanilla armor model for the goggles: the item extends ArmorItem so vanilla's
 * head-item layer skips it, and this empty humanoid model makes the armor layer draw nothing.
 * The actual 3D model is rendered by {@code GogglesLayer}.
 */
public final class GogglesClientExtensions implements IClientItemExtensions {

    private static HumanoidModel<?> emptyModel;

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                  EquipmentSlot slot, HumanoidModel<?> original) {
        if (emptyModel == null) {
            emptyModel = buildEmptyModel();
        }
        return emptyModel;
    }

    private static HumanoidModel<?> buildEmptyModel() {
        ModelPart empty = new ModelPart(List.of(), Map.of());
        ModelPart root = new ModelPart(List.of(), Map.of(
                "head", empty,
                "hat", new ModelPart(List.of(), Map.of()),
                "body", new ModelPart(List.of(), Map.of()),
                "right_arm", new ModelPart(List.of(), Map.of()),
                "left_arm", new ModelPart(List.of(), Map.of()),
                "right_leg", new ModelPart(List.of(), Map.of()),
                "left_leg", new ModelPart(List.of(), Map.of())
        ));
        return new HumanoidModel<LivingEntity>(root);
    }
}
