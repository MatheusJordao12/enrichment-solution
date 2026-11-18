package com.enrichmentsolution.mod.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * Serializer for one {@link MachineRecipeKind}. The kind is fixed by the registered instance, so it does not appear in
 * the JSON (the {@code type} field already selects it).
 */
public class MachineRecipeSerializer implements RecipeSerializer<MachineRecipe> {

    private final MachineRecipeKind kind;
    private final MapCodec<MachineRecipe> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec;

    public MachineRecipeSerializer(MachineRecipeKind kind) {
        this.kind = kind;
        this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                MachineItemInput.CODEC.listOf().optionalFieldOf("item_inputs", List.of()).forGetter(MachineRecipe::itemInputs),
                FluidStack.OPTIONAL_CODEC.optionalFieldOf("fluid_input", FluidStack.EMPTY).forGetter(MachineRecipe::fluidInput),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("item_output", ItemStack.EMPTY).forGetter(MachineRecipe::itemOutput),
                FluidStack.OPTIONAL_CODEC.optionalFieldOf("fluid_output", FluidStack.EMPTY).forGetter(MachineRecipe::fluidOutput),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("energy_per_tick", 0).forGetter(MachineRecipe::energyPerTick),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("ticks", 1).forGetter(MachineRecipe::ticks)
        ).apply(instance, (itemInputs, fluidInput, itemOutput, fluidOutput, energyPerTick, ticks) ->
                new MachineRecipe(kind, itemInputs, fluidInput, itemOutput, fluidOutput, energyPerTick, ticks)));

        this.streamCodec = StreamCodec.composite(
                MachineItemInput.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::itemInputs,
                FluidStack.OPTIONAL_STREAM_CODEC, MachineRecipe::fluidInput,
                ItemStack.OPTIONAL_STREAM_CODEC, MachineRecipe::itemOutput,
                FluidStack.OPTIONAL_STREAM_CODEC, MachineRecipe::fluidOutput,
                ByteBufCodecs.VAR_INT, MachineRecipe::energyPerTick,
                ByteBufCodecs.VAR_INT, MachineRecipe::ticks,
                (itemInputs, fluidInput, itemOutput, fluidOutput, energyPerTick, ticks) ->
                        new MachineRecipe(kind, itemInputs, fluidInput, itemOutput, fluidOutput, energyPerTick, ticks)
        );
    }

    public MachineRecipeKind kind() {
        return this.kind;
    }

    @Override
    public MapCodec<MachineRecipe> codec() {
        return this.codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() {
        return this.streamCodec;
    }
}
