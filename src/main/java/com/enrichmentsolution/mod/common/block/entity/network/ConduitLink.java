package com.enrichmentsolution.mod.common.block.entity.network;

import com.enrichmentsolution.mod.common.block.ConduitSideConfig;
import com.enrichmentsolution.mod.common.config.ModServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Shared conduit link traversal and pull/push helpers for pipe and cable block entities.
 */
public final class ConduitLink {

    private ConduitLink() {
    }

    /**
     * Pulls from adjacent non-conduit neighbors on sides where {@code config.canPull(dir)} is true.
     * Stops early if {@code puller} returns false (e.g. conduit buffer is full).
     */
    public static <C extends BlockEntity, H> void pullFromAdjacent(
            Level level,
            BlockPos pos,
            Class<C> conduitClass,
            ConduitSideConfig config,
            BlockCapability<H, Direction> capability,
            AdjacentPuller<H> puller
    ) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe == null || conduitClass.isInstance(neighborBe) || !config.canPull(dir)) {
                continue;
            }
            H source = level.getCapability(capability, neighborPos, dir.getOpposite());
            if (source != null && !puller.pull(neighborPos, dir, source)) {
                return;
            }
        }
    }

    /**
     * Walks the connected conduit network up to {@link ModServerConfig#CONDUIT_MAX_NETWORK_SCAN}
     * nodes and records every reachable non-conduit target whose exit side allows pushing.
     */
    public static <C extends BlockEntity, S, T> List<T> scanNetwork(
            Level level,
            BlockPos startPos,
            Class<C> conduitClass,
            Function<C, ConduitSideConfig> configGetter,
            S startState,
            BiFunction<S, C, S> stateStep,
            TargetFactory<S, T> targetFactory
    ) {
        List<T> targets = new ArrayList<>();
        Map<BlockPos, S> stateByPos = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        stateByPos.put(startPos, startState);
        visited.add(startPos);
        queue.add(startPos);

        int maxScan = ModServerConfig.get(ModServerConfig.CONDUIT_MAX_NETWORK_SCAN);
        while (!queue.isEmpty() && visited.size() <= maxScan) {
            BlockPos current = queue.poll();
            S currentState = stateByPos.getOrDefault(current, startState);
            BlockEntity currentBe = level.getBlockEntity(current);
            if (!conduitClass.isInstance(currentBe)) {
                continue;
            }
            C currentConduit = conduitClass.cast(currentBe);
            ConduitSideConfig currentConfig = configGetter.apply(currentConduit);

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (conduitClass.isInstance(neighborBe)) {
                    if (visited.add(neighborPos)) {
                        stateByPos.put(neighborPos, stateStep.apply(currentState, conduitClass.cast(neighborBe)));
                        queue.add(neighborPos);
                    }
                    continue;
                }
                if (neighborBe == null || !currentConfig.canPush(dir)) {
                    continue;
                }
                targets.add(targetFactory.create(neighborPos, dir.getOpposite(), currentState));
            }
        }
        return targets;
    }

    /**
     * Iterates over cached {@code targets}, resolving the block capability on each and invoking
     * {@code pusher} while {@code hasMore} remains true.
     */
    public static <T, H> void distribute(
            Level level,
            List<T> targets,
            Function<T, BlockPos> posGetter,
            Function<T, Direction> sideGetter,
            BlockCapability<H, Direction> capability,
            BooleanSupplier hasMore,
            TargetPusher<T, H> pusher
    ) {
        for (T target : targets) {
            if (!hasMore.getAsBoolean()) {
                return;
            }
            H receiver = level.getCapability(capability, posGetter.apply(target), sideGetter.apply(target));
            if (receiver != null) {
                pusher.push(target, receiver);
            }
        }
    }

    @FunctionalInterface
    public interface AdjacentPuller<H> {
        boolean pull(BlockPos neighborPos, Direction dir, H source);
    }

    @FunctionalInterface
    public interface TargetFactory<S, T> {
        T create(BlockPos pos, Direction accessSide, S state);
    }

    @FunctionalInterface
    public interface TargetPusher<T, H> {
        void push(T target, H receiver);
    }
}
