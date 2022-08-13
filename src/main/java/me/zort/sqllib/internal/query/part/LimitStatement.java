package me.zort.sqllib.internal.query.part;

import me.zort.sqllib.internal.query.QueryPart;
import me.zort.sqllib.internal.query.QueryPartQuery;
import me.zort.sqllib.internal.query.QueryPriority;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LimitStatement<P extends QueryPart<?>> extends QueryPartQuery<P> {

    private final int limit;

    public LimitStatement(@Nullable P parent, List<QueryPart<?>> initial, int limit) {
        super(parent, initial, QueryPriority.LAST);
        this.limit = limit;
    }

    @Override
    public String buildQuery() {
        return " LIMIT " + Math.max(limit, 0);
    }

}
