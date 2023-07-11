package me.zort.sqllib.cache;

import com.google.common.annotations.Beta;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.cache.CacheManager;
import me.zort.sqllib.api.data.QueryResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

@Beta
public class ExpireWriteCacheManager implements CacheManager {

  private final Cache<Query, QueryResult> cache;

  /**
   * Creates a new cache manager with provided expiration duration
   * in milliseconds.
   *
   * @param expirationDuration The expiration duration
   */
  public ExpireWriteCacheManager(long expirationDuration) {
    cache = CacheBuilder.newBuilder()
            .expireAfterWrite(expirationDuration, TimeUnit.MILLISECONDS)
            .build();
  }

  @Override public void set(@NotNull Query query, @NotNull QueryResult result) {
    cache.put(query, result);
  }

  @Override
  public @Nullable QueryResult get(@NotNull Query query, boolean isExec) {
    return cache.getIfPresent(query);
  }
}
