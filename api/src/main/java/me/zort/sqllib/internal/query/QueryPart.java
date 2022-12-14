package me.zort.sqllib.internal.query;

import lombok.Getter;
import me.zort.sqllib.api.Executive;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.internal.exception.NoLinkedConnectionException;
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

    public <T extends QueryPart<?>> QueryPart<T> then(QueryPart<T> part) {
        this.children.add(part);
        return part;
    }

    public QueryPart<?> then(String part) {
        int maxPriority = children.stream()
                .map(QueryPart::getPriority)
                .max(Comparator.naturalOrder())
                .orElse(0);
        then(new LocalQueryPart(this, maxPriority + 1, part));
        return this;
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

    public QueryResult execute() {
        return invokeToConnection(connection -> connection.exec(getAncestor()));
    }

    @Nullable
    protected <T> T invokeToConnection(Function<SQLDatabaseConnection, T> func) throws NoLinkedConnectionException {
        QueryPart<?> current = this;
        while(current.getParent() != null && !(current instanceof Executive)) {
            current = current.getParent();
        }
        T result;
        if(current instanceof Executive) {
            SQLDatabaseConnection connection = ((Executive) current).getConnection();
            result = func.apply(connection);
        } else {
            throw new NoLinkedConnectionException(this);
        }
        return result;
    }

    public QueryPart<?> getAncestor() {
        QueryPart<?> current = this;
        while(current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    private static class LocalQueryPart extends QueryPart {

        private final String queryPartString;

        public LocalQueryPart(@Nullable QueryPart parent, int priority, String queryPartString) {
            super(parent, Collections.emptyList(), priority);
            this.queryPartString = queryPartString;
        }

        @Override
        public String buildQuery() {
            return queryPartString;
        }

    }

}
