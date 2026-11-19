package com.enrichmentsolution.mod.common.world;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Optional;

/**
 * Persists the unique Destroyed Laboratory coordinates (2000-2500 blocks from world spawn)
 * and handles one-time placement when its area chunks load.
 */
public class LaboratorySavedData extends SavedData {

    private static final String DATA_NAME = "enrichment_solution_laboratory";
    private static final ResourceLocation STRUCTURE_ID =
            ResourceLocation.fromNamespaceAndPath(EnrichmentSolutionMod.MOD_ID, "laboratorio_destruido");
    public static final String LAB_MAP_TAG = "enrichment_solution_lab_map";

    private static final int MIN_DISTANCE = 2000;
    private static final int MAX_DISTANCE = 2500;
    private static final int SIZE_X = 22;
    private static final int SIZE_Z = 20;

    private int targetX;
    private int targetZ;
    private int placedY;
    private boolean placed;

    public LaboratorySavedData() {
    }

    public static LaboratorySavedData getOrCreate(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new Factory<>(
                        () -> createInitial(overworld),
                        LaboratorySavedData::load
                ),
                DATA_NAME
        );
    }

    private static LaboratorySavedData createInitial(ServerLevel overworld) {
        LaboratorySavedData data = new LaboratorySavedData();
        BlockPos spawn = overworld.getSharedSpawnPos();
        RandomSource random = RandomSource.create(overworld.getSeed() ^ 0x4C41425F534F4C55L);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        data.targetX = spawn.getX() + (int) Math.round(Math.cos(angle) * distance);
        data.targetZ = spawn.getZ() + (int) Math.round(Math.sin(angle) * distance);
        data.placed = false;
        data.placedY = 64;
        data.setDirty();
        return data;
    }

    public static LaboratorySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LaboratorySavedData data = new LaboratorySavedData();
        data.targetX = tag.getInt("TargetX");
        data.targetZ = tag.getInt("TargetZ");
        data.placedY = tag.getInt("PlacedY");
        data.placed = tag.getBoolean("Placed");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("TargetX", targetX);
        tag.putInt("TargetZ", targetZ);
        tag.putInt("PlacedY", placedY);
        tag.putBoolean("Placed", placed);
        return tag;
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetZ() {
        return targetZ;
    }

    public int getCenterX() {
        return targetX + SIZE_X / 2;
    }

    public int getCenterZ() {
        return targetZ + SIZE_Z / 2;
    }

    public boolean isPlaced() {
        return placed;
    }

    public boolean isNearChunk(int chunkX, int chunkZ) {
        int minChunkX = (targetX >> 4) - 1;
        int maxChunkX = ((targetX + SIZE_X) >> 4) + 1;
        int minChunkZ = (targetZ >> 4) - 1;
        int maxChunkZ = ((targetZ + SIZE_Z) >> 4) + 1;
        return chunkX >= minChunkX && chunkX <= maxChunkX && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }

    public void tryPlaceIfChunkLoaded(ServerLevel overworld) {
        if (placed) {
            return;
        }
        BlockPos cornerMin = new BlockPos(targetX, 64, targetZ);
        BlockPos cornerMax = new BlockPos(targetX + SIZE_X, 64, targetZ + SIZE_Z);
        if (!overworld.isLoaded(cornerMin) || !overworld.isLoaded(cornerMax)
                || !overworld.isLoaded(new BlockPos(targetX + SIZE_X, 64, targetZ))
                || !overworld.isLoaded(new BlockPos(targetX, 64, targetZ + SIZE_Z))) {
            return;
        }

        Optional<StructureTemplate> templateOpt = overworld.getStructureManager().get(STRUCTURE_ID);
        if (templateOpt.isEmpty()) {
            EnrichmentSolutionMod.LOGGER.warn("Destroyed laboratory structure template {} not found", STRUCTURE_ID);
            return;
        }

        int surfaceY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getCenterX(), getCenterZ());
        int y = Math.max(overworld.getMinBuildHeight() + 4, surfaceY - 1);
        BlockPos origin = new BlockPos(targetX, y, targetZ);

        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(false);
        templateOpt.get().placeInWorld(overworld, origin, origin, settings, overworld.getRandom(), 2);

        placedY = y;
        placed = true;
        setDirty();
        EnrichmentSolutionMod.LOGGER.info("Placed unique Destroyed Laboratory at ({}, {}, {})", targetX, y, targetZ);
    }

    public static ItemStack createLabExplorerMap(ServerLevel overworld) {
        LaboratorySavedData data = getOrCreate(overworld);
        int cx = data.getCenterX();
        int cz = data.getCenterZ();

        ItemStack map = MapItem.create(overworld, cx, cz, (byte) 2, true, true);
        MapItemSavedData.addTargetDecoration(map, new BlockPos(cx, 64, cz), "+", MapDecorationTypes.RED_X);
        map.set(DataComponents.ITEM_NAME, Component.translatable("item.enrichment_solution.lab_explorer_map"));

        CompoundTag custom = new CompoundTag();
        custom.putBoolean(LAB_MAP_TAG, true);
        map.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        return map;
    }

    public static boolean isLabExplorerMap(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().getBoolean(LAB_MAP_TAG);
    }
}
