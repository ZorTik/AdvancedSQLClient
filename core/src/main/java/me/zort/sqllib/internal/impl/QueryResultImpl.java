package me.zort.sqllib.internal.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;

@RequiredArgsConstructor
@Getter
public class QueryResultImpl implements QueryResult {

    private final boolean successful;
    private String rejectMessage = null;

    public QueryResultImpl(boolean successful, String rejectMessage) {
        this.successful = successful;
        rejectMessage(rejectMessage);
    }

    public QueryResultImpl rejectMessage(String message) {
        if (rejectMessage != null)
            throw new RuntimeException("Reject message is already set!");

        this.rejectMessage = message;
        return this;
    }

    @Override
    public String toString() {
        return "QueryResultImpl{" +
                "successful=" + successful +
                ", rejectMessage='" + rejectMessage + '\'' +
                '}';
    }
}
