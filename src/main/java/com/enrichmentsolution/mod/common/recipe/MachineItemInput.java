package com.enrichmentsolution.mod.common.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * One item input of a {@link MachineRecipe}: an ingredient plus the amount that has to be present in a single slot.
 * <p>
 * JSON shape (the {@code count} field is optional and defaults to 1, tags are supported through the ingredient codec):
 * <pre>{@code
 * {"item": "enrichment_solution:steel_ingot", "count": 2}
 * }</pre>
 */
public record MachineItemInput(Ingredient ingredient, int count) {

    public static final MapCodec<MachineItemInput> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.MAP_CODEC_NONEMPTY.forGetter(MachineItemInput::ingredient),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(MachineItemInput::count)
    ).apply(instance, MachineItemInput::new));

    public static final Codec<MachineItemInput> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineItemInput> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, MachineItemInput::ingredient,
            ByteBufCodecs.VAR_INT, MachineItemInput::count,
            MachineItemInput::new
    );

    public MachineItemInput {
        if (count < 1) {
            throw new IllegalArgumentException("Machine item input count must be positive");
        }
    }

    /** Whether the given slot stack satisfies this input (same ingredient and at least {@link #count} items). */
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() >= this.count && this.ingredient.test(stack);
    }

    /** Item stack used to render this input in recipe viewers. */
    public ItemStack displayStack() {
        ItemStack[] items = this.ingredient.getItems();
        if (items.length == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items[0].copy();
        stack.setCount(Math.min(this.count, stack.getMaxStackSize()));
        return stack;
    }
}
