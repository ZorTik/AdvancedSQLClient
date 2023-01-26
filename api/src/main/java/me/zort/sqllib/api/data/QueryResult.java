package me.zort.sqllib.api.data;

import org.jetbrains.annotations.Nullable;

public interface QueryResult {

    boolean isSuccessful();
    @Nullable
    String getRejectMessage();

}
