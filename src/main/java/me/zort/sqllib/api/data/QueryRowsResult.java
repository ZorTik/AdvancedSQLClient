package me.zort.sqllib.api.data;

import lombok.Getter;

import java.util.ArrayList;

public class QueryRowsResult<T> extends ArrayList<T> implements QueryResult {

    @Getter
    private final boolean successful;

    public QueryRowsResult(boolean successful) {
        this.successful = successful;
    }

}
