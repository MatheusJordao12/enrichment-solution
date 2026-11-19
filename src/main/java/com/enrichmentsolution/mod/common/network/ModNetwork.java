package com.enrichmentsolution.mod.common.network;

import com.enrichmentsolution.mod.common.block.entity.EnergyCableBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.FluidPipeBlockEntity;
import com.enrichmentsolution.mod.common.entity.RobotEntity;
import com.enrichmentsolution.mod.common.fluid.ThermalFluidHelper;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Network glue for the goggles inspector: the client asks for the contents of the conduit
 * under the crosshair and the server answers with a small, already-shaped payload.
 * <p>
 * This class lives in {@code common} and must never touch client classes directly, otherwise a
 * dedicated server would try to load them. The HUD that consumes the answer is plugged in by the
 * client only, through {@link #setInspectSink(Consumer)}.
 */
public final class ModNetwork {

    /** Vanilla reach is 4.5 blocks; the goggles intentionally match a 4 block inspection range. */
    public static final double INSPECT_RANGE = 4.0D;

    /**
     * Receives the inspection answers on the client. Stays {@code null} on a dedicated server, which
     * makes {@link #handleResponse} a no-op there instead of loading {@code client.InspectHud}.
     */
    @Nullable
    private static volatile Consumer<InspectResponsePayload> inspectSink;

    /**
     * Registers the client-side consumer of inspection answers. Called by the client setup only;
     * passing {@code null} removes the sink (e.g. when leaving a world).
     */
    public static void setInspectSink(@Nullable Consumer<InspectResponsePayload> sink) {
        inspectSink = sink;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(InspectRequestPayload.TYPE, InspectRequestPayload.STREAM_CODEC, ModNetwork::handleRequest)
                .playToServer(RobotConfigPayload.TYPE, RobotConfigPayload.STREAM_CODEC, ModNetwork::handleRobotConfig)
                .playToClient(InspectResponsePayload.TYPE, InspectResponsePayload.STREAM_CODEC, ModNetwork::handleResponse);
    }

    private static void handleRobotConfig(RobotConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.level().getEntity(payload.entityId()) instanceof RobotEntity robot && robot.isOwner(player)) {
                robot.setMiningTargetY(payload.targetY());
                robot.setDigChunks(payload.digChunks());
                robot.setChunkRadius(payload.chunkRadius());
            }
        });
    }

    private static void handleRequest(InspectRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PacketDistributor.sendToPlayer(player, inspect(player, payload.pos()));
        });
    }

    private static void handleResponse(InspectResponsePayload payload, IPayloadContext context) {
        Consumer<InspectResponsePayload> sink = inspectSink;
        if (sink == null) {
            // Dedicated server (or client not initialized yet): nothing to render here.
            return;
        }
        context.enqueueWork(() -> sink.accept(payload));
    }

    private static InspectResponsePayload inspect(ServerPlayer player, BlockPos pos) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(ModItems.HI_TECH_GOGGLES.get())) {
            return InspectResponsePayload.NONE;
        }
        double distanceSqr = player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (distanceSqr > INSPECT_RANGE * INSPECT_RANGE) {
            return InspectResponsePayload.NONE;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            FluidStack fluid = pipe.getStoredFluid();
            if (fluid.isEmpty()) {
                return new InspectResponsePayload(1, "", 0, pipe.getTankCapacity(), 0, 0, 0, 0);
            }
            String key = fluid.getFluid().getFluidType().getDescriptionId();
            return InspectResponsePayload.fluid(
                    key,
                    fluid.getAmount(),
                    pipe.getTankCapacity(),
                    ThermalFluidHelper.getTemperature(fluid),
                    ThermalFluidHelper.getMultiplier(fluid));
        }
        if (blockEntity instanceof EnergyCableBlockEntity cable) {
            return InspectResponsePayload.energy(cable.getStoredEnergy(), cable.getMaxEnergy());
        }
        return InspectResponsePayload.NONE;
    }

    private ModNetwork() {}
}
