package me.zort.sqllib.internal.query;

import lombok.Getter;
import me.zort.sqllib.api.Executive;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.internal.query.part.WhereStatement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public abstract class QueryPart<P extends QueryPart<?>> implements Query {

    @Getter(onMethod_ = {@Nullable})
    private final P parent;
    private final List<QueryPart<?>> children;
    private final int priority;

    public QueryPart(@Nullable P parent, List<QueryPart<?>> initial) {
        this(parent, initial, QueryPriority.GENERAL);
    }

    public QueryPart(@Nullable P parent, List<QueryPart<?>> initial, QueryPriority priority) {
        this(parent, initial, priority.getPrior());
    }

    public QueryPart(@Nullable P parent, List<QueryPart<?>> initial, int priority) {
        this.parent = parent;
        this.children = initial;
        this.priority = priority;
    }

    protected <T extends QueryPart<?>> WhereStatement<T> where(T parent) {
        return new WhereStatement<>(parent, new ArrayList<>());
    }

    public <T extends QueryPart<?>> QueryPart<T> then(QueryPart<T> part) {
        this.children.add(part);
        return part;
    }

    public P also() {
        return parent;
    }

    public String buildInnerQuery() {
        List<QueryPart<?>> children = new ArrayList<>(this.children);
        Collections.sort(children, Comparator.comparingInt(QueryPart::getPriority));
        return !children.isEmpty() ? String.join(" ", children
                .stream()
                .map(QueryPart::buildQuery)
                .collect(Collectors.toList())) : "";
    }

    @Nullable
    protected <T> T invokeToConnection(Function<SQLDatabaseConnection, T> func) {
        QueryPart<?> current = this;
        while(current.getParent() != null && !(current instanceof Executive)) {
            current = current.getParent();
        }
        T result = null;
        if(current instanceof Executive) {
            SQLDatabaseConnection connection = ((Executive) current).getConnection();
            result = func.apply(connection);
        }
        return result;
    }

    protected QueryPart<?> getAncestor() {
        QueryPart<?> current = this;
        while(current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

}
