package me.zort.sqllib.internal.query.part;

import me.zort.sqllib.internal.query.QueryDetails;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.internal.query.QueryNodeRequest;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public class LimitStatement<P extends QueryNode<?>> extends QueryNodeRequest<P> {

    private final int limit;

    public LimitStatement(@Nullable P parent, List<QueryNode<?>> initial, int limit) {
        super(parent, initial, Integer.MAX_VALUE);
        this.limit = limit;
    }

    @Override
    public QueryDetails buildQueryDetails() {
        return new QueryDetails(" LIMIT " + Math.max(limit, 0), new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    @Override
    public LimitStatement<P> then(String part) {
        return (LimitStatement<P>) super.then(part);
    }
}
