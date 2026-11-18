package com.enrichmentsolution.mod.common.network;

import com.enrichmentsolution.mod.client.InspectHud;
import com.enrichmentsolution.mod.common.block.entity.EnergyCableBlockEntity;
import com.enrichmentsolution.mod.common.block.entity.FluidPipeBlockEntity;
import com.enrichmentsolution.mod.common.fluid.ThermalFluidHelper;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Network glue for the goggles inspector: the client asks for the contents of the conduit
 * under the crosshair and the server answers with a small, already-shaped payload.
 */
public final class ModNetwork {

    /** Vanilla reach is 4.5 blocks; the goggles intentionally match a 4 block inspection range. */
    public static final double INSPECT_RANGE = 4.0D;

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(InspectRequestPayload.TYPE, InspectRequestPayload.STREAM_CODEC, ModNetwork::handleRequest)
                .playToClient(InspectResponsePayload.TYPE, InspectResponsePayload.STREAM_CODEC, ModNetwork::handleResponse);
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
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                InspectHud.accept(payload);
            }
        });
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
