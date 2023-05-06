package me.zort.sqllib.cache;

import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.cache.CacheManager;
import me.zort.sqllib.api.data.QueryResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExpirableEntriesCacheManager implements CacheManager {
    @Override
    public void set(@NotNull Query query, @NotNull QueryResult result) {
        // TODO: Implement
    }

    @Override
    public @Nullable QueryResult get(@NotNull Query query, boolean isExec) {
        // TODO: Implement
        return null;
    }
}
