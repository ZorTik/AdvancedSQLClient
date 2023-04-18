package me.zort.sqllib.internal.query.part;

import me.zort.sqllib.internal.exception.IllegalStatementOperationException;
import me.zort.sqllib.internal.query.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WhereStatement<P extends QueryNode<?>> extends QueryNode<P> implements ResultSetAware {

    private final List<QueryDetails> conditions = new ArrayList<>();
    private int currPhIndex = 0;

    public WhereStatement(@Nullable P parent, List<QueryNode<?>> initial) {
        super(parent, initial, QueryPriority.CONDITION.getPrior());
    }

    public WhereStatement(@Nullable P parent, List<QueryNode<?>> initial, int priority) {
        super(parent, initial, priority);
    }

    public WhereStatement<P> isEqual(String column, Object value) {
        String placeholder = nextPlaceholder();
        conditions.add(new QueryDetails.Builder(String.format("%s = <%s>", column, placeholder))
                .placeholder(placeholder, value)
                .build());
        return this;
    }

    public WhereStatement<P> bt(String column, long value) {
        String placeholder = nextPlaceholder();
        conditions.add(new QueryDetails.Builder(String.format("%s > <%s>", column, placeholder))
                .placeholder(placeholder, value)
                .build());
        return this;
    }

    public WhereStatement<P> lt(String column, long value) {
        String placeholder = nextPlaceholder();
        conditions.add(new QueryDetails.Builder(String.format("%s < <%s>", column, placeholder))
                .placeholder(placeholder, value)
                .build());
        return this;
    }

    public WhereStatement<P> in(String column, Object... objs) {
        return in(column, Arrays.asList(objs));
    }

    public WhereStatement<P> in(String column, List<?> objs) {
        if(objs.isEmpty()) return this;

        QueryDetails details = new QueryDetails(column + " IN (", new HashMap<>());
        for (Object obj : objs) {

            if (!details.getQueryStr().endsWith("("))
                details.append(", ");

            String placeholder = nextPlaceholder();
            details.append(new QueryDetails.Builder(String.format("<%s>", placeholder))
                    .placeholder(placeholder, obj)
                    .build());
        }
        details.append(")");

        conditions.add(details);

        return this;
    }

    public WhereStatement<P> like(String column, String paramPlaceholder) {
        String placeholder = nextPlaceholder();
        conditions.add(new QueryDetails.Builder(String.format("%s LIKE <%s>", column, placeholder))
                .placeholder(placeholder, paramPlaceholder)
                .build());
        return this;
    }

    private String nextPlaceholder() {
        return "where_" + currPhIndex++;
    }

    @Override
    public <T extends QueryNode<?>> QueryNode<T> then(QueryNode<T> part) {
        throw new IllegalStatementOperationException("Where statement can't have inner parts!");
    }

    public WhereStatement<P> and() {
        return this;
    }

    public WhereStatement<P> or() {
        conditions.add(new QueryDetails(" OR ", new HashMap<>()));
        return this;
    }

    @Override
    public QueryDetails buildQueryDetails() {
        QueryDetails details = new QueryDetails(" WHERE ", new HashMap<>());

        if(conditions.isEmpty()) {
            // We don't have any conditions, so where statement should be true.
            details.append("TRUE");
        } else {
            for(QueryDetails _details : conditions) {
                String condition = _details.getQueryStr();

                if(!details.getQueryStr().equals(" WHERE ") && !condition.equals(" OR ") && !details.getQueryStr().endsWith(" OR ")) {
                    details.append(" AND ");
                }

                details.append(_details);
            }
        }

        return details;
    }

    @SuppressWarnings("unchecked")
    @Override
    public WhereStatement<P> then(String part) {
        return (WhereStatement<P>) super.then(part);
    }
}
