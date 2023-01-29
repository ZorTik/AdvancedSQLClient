package me.zort.sqllib.internal.query;

import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.SQLDatabaseConnectionImpl;
import me.zort.sqllib.api.*;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.internal.exception.InvalidConnectionInstanceException;
import me.zort.sqllib.internal.exception.NoLinkedConnectionException;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Getter
public abstract class QueryNode<P extends QueryNode<?>> implements Query, StatementFactory<PreparedStatement> {

    @Getter(onMethod_ = {@Nullable})
    private final transient P parent;
    private final List<QueryNode<?>> children;
    private final int priority;
    private final Map<String, QueryDetails> details;

    public QueryNode(@Nullable P parent, List<QueryNode<?>> initial, QueryPriority priority) {
        this(parent, initial, priority.getPrior());
    }

    public QueryNode(@Nullable P parent, List<QueryNode<?>> initial, int priority) {
        this.parent = parent;
        this.children = initial;
        this.priority = priority;
        this.details = new ConcurrentHashMap<>();
    }

    /**
     * Builds the query string with placeholders containing values
     * for passing into PreparedStatement.
     * <p>
     * Query example: SELECT * FROM table WHERE id = &lt;id&gt;;
     * Values example: [AnyId]
     *
     * @return QueryDetails object.
     */
    public abstract QueryDetails buildQueryDetails();

    @Override
    public PreparedStatement prepare(Connection connection) throws SQLException {
        return details.remove(buildQuery()).prepare(connection);
    }

    @Override
    public String buildQuery() {
        QueryDetails queryDetails = buildQueryDetails();

        if (isAncestor())
            debug(String.format("Query: %s", queryDetails.getQueryStr()));

        String uuid = UUID.randomUUID().toString();
        details.put(uuid, queryDetails);
        return uuid;
    }

    public QueryDetails buildInnerQuery() {
        List<QueryNode<?>> children = new ArrayList<>(this.children);
        Collections.sort(children, Comparator.comparingInt(QueryNode::getPriority));

        QueryDetails details = new QueryDetails("", new HashMap<>());

        if (children.isEmpty()) {
            return QueryDetails.empty();
        }

        for (QueryNode<?> inner : children) {
            if (details.length() > 0)
                details.append(" ");

            QueryDetails innerDetails = inner.getDetails().get(inner.buildQuery());
            details.append(innerDetails);
        }

        return details;
    }

    @Nullable
    protected <T> T invokeToConnection(Function<SQLDatabaseConnection, T> func)
            throws NoLinkedConnectionException, InvalidConnectionInstanceException {
        QueryNode<?> current = this;
        while(current.getParent() != null && !(current instanceof Executive)) {
            current = current.getParent();
        }
        T result;
        if(current instanceof Executive) {
            SQLConnection connection = ((Executive) current).getConnection();
            if(!(connection instanceof SQLDatabaseConnection)) {
                throw new InvalidConnectionInstanceException(connection);
            }

            result = func.apply((SQLDatabaseConnection) connection);
        } else {
            throw new NoLinkedConnectionException(this);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public QueryNode<?> then(String part) {
        int maxPriority = children.stream()
                .map(QueryNode::getPriority)
                .max(Comparator.naturalOrder())
                .orElse(0);

        then(new LocalQueryNode(this, maxPriority + 1, part));
        return this;
    }

    public <T extends QueryNode<?>> QueryNode<T> then(QueryNode<T> part) {
        this.children.add(part);
        return part;
    }

    public P also() {
        return parent;
    }

    public QueryResult execute() {
        return invokeToConnection(connection -> connection.exec(getAncestor()));
    }

    public QueryNode<?> getAncestor() {
        QueryNode<?> current = this;
        while(current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    private void debug(String message) {
        if (getAncestor() instanceof Executive
        && ((Executive) getAncestor()).getConnection() instanceof SQLDatabaseConnectionImpl) {
            ((SQLDatabaseConnectionImpl) ((Executive) getAncestor()).getConnection()).debug(message);
        }
    }

    private static class LocalQueryNode extends QueryNode {

        private final String queryPartString;

        public LocalQueryNode(@Nullable QueryNode parent, int priority, String queryPartString) {
            super(parent, Collections.emptyList(), priority);
            this.queryPartString = queryPartString;
        }

        @Override
        public QueryDetails buildQueryDetails() {
            return new QueryDetails(queryPartString, new HashMap<>());
        }
    }

}
