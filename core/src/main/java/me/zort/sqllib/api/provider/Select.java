package me.zort.sqllib.api.provider;

import me.zort.sqllib.internal.query.SelectQuery;

public final class Select {

  public static SelectQuery of(String... columns) {
    return new SelectQuery(null, columns);
  }

}
