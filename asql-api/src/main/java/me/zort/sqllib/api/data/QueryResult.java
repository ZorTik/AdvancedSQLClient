package me.zort.sqllib.api.data;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a query result.
 *
 * @author ZorTik
 */
public interface QueryResult {

  boolean isSuccessful();

  @Nullable
  String getRejectMessage();

  QueryResult noChangesResult = successful();

  static QueryResult successful() {
    return new QueryResult() {
      @Override
      public boolean isSuccessful() {
        return true;
      }

      @Override
      public @Nullable String getRejectMessage() {
        return null;
      }
    };
  }

}
