package me.zort.sqllib.internal.query.part;

import me.zort.sqllib.internal.exception.IllegalStatementOperationException;
import me.zort.sqllib.internal.query.QueryPart;
import me.zort.sqllib.internal.query.QueryPartQuery;
import me.zort.sqllib.internal.query.QueryPriority;
import me.zort.sqllib.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WhereStatement<P extends QueryPart<?>> extends QueryPartQuery<P> {

    private final List<String> conditions = new ArrayList<>();

    public WhereStatement(@Nullable P parent, List<QueryPart<?>> initial) {
        super(parent, initial, QueryPriority.CONDITION.getPrior());
    }

    public WhereStatement(@Nullable P parent, List<QueryPart<?>> initial, int priority) {
        super(parent, initial, priority);
    }

    public WhereStatement<P> isEqual(String column, Object value) {
        conditions.add(column + " = " + Util.buildQuoted(value));
        return this;
    }

    public WhereStatement<P> bt(String column, long value) {
        conditions.add(column + " > " + value);
        return this;
    }

    public WhereStatement<P> lt(String column, long value) {
        conditions.add(column + " < " + value);
        return this;
    }

    public WhereStatement<P> in(String column, Object... objs) {
        return in(column, Arrays.asList(objs));
    }

    public WhereStatement<P> in(String column, List<?> objs) {
        if(objs.isEmpty()) return this;
        conditions.add(column + " IN (" + objs.stream()
                .map(Util::buildQuoted)
                .collect(Collectors.joining(", ")) + ")");
        return this;
    }

    @Override
    public <T extends QueryPart<?>> QueryPart<T> then(QueryPart<T> part) {
        throw new IllegalStatementOperationException("Where statement can't have inner parts!");
    }

    public WhereStatement<P> and() {
        return this;
    }

    public WhereStatement<P> or() {
        conditions.add(" OR ");
        return this;
    }

    @Override
    public String buildQuery() {
        StringBuilder stmt = new StringBuilder(" WHERE ");
        if(conditions.isEmpty()) {
            // We don't have any conditions, so where statement should be true.
            return stmt + "TRUE";
        }
        for(String condition : conditions) {
            if(!stmt.toString().equals(" WHERE ") && !condition.equals(" OR ") && !stmt.toString().endsWith(" OR ")) {
                stmt.append(" AND ");
            }
            stmt.append(condition);
        }
        return stmt.toString();
    }

}
