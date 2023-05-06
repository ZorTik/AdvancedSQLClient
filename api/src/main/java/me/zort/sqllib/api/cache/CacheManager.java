package me.zort.sqllib.api.cache;

import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.data.QueryResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CacheManager {

    void set(@NotNull Query query, @NotNull QueryResult result);
    @Nullable QueryResult get(@NotNull Query query, boolean isExec);

    static CacheManager noCache() {
        return new CacheManager() {
            @Override
            public void set(@NotNull Query query, @NotNull QueryResult result) {
            }
            @Override
            public @Nullable QueryResult get(@NotNull Query query, boolean isExec) {
                return null;
            }
        };
    }

}
