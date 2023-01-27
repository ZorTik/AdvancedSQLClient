package me.zort.sqllib.internal.query;

import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class QueryNodeRequest<P extends QueryNode<?>> extends QueryNode<P> {

    public QueryNodeRequest(@Nullable P parent, List<QueryNode<?>> initial, QueryPriority priority) {
        super(parent, initial, priority);
    }

    public QueryNodeRequest(@Nullable P parent, List<QueryNode<?>> initial, int priority) {
        super(parent, initial, priority);
    }

    public Optional<Row> obtainOne() {
        QueryRowsResult<Row> resultList = obtainAll();

        return resultList.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(resultList.get(0));
    }

    public QueryRowsResult<Row> obtainAll() {
        return invokeToConnection(connection -> connection.query(getAncestor()));
    }

}
