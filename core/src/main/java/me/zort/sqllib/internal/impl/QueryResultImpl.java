package me.zort.sqllib.internal.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zort.sqllib.api.data.QueryResult;

@RequiredArgsConstructor
public class QueryResultImpl implements QueryResult {

    @Getter
    private final boolean successful;

}
