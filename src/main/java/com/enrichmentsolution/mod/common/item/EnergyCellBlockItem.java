package com.enrichmentsolution.mod.common.item;

import com.enrichmentsolution.mod.common.block.EnergyCellBlock;
import com.enrichmentsolution.mod.common.block.EnergyCellTier;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * BlockItem for Energy Cells that displays stored and maximum FE in its tooltip.
 */
public class EnergyCellBlockItem extends BlockItem {

    private final EnergyCellTier tier;

    public EnergyCellBlockItem(EnergyCellBlock block, Properties properties) {
        super(block, properties);
        this.tier = block.getTier();
    }

    public EnergyCellTier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        int stored = stack.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0);
        tooltipComponents.add(Component.translatable(
                "tooltip.enrichment_solution.energy",
                String.format("%,d", stored),
                String.format("%,d", tier.getCapacity())
        ).withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.translatable(
                "tooltip.enrichment_solution.energy_cell.transfer",
                String.format("%,d", tier.getTransferRate())
        ).withStyle(ChatFormatting.GRAY));
    }
}
