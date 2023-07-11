package me.zort.sqllib.api.provider;

import me.zort.sqllib.internal.query.SelectQuery;
import org.jetbrains.annotations.ApiStatus;

@Deprecated
@ApiStatus.ScheduledForRemoval
public final class Select {

  public static SelectQuery of(String... columns) {
    return new SelectQuery(null, columns);
  }

}
