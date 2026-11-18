package com.enrichmentsolution.mod.client;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.registry.ModFluids;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector3f;

public final class ClientFluidExtensions {

    private static final ResourceLocation CRUDE_OIL_STILL =
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/fluid/crude_oil_still");
    private static final ResourceLocation CRUDE_OIL_FLOW =
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/fluid/crude_oil_flow");

    private static final ResourceLocation LIQUID_ISOBUTANE_STILL =
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/fluid/liquid_isobutane_still");
    private static final ResourceLocation LIQUID_ISOBUTANE_FLOW =
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/fluid/liquid_isobutane_flow");

    private static final ResourceLocation GASEOUS_ISOBUTANE_STILL =
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/fluid/gaseous_isobutane_still");
    private static final ResourceLocation GASEOUS_ISOBUTANE_FLOW =
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "block/fluid/gaseous_isobutane_flow");

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return CRUDE_OIL_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return CRUDE_OIL_FLOW;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                           int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(0.05F, 0.04F, 0.04F);
            }
        }, ModFluids.CRUDE_OIL_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return LIQUID_ISOBUTANE_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LIQUID_ISOBUTANE_FLOW;
            }

            @Override
            public int getTintColor() {
                return 0xE0D8F4FF;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                           int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(0.75F, 0.90F, 0.98F);
            }
        }, ModFluids.LIQUID_ISOBUTANE_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return GASEOUS_ISOBUTANE_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return GASEOUS_ISOBUTANE_FLOW;
            }

            @Override
            public int getTintColor() {
                return 0xA0E8FCFF;
            }
        }, ModFluids.GASEOUS_ISOBUTANE_TYPE.get());
    }

    private ClientFluidExtensions() {}
}
