package com.enrichmentsolution.mod.client.ctm;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.registry.ModBlocks;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Declarative registry for Connected Texture (CTM) blocks.
 * Adding 47-tile CTM to a new block takes a single {@link #register} call.
 */
public final class CtmRegistry {

    private record Entry(Supplier<? extends Block> block, ResourceLocation ctmSprite, Predicate<BlockState> connectsTo) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean initialized = false;

    private CtmRegistry() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        register(ModBlocks.REACTOR_CASING, "block/reactor_casing_ctm");
    }

    /**
     * Registers a block for 47-tile CTM connecting only to itself.
     */
    public static void register(Supplier<? extends Block> block, String spritePath) {
        register(block, spritePath, state -> state.is(block.get()));
    }

    /**
     * Registers a block for 47-tile CTM with a custom connection predicate.
     */
    public static void register(Supplier<? extends Block> block, String spritePath, Predicate<BlockState> connectsTo) {
        ENTRIES.add(new Entry(block, ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, spritePath), connectsTo));
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        init();
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (Entry entry : ENTRIES) {
            Block block = entry.block().get();
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation mrl = BlockModelShaper.stateToModelLocation(state);
                BakedModel existing = models.get(mrl);
                if (existing != null && !(existing instanceof ConnectedTextureBakedModel)) {
                    models.put(mrl, new ConnectedTextureBakedModel(existing, entry.ctmSprite(), entry.connectsTo()));
                }
            }
        }
    }
}
