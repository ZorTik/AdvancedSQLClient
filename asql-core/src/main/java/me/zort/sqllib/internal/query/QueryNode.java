package me.zort.sqllib.internal.query;

import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.SQLDatabaseConnectionImpl;
import me.zort.sqllib.api.*;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.exception.InvalidConnectionInstanceException;
import me.zort.sqllib.internal.exception.NoLinkedConnectionException;
import me.zort.sqllib.util.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Represents a query builder node, a part of a query builder flow.
 * This is a tree structure node, each node can represent part in a query that is prepared
 * to be joined in a final query.
 *
 * @param <P> Parent node type.
 * @author ZorTik
 */
@Getter
public abstract class QueryNode<P extends QueryNode<?>> implements Query, StatementFactory<PreparedStatement> {

  @Getter(onMethod_ = {@Nullable})
  private final transient P parent;
  private final List<QueryNode<?>> children;
  private final Map<String, QueryDetails> details;
  private final int priority;

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
   * Creates a new QueryNode from a query string in PreparedStatement format and
   * parameters to replace question marks in the query. This is useful if there is no
   * other way to create a query than using raw SQL details.
   * <p></p>
   * Example:
   * <pre>
   * Query query = QueryNode.fromRawQuery("SELECT * FROM table WHERE id = ?", 1);
   * </pre>
   *
   * @param query
   * @param params
   * @return
   */
  public static QueryNode<?> fromRawQuery(String query, Object... params) {
    return new QueryNode<>(null, Collections.emptyList(), QueryPriority.GENERAL) {
      @Override
      public QueryDetails buildQueryDetails() {
        Map<String, Object> values = new HashMap<>();
        String preparedStr = query;
        int index = 0;
        while (true) {
          final String before = preparedStr;
          preparedStr = before.replaceFirst("\\?", String.format("<val_%d>", index));
          if (preparedStr.equals(before)) {
            break;
          }
          values.put("val_" + index, params[index]);
        }
        return new QueryDetails(preparedStr, values);
      }
    };
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

  @ApiStatus.Internal
  @Override
  public String buildQuery() {
    QueryDetails queryDetails = buildQueryDetails();

    if (isAncestor())
      debug(String.format("Query: %s", queryDetails.getQueryStr()));

    String uuid = UUID.randomUUID().toString();
    details.put(uuid, queryDetails);
    return uuid;
  }

  @ApiStatus.Internal
  public QueryDetails buildInnerQuery() {
    List<QueryNode<?>> children = new ArrayList<>(this.children);
    children.sort(Comparator.comparingInt(QueryNode::getPriority));

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
    while (current.getParent() != null && !(current instanceof Executive)) {
      current = current.getParent();
    }
    T result;
    if (current instanceof Executive) {
      SQLConnection connection = ((Executive) current).getConnection();
      if (!(connection instanceof SQLDatabaseConnection)) {
        throw new InvalidConnectionInstanceException(connection);
      }

      result = func.apply((SQLDatabaseConnection) connection);
    } else {
      throw new NoLinkedConnectionException(this);
    }
    return result;
  }

  public QueryNode<?> then(String part) {
    int maxPriority = children.stream()
            .map(QueryNode::getPriority)
            .max(Comparator.naturalOrder())
            .orElse(0);

    then(new QueryNode<QueryNode<?>>(parent, Collections.emptyList(), maxPriority + 1) {
      @Override
      public QueryDetails buildQueryDetails() {
        return new QueryDetails(part, new HashMap<>());
      }
    });
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

  public Optional<Row> obtainOne() {
    QueryRowsResult<Row> resultList = obtainAll();

    return resultList.isEmpty()
            ? Optional.empty()
            : Optional.ofNullable(resultList.get(0));
  }

  public <T> Optional<T> obtainOne(Class<T> mapTo) {
    QueryRowsResult<T> resultList = obtainAll(mapTo);

    return resultList.isEmpty()
            ? Optional.empty()
            : Optional.ofNullable(resultList.get(0));
  }

  public QueryRowsResult<Row> obtainAll() {
    requireResultSetAware();
    return invokeToConnection(connection -> connection.query(getAncestor()));
  }

  public <T> QueryRowsResult<T> obtainAll(Class<T> mapTo) {
    requireResultSetAware();
    return invokeToConnection(connection -> connection.query(getAncestor(), mapTo));
  }

  private void requireResultSetAware() {
    if (!generatesResultSet()) {
      throw new IllegalStateException("This query node is not ResultSetAware! (Did you mean execute()?)");
    }
  }

  public QueryNode<?> getAncestor() {
    QueryNode<?> current = this;
    while (current.getParent() != null) {
      current = current.getParent();
    }
    return current;
  }

  public boolean generatesResultSet() {
    return this instanceof ResultSetAware;
  }

  private void debug(String message) {
    if (getAncestor() instanceof Executive
            && ((Executive) getAncestor()).getConnection() instanceof SQLDatabaseConnectionImpl) {
      ((SQLDatabaseConnectionImpl) ((Executive) getAncestor()).getConnection()).debug(message);
    }
  }

  @SuppressWarnings("unused")
  public Pair<String, Object[]> toPreparedQuery() {
    return getAncestor().buildQueryDetails().buildStatementDetails();
  }

  public String toString() {
    return "QueryNode{details=" + buildQueryDetails().toString() + "}";
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof QueryNode)) return false;
    QueryNode<?> other = (QueryNode<?>) obj;
    return toString().equals(other.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
