package me.zort.sqllib.internal.query.part;

import me.zort.sqllib.internal.exception.IllegalStatementOperationException;
import me.zort.sqllib.internal.query.Conditional;
import me.zort.sqllib.internal.query.QueryPart;
import me.zort.sqllib.util.Encoding;
import me.zort.sqllib.util.Pair;
import me.zort.sqllib.util.Pairs;
import me.zort.sqllib.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.stream.Collectors;

public class SetStatement<P extends QueryPart<?> & Conditional<P>> extends QueryPart<P> implements Conditional<P> {

    private final Pairs<String, Object> update;

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
    public String buildQuery() {
        if(update.isEmpty()) {
            return "";
        }
        return " SET " + update
                .stream()
                .map(pair -> {
                    Object obj = pair.getSecond();
                    if(obj instanceof String) {
                        obj = Encoding.handleTo((String) obj);
                    }
                    return pair.getFirst() + " = " + Util.buildQuoted(obj);
                })
                .collect(Collectors.joining(", "));
    }

    @Override
    public SetStatement<P> then(String part) {
        return (SetStatement<P>) super.then(part);
    }
}
