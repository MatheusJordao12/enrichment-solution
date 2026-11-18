package com.enrichmentsolution.mod.common.block.entity.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Per-level cache of pre-computed conduit routes.
 *
 * <p>Every pipe/cable owns one entry: the list of blocks it can reach through its network, already
 * filtered by the push side configuration of the last conduit on the path. The entry is rebuilt
 * lazily, the first time it is requested after an invalidation, so the server tick only iterates a
 * short list instead of walking the whole network block by block.
 *
 * <p>Invalidation is a single generation bump per level: every cached entry becomes stale at once
 * and is rebuilt on demand. Only conduits that actually carry something rebuild, so a network
 * change costs nothing until the next transfer tick.
 *
 * <p>The cache is keyed weakly by {@link Level} and its values never reference the level, so
 * unloading a dimension drops its routes together with the level itself.
 *
 * @param <T> delivery target type stored by the owning conduit, e.g. {@link ConduitTarget}
 */
public final class ConduitNetworkCache<T> {

    private final Map<Level, LevelView<T>> viewsByLevel = new WeakHashMap<>();

    /**
     * Returns the cached route list of the conduit at {@code origin}, scanning the network with
     * {@code scanner} when the cache is empty or stale.
     *
     * @param level   level that owns the conduit, used as the cache partition key
     * @param origin  position of the conduit asking for its routes
     * @param scanner rebuilds the route list, only called when needed
     */
    public List<T> routes(Level level, BlockPos origin, Supplier<List<T>> scanner) {
        LevelView<T> view = viewsByLevel.get(level);
        if (view == null) {
            view = new LevelView<>();
            viewsByLevel.put(level, view);
        }
        BlockPos key = origin.immutable();
        Route<T> route = view.routes.get(key);
        if (route == null || route.generation() != view.generation) {
            route = new Route<>(view.generation, List.copyOf(scanner.get()));
            view.routes.put(key, route);
        }
        return route.targets();
    }

    /**
     * Marks every cached route of {@code level} stale. Called on block placement/removal, on
     * network shape changes and on side configuration changes.
     */
    public void invalidate(Level level) {
        LevelView<T> view = viewsByLevel.get(level);
        if (view != null) {
            view.generation++;
            view.routes.clear();
        }
    }

    private record Route<E>(long generation, List<E> targets) {
    }

    private static final class LevelView<E> {
        private long generation;
        private final Map<BlockPos, Route<E>> routes = new HashMap<>();
    }
}
