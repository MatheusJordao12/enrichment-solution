package com.enrichmentsolution.mod.common.network;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: contents of the inspected conduit.
 * {@code kind}: 0 = nothing, 1 = fluid pipe, 2 = energy cable.
 */
public record InspectResponsePayload(
        int kind,
        String fluidKey,
        int amount,
        int capacity,
        int temperatureTenths,
        int multiplierHundredths,
        int energy,
        int energyMax
) implements CustomPacketPayload {

    public static final InspectResponsePayload NONE =
            new InspectResponsePayload(0, "", 0, 0, 0, 0, 0, 0);

    public static final Type<InspectResponsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "inspect_response"));

    public static final StreamCodec<FriendlyByteBuf, InspectResponsePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeByte(payload.kind());
                buf.writeUtf(payload.fluidKey());
                buf.writeVarInt(payload.amount());
                buf.writeVarInt(payload.capacity());
                buf.writeVarInt(payload.temperatureTenths());
                buf.writeVarInt(payload.multiplierHundredths());
                buf.writeVarInt(payload.energy());
                buf.writeVarInt(payload.energyMax());
            },
            buf -> new InspectResponsePayload(
                    buf.readByte(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt())
    );

    public static InspectResponsePayload fluid(String fluidKey, int amount, int capacity,
                                               double temperature, double multiplier) {
        return new InspectResponsePayload(1, fluidKey, amount, capacity,
                (int) Math.round(temperature * 10.0D), (int) Math.round(multiplier * 100.0D), 0, 0);
    }

    public static InspectResponsePayload energy(int stored, int max) {
        return new InspectResponsePayload(2, "", 0, 0, 0, 0, stored, max);
    }

    public boolean isEmpty() {
        return kind == 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
