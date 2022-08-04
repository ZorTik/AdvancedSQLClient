package me.zort.sqllib.internal.query;

import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class QueryPartQuery<P extends QueryPart<?>> extends QueryPart<P> {

    public QueryPartQuery(@Nullable P parent, List<QueryPart<?>> initial) {
        super(parent, initial);
    }

    public QueryPartQuery(@Nullable P parent, List<QueryPart<?>> initial, QueryPriority priority) {
        super(parent, initial, priority);
    }

    public QueryPartQuery(@Nullable P parent, List<QueryPart<?>> initial, int priority) {
        super(parent, initial, priority);
    }

    public Optional<Row> obtainOne() {
        QueryRowsResult<Row> resultList = obtainAll();
        if(resultList.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(resultList.get(0));
        }
    }

    public QueryRowsResult<Row> obtainAll() {
        return invokeToConnection(connection -> connection.query(getAncestor()));
    }

}
