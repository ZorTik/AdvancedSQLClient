package me.zort.sqllib.api.data;

import lombok.Getter;

import java.util.LinkedList;

@Getter
public class QueryRowsResult<T> extends LinkedList<T> implements QueryResult {

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

    @Override
    public String toString() {
        return "QueryRowsResult{" +
                "successful=" + successful +
                ", rejectMessage='" + rejectMessage + '\'' +
                ", data=" + super.toString() +
                '}';
    }
}
