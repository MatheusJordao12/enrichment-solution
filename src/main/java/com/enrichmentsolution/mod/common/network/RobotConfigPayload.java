package com.enrichmentsolution.mod.common.network;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: updates the robot's mining configuration (target Y, dig chunks, chunk radius). */
public record RobotConfigPayload(int entityId, int targetY, boolean digChunks, int chunkRadius)
        implements CustomPacketPayload {

    public static final Type<RobotConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "robot_config"));

    public static final StreamCodec<FriendlyByteBuf, RobotConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.entityId());
                buf.writeInt(payload.targetY());
                buf.writeBoolean(payload.digChunks());
                buf.writeInt(payload.chunkRadius());
            },
            buf -> new RobotConfigPayload(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
