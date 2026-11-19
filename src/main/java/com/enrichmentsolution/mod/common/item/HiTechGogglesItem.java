package com.enrichmentsolution.mod.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Hi-tech goggles.
 * <p>
 * Extends {@link ArmorItem} on purpose: vanilla's {@code CustomHeadLayer} skips head armor, so the
 * only thing rendered on the head is our own layer ({@code GogglesLayer}), which draws the
 * Blockbench item model directly in head space. That keeps the Blockbench coordinates as the
 * single source of truth for the fit.
 */
public class HiTechGogglesItem extends ArmorItem {

    public HiTechGogglesItem(Item.Properties properties) {
        super(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.enrichment_solution.hi_tech_goggles.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
