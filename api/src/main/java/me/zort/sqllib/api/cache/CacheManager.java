package me.zort.sqllib.api.cache;

import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.data.QueryResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A cache manager that caches query results. This is used
 * primarly in {@link me.zort.sqllib.api.SQLConnection} instances
 * that hold a cache manager.
 *
 * @author ZorTik
 */
public interface CacheManager {

    /**
     * Sets a query result to the cache.
     * If the query is already cached, it should be overwritten.
     *
     * @param query Query that was executed
     * @param result Result
     */
    void set(@NotNull Query query, @NotNull QueryResult result);

    /**
     * Returns a query result from the cache, or null if
     * the query is not cached.
     *
     * @param query Query that was executed
     * @param isExec Whether the query is an exec (no ResultSet) query
     * @return The nullable query result
     */
    @Nullable QueryResult get(@NotNull Query query, boolean isExec);

    /**
     * Returns a cache manager that does not cache anything.
     *
     * @return The cache manager
     */
    static CacheManager noCache() {
        return new CacheManager() {
            @Override
            public void set(@NotNull Query query, @NotNull QueryResult result) {}
            @Override
            public @Nullable QueryResult get(@NotNull Query query, boolean isExec) {
                return null;
            }
        };
    }

}
