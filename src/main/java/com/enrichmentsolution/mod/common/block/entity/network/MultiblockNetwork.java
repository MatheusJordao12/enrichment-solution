package com.enrichmentsolution.mod.common.block.entity.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Shared BFS discovery, bounding-box clamping, master selection, break-split handling,
 * and member NBT persistence for connected multiblock structures.
 */
public final class MultiblockNetwork {

    private MultiblockNetwork() {
    }

    /**
     * Runs a BFS from {@code seedPos} over blocks matching {@code blockMatcher}, clamping
     * the bounding box on each axis to {@code maxSpan}. Returns the discovered positions
     * sorted deterministically by Y, then X, then Z (so index 0 is the canonical master).
     */
    public static List<BlockPos> discoverComponent(
            Level level,
            BlockPos seedPos,
            @Nullable BlockPos ignoredPos,
            int maxSpan,
            Predicate<BlockState> blockMatcher
    ) {
        if (level.isClientSide || seedPos.equals(ignoredPos) || !blockMatcher.test(level.getBlockState(seedPos))) {
            return List.of();
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> component = new ArrayList<>();

        visited.add(seedPos);
        queue.add(seedPos);

        int minX = seedPos.getX(), maxX = seedPos.getX();
        int minY = seedPos.getY(), maxY = seedPos.getY();
        int minZ = seedPos.getZ(), maxZ = seedPos.getZ();

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            component.add(current);

            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (next.equals(ignoredPos) || visited.contains(next)) {
                    continue;
                }
                if (!blockMatcher.test(level.getBlockState(next))) {
                    continue;
                }
                int nMinX = Math.min(minX, next.getX());
                int nMaxX = Math.max(maxX, next.getX());
                int nMinY = Math.min(minY, next.getY());
                int nMaxY = Math.max(maxY, next.getY());
                int nMinZ = Math.min(minZ, next.getZ());
                int nMaxZ = Math.max(maxZ, next.getZ());
                if (nMaxX - nMinX + 1 > maxSpan || nMaxY - nMinY + 1 > maxSpan || nMaxZ - nMinZ + 1 > maxSpan) {
                    continue;
                }
                minX = nMinX;
                maxX = nMaxX;
                minY = nMinY;
                maxY = nMaxY;
                minZ = nMinZ;
                maxZ = nMaxZ;
                visited.add(next);
                queue.add(next);
            }
        }

        component.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getZ));
        return component;
    }

    /**
     * Rebuilds every distinct connected component adjacent to {@code brokenPos},
     * tracking visited members so each split sub-network is rebuilt at most once.
     */
    public static <T extends BlockEntity> void handleBlockBroken(
            Level level,
            BlockPos brokenPos,
            Predicate<BlockState> blockMatcher,
            Class<T> entityClass,
            Rebuilder rebuildAction,
            MasterResolver<T> masterResolver,
            MembersGetter<T> membersGetter,
            Consumer<T> onComponentRebuilt
    ) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos adj = brokenPos.relative(dir);
            if (blockMatcher.test(level.getBlockState(adj))) {
                neighbors.add(adj);
            }
        }
        if (neighbors.isEmpty()) {
            return;
        }
        Set<BlockPos> processed = new HashSet<>();
        for (BlockPos neighbor : neighbors) {
            if (processed.contains(neighbor)) {
                continue;
            }
            rebuildAction.rebuild(level, neighbor, brokenPos);
            BlockEntity be = level.getBlockEntity(neighbor);
            if (entityClass.isInstance(be)) {
                T master = masterResolver.getMaster(entityClass.cast(be));
                if (master != null) {
                    processed.addAll(membersGetter.getMembers(master));
                    onComponentRebuilt.accept(master);
                }
            }
        }
    }

    /**
     * Resolves the master block entity for a member at {@code selfPos}.
     */
    @Nullable
    public static <T extends BlockEntity> T resolveMaster(
            @Nullable Level level,
            BlockPos selfPos,
            BlockPos masterPos,
            Class<T> entityClass,
            T self
    ) {
        if (selfPos.equals(masterPos)) {
            return self;
        }
        if (level != null) {
            BlockEntity be = level.getBlockEntity(masterPos);
            if (entityClass.isInstance(be)) {
                return entityClass.cast(be);
            }
        }
        return self;
    }

    public static void saveMembers(CompoundTag tag, BlockPos masterPos, int blockCount, List<BlockPos> members) {
        tag.put("MasterPos", NbtUtils.writeBlockPos(masterPos));
        tag.putInt("BlockCount", blockCount);
        ListTag memberList = new ListTag();
        for (BlockPos m : members) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", m.getX());
            entry.putInt("Y", m.getY());
            entry.putInt("Z", m.getZ());
            memberList.add(entry);
        }
        tag.put("Members", memberList);
    }

    public static void loadMembers(CompoundTag tag, BlockPos fallbackPos, List<BlockPos> membersOut) {
        membersOut.clear();
        if (tag.contains("Members", Tag.TAG_LIST)) {
            ListTag memberList = tag.getList("Members", Tag.TAG_COMPOUND);
            for (int i = 0; i < memberList.size(); i++) {
                CompoundTag entry = memberList.getCompound(i);
                membersOut.add(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
            }
        }
        if (membersOut.isEmpty()) {
            membersOut.add(fallbackPos);
        }
    }

    @FunctionalInterface
    public interface Rebuilder {
        void rebuild(Level level, BlockPos seedPos, @Nullable BlockPos ignoredPos);
    }

    @FunctionalInterface
    public interface MasterResolver<T extends BlockEntity> {
        @Nullable
        T getMaster(T member);
    }

    @FunctionalInterface
    public interface MembersGetter<T extends BlockEntity> {
        List<BlockPos> getMembers(T master);
    }
}
