package me.zort.sqllib.internal.query.part;

import me.zort.sqllib.internal.exception.IllegalStatementOperationException;
import me.zort.sqllib.internal.query.Conditional;
import me.zort.sqllib.internal.query.QueryDetails;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.util.Pair;
import me.zort.sqllib.util.Pairs;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;

public class SetStatement<P extends QueryNode<?> & Conditional<P>> extends QueryNode<P> implements Conditional<P> {

    private final Pairs<String, Object> update;
    private int currPhIndex = 0;

    public SetStatement(@Nullable P parent) {
        this(parent, 1);
    }

    public SetStatement(@Nullable P parent, int priority) {
        super(parent, Collections.emptyList(), priority);
        this.update = new Pairs<>();
    }


    public SetStatement<P> and(String column, Object value) {
        return and(new Pair<>(column, value));
    }

    public SetStatement<P> and(Pair<String, Object> pair) {
        this.update.add(pair);
        return this;
    }

    public SetStatement<P> and(Pairs<String, Object> pairs) {
        this.update.addAll(pairs);
        return this;
    }

    @Override
    public WhereStatement<P> where(int priority) {
        if(getParent() == null) {
            throw new IllegalStatementOperationException("Statement does not have parent set!");
        }
        return getParent().where(priority);
    }

    @Override
    public QueryDetails buildQueryDetails() {
        if(update.isEmpty()) {
            return QueryDetails.empty();
        }

        QueryDetails details = new QueryDetails(" SET ", new HashMap<>());

        for (Pair<String, Object> pair : update) {
            String name = pair.getFirst();
            Object value = pair.getSecond();

            String placeholder = nextPlaceholder();

            if (!details.getQueryStr().equals(" SET "))
                details.append(", ");

            details.append(new QueryDetails.Builder(String.format("%s = {%s}", name, placeholder))
                    .placeholder(placeholder, value)
                    .build());
        }

        return details;
    }

    private String nextPlaceholder() {
        return "set_" + currPhIndex++;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SetStatement<P> then(String part) {
        return (SetStatement<P>) super.then(part);
    }
}
