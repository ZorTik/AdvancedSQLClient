package me.zort.sqllib.internal.query.part;

import me.zort.sqllib.internal.exception.IllegalStatementOperationException;
import me.zort.sqllib.internal.query.QueryPart;
import me.zort.sqllib.internal.query.QueryPartQuery;
import me.zort.sqllib.internal.query.QueryPriority;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WhereStatement<P extends QueryPart<?>> extends QueryPartQuery<P> {

    private final List<String> conditions = new ArrayList<>();

    public WhereStatement(@Nullable P parent, List<QueryPart<?>> initial) {
        super(parent, initial, QueryPriority.CONDITION);
    }

    public WhereStatement<P> isEqual(String column, Object value) {
        conditions.add(column + " = " + buildQuoted(value));
        return this;
    }

    public WhereStatement<P> in(String column, List<?> objs) {
        if(objs.isEmpty()) return this;
        conditions.add(column + " IN (" + objs.stream()
                .map(WhereStatement::buildQuoted)
                .collect(Collectors.joining(", ")) + ")");
        return this;
    }

    @Override
    public <T extends QueryPart<?>> QueryPart<T> then(QueryPart<T> part) {
        throw new IllegalStatementOperationException("Where statement can't have inner parts!");
    }

    @Override
    public String buildQuery() {
        StringBuilder stmt = new StringBuilder(" WHERE ");
        if(conditions.isEmpty()) {
            // We don't have any conditions, so where statement should be true.
            return stmt + "TRUE";
        }
        for(String condition : conditions) {
            if(!stmt.toString().equals(" WHERE ")) {
                stmt.append("AND ");
            }
            stmt.append(condition);
        }
        return stmt.toString();
    }

    private static String buildQuoted(Object obj) {
        obj = obj instanceof String
                ? String.format("'%s'", obj)
                : String.valueOf(obj);
        return (String) obj;
    }

}
