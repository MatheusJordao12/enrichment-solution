package com.enrichmentsolution.mod.client;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.network.InspectRequestPayload;
import com.enrichmentsolution.mod.common.network.InspectResponsePayload;
import com.enrichmentsolution.mod.common.network.ModNetwork;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Head-slot goggles HUD: while worn, points the crosshair at a fluid pipe or energy cable
 * (up to 4 blocks) and shows what is inside — fluid name, mB, temperature / multiplier or FE.
 * <p>
 * This is the client half of the inspector. It is registered as the network sink by
 * {@link ClientSetup#onClientSetup}, so {@code common/network/ModNetwork} stays free of client
 * classes and a dedicated server never loads this file.
 */
@EventBusSubscriber(modid = EnrichmentSolutionMod.MOD_ID, value = Dist.CLIENT)
public final class InspectHud {

    private static final int REFRESH_TICKS = 4;
    private static final int LINGER_TICKS = 20;

    @Nullable
    private static InspectResponsePayload current;
    private static int life;

    /** Sink for {@code ModNetwork}: called on the client thread with the server's answer. */
    public static void accept(InspectResponsePayload payload) {
        current = payload;
        life = LINGER_TICKS;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (life > 0) {
            life--;
        }
        if (player == null || minecraft.level == null) {
            current = null;
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.HI_TECH_GOGGLES.get())) {
            current = null;
            life = 0;
            return;
        }
        if (player.tickCount % REFRESH_TICKS != 0) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            current = null;
            return;
        }
        Vec3 location = hit.getLocation();
        if (player.getEyePosition().distanceToSqr(location) > ModNetwork.INSPECT_RANGE * ModNetwork.INSPECT_RANGE) {
            current = null;
            return;
        }
        PacketDistributor.sendToServer(new InspectRequestPayload(((BlockHitResult) hit).getBlockPos()));
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (current == null || current.isEmpty() || life <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;

        List<Component> lines = buildLines(current);
        if (lines.isEmpty()) {
            return;
        }
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        int x = graphics.guiWidth() / 2 + 14;
        int y = graphics.guiHeight() / 2 - 8;
        if (x + width + 8 > graphics.guiWidth()) {
            x = graphics.guiWidth() / 2 - width - 14;
        }
        graphics.fill(x - 4, y - 4, x + width + 4, y + lines.size() * 10 + 2, 0xC80B1016);
        graphics.fill(x - 4, y - 4, x - 3, y + lines.size() * 10 + 2, 0xFF22D3EE);

        int color = current.kind() == 2 ? 0xFFFDE68A : 0xFFA5F3FC;
        int lineY = y;
        for (Component line : lines) {
            graphics.drawString(font, line, x, lineY, color, false);
            lineY += 10;
        }
    }

    private static List<Component> buildLines(InspectResponsePayload payload) {
        List<Component> lines = new ArrayList<>(2);
        if (payload.kind() == 1) {
            if (payload.amount() <= 0) {
                lines.add(Component.translatable("hud.enrichment_solution.inspect.empty"));
                return lines;
            }
            lines.add(Component.translatable("hud.enrichment_solution.inspect.fluid",
                    Component.translatable(payload.fluidKey()),
                    String.format("%,d", payload.amount()),
                    String.format("%,d", payload.capacity())));
            lines.add(Component.translatable("hud.enrichment_solution.inspect.temperature",
                    String.format("%.1f", payload.temperatureTenths() / 10.0D),
                    String.format("%.2f", payload.multiplierHundredths() / 100.0D)));
        } else if (payload.kind() == 2) {
            lines.add(Component.translatable("hud.enrichment_solution.inspect.energy",
                    String.format("%,d", payload.energy()),
                    String.format("%,d", payload.energyMax())));
        }
        return lines;
    }

    private InspectHud() {}
}
