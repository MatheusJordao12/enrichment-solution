package com.enrichmentsolution.mod.common.network;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: "what is inside the conduit at this position?" */
public record InspectRequestPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<InspectRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "inspect_request"));

    public static final StreamCodec<FriendlyByteBuf, InspectRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBlockPos(payload.pos()),
            buf -> new InspectRequestPayload(buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
