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

}
