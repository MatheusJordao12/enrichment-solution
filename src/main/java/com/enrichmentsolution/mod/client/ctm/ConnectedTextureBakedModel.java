package com.enrichmentsolution.mod.client.ctm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Reusable dynamic baked model that renders 47-tile connected textures (CTM)
 * from a single 8x6 sprite sheet, while falling back to {@code original}
 * for items/inventory or non-culled quads.
 */
public class ConnectedTextureBakedModel implements IDynamicBakedModel {

    public static final ModelProperty<int[]> CTM_TILES_PROPERTY = new ModelProperty<>();

    private final BakedModelAccess original;
    private final ResourceLocation ctmSpriteLocation;
    private final Predicate<BlockState> connectsTo;

    public ConnectedTextureBakedModel(net.minecraft.client.resources.model.BakedModel original,
                                      ResourceLocation ctmSpriteLocation,
                                      Predicate<BlockState> connectsTo) {
        this.original = new BakedModelAccess(original);
        this.ctmSpriteLocation = ctmSpriteLocation;
        this.connectsTo = connectsTo;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level,
                                           @NotNull BlockPos pos,
                                           @NotNull BlockState state,
                                           @NotNull ModelData modelData) {
        int[] tiles = CtmLookup.computeFaceTiles(level, pos, connectsTo);
        return modelData.derive().with(CTM_TILES_PROPERTY, tiles).build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state,
                                             @Nullable Direction side,
                                             @NotNull RandomSource rand,
                                             @NotNull ModelData extraData,
                                             @Nullable RenderType renderType) {
        if (side == null) {
            return Collections.emptyList();
        }
        int[] tiles = extraData.get(CTM_TILES_PROPERTY);
        if (tiles == null || state == null) {
            return original.delegate().getQuads(state, side, rand, extraData, renderType);
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ctmSpriteLocation);

        int tileIndex = tiles[side.get3DDataValue()];
        return List.of(buildFaceQuad(side, sprite, tileIndex));
    }

    private static BakedQuad buildFaceQuad(Direction side, TextureAtlasSprite sprite, int tileIndex) {
        int col = tileIndex % CtmLookup.ATLAS_COLS;
        int row = tileIndex / CtmLookup.ATLAS_COLS;

        float uSize = (sprite.getU1() - sprite.getU0()) / CtmLookup.ATLAS_COLS;
        float vSize = (sprite.getV1() - sprite.getV0()) / CtmLookup.ATLAS_ROWS;
        float u0 = sprite.getU0() + col * uSize;
        float u1 = u0 + uSize;
        float v0 = sprite.getV0() + row * vSize;
        float v1 = v0 + vSize;

        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
        builder.setSprite(sprite);
        builder.setDirection(side);
        builder.setTintIndex(-1);
        builder.setShade(true);
        builder.setHasAmbientOcclusion(true);

        float nx = side.getStepX();
        float ny = side.getStepY();
        float nz = side.getStepZ();

        switch (side) {
            case NORTH -> {
                putVertex(builder, 1, 1, 0, u0, v0, nx, ny, nz);
                putVertex(builder, 1, 0, 0, u0, v1, nx, ny, nz);
                putVertex(builder, 0, 0, 0, u1, v1, nx, ny, nz);
                putVertex(builder, 0, 1, 0, u1, v0, nx, ny, nz);
            }
            case SOUTH -> {
                putVertex(builder, 0, 1, 1, u0, v0, nx, ny, nz);
                putVertex(builder, 0, 0, 1, u0, v1, nx, ny, nz);
                putVertex(builder, 1, 0, 1, u1, v1, nx, ny, nz);
                putVertex(builder, 1, 1, 1, u1, v0, nx, ny, nz);
            }
            case WEST -> {
                putVertex(builder, 0, 1, 0, u0, v0, nx, ny, nz);
                putVertex(builder, 0, 0, 0, u0, v1, nx, ny, nz);
                putVertex(builder, 0, 0, 1, u1, v1, nx, ny, nz);
                putVertex(builder, 0, 1, 1, u1, v0, nx, ny, nz);
            }
            case EAST -> {
                putVertex(builder, 1, 1, 1, u0, v0, nx, ny, nz);
                putVertex(builder, 1, 0, 1, u0, v1, nx, ny, nz);
                putVertex(builder, 1, 0, 0, u1, v1, nx, ny, nz);
                putVertex(builder, 1, 1, 0, u1, v0, nx, ny, nz);
            }
            case UP -> {
                putVertex(builder, 0, 1, 0, u0, v0, nx, ny, nz);
                putVertex(builder, 0, 1, 1, u0, v1, nx, ny, nz);
                putVertex(builder, 1, 1, 1, u1, v1, nx, ny, nz);
                putVertex(builder, 1, 1, 0, u1, v0, nx, ny, nz);
            }
            case DOWN -> {
                putVertex(builder, 0, 0, 1, u0, v0, nx, ny, nz);
                putVertex(builder, 0, 0, 0, u0, v1, nx, ny, nz);
                putVertex(builder, 1, 0, 0, u1, v1, nx, ny, nz);
                putVertex(builder, 1, 0, 1, u1, v0, nx, ny, nz);
            }
        }

        return builder.bakeQuad();
    }

    private static void putVertex(QuadBakingVertexConsumer builder, float x, float y, float z,
                                  float u, float v, float nx, float ny, float nz) {
        builder.addVertex(x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setUv2(0, 0)
                .setNormal(nx, ny, nz);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return original.delegate().useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return original.delegate().isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return original.delegate().usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return original.delegate().getParticleIcon();
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return original.delegate().getTransforms();
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return original.delegate().getOverrides();
    }

    private record BakedModelAccess(net.minecraft.client.resources.model.BakedModel delegate) {
    }
}
