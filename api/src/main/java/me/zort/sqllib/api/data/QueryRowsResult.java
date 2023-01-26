package me.zort.sqllib.api.data;

import lombok.Getter;

import java.util.ArrayList;

@Getter
public class QueryRowsResult<T> extends ArrayList<T> implements QueryResult {

    private final boolean successful;
    private String rejectMessage = null;

    public QueryRowsResult(boolean successful) {
        this(successful, null);
    }

    public QueryRowsResult(boolean successful, String rejectMessage) {
        this.successful = successful;
        rejectMessage(rejectMessage);
    }

    public QueryRowsResult<T> rejectMessage(String message) {
        if (rejectMessage != null)
            throw new RuntimeException("Reject message is already set!");

        this.rejectMessage = message;
        return this;
    }

}
