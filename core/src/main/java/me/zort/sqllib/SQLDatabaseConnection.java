package me.zort.sqllib;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.cache.CacheManager;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.mapping.StatementMappingFactory;
import me.zort.sqllib.api.mapping.StatementMappingOptions;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.impl.QueryResultImpl;
import me.zort.sqllib.internal.query.*;
import me.zort.sqllib.internal.query.part.SetStatement;
import me.zort.sqllib.transaction.Transaction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database connection object able to handle queries
 * from this library.
 *
 * @author ZorTik
 */
@SuppressWarnings("unused")
public abstract class SQLDatabaseConnection implements SQLConnection, Closeable {

    private final SQLConnectionFactory connectionFactory;
    @Getter(onMethod_ = {@Nullable})
    private Connection connection;
    @Getter(onMethod_ = {@Nullable})
    private SQLException lastError = null;

    public SQLDatabaseConnection(final @NotNull SQLConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.connection = null;

        SQLConnectionRegistry.register(this);
    }

    /**
     * Sets a mapping to use when using {@link SQLDatabaseConnection#createProxy(Class, StatementMappingOptions)}.
     *
     * @param mappingFactory Mapping factory to use.
     */
    public abstract void setProxyMapping(final @NotNull StatementMappingFactory mappingFactory);

    /**
     * @deprecated Use {@link SQLDatabaseConnection#createProxy(Class)} instead.
     */
    @Deprecated
    public abstract <T> T createGate(Class<T> mappingInterface);

    /**
     * Constructs a mapping repository based on provided interface.
     * The interface should follow rules for creating mapping repositories
     * in this library.
     * <p>
     * Example:
     * <pre>
     *     &#64;Table("users")
     *     public interface MyRepository {
     *          &#64;Select("*")
     *          &#64;Where(&#64;Where.Condition(column = "firstname", value = "{First Name}"))
     *          &#64;Limit(1)
     *          Optional&lt;User&gt; getUser(&#64;Placeholder("First Name") String firstName);
     *
     *          &#64;Select
     *          List&lt;User&gt; getUsers();
     *
     *          &#64;Delete
     *          QueryResult deleteUsers();
     *     }
     *
     *     SQLDatabaseConnection connection = ...;
     *     MyRepository repository = connection.createGate(MyRepository.class);
     *
     *     Optional&lt;User&gt; user = repository.getUser("John");
     * </pre>
     *
     * @param mappingInterface Interface to create mapping repository for.
     * @return Mapping repository.
     * @param <T> Type of mapping repository.
     */
    public abstract <T> T createProxy(Class<T> mappingInterface);
    public abstract <T> T createProxy(Class<T> mappingInterface, @NotNull StatementMappingOptions options);
    public abstract boolean buildEntitySchema(String tableName, Class<?> entityClass);

    /**
     * Performs new query and returns the result. This result is never null.
     * See: {@link QueryRowsResult#isSuccessful()}
     *
     * Examples:
     * <p>
     * query(Select.of().from("players"), Player.class)
     *  .stream()
     *  .map(Player::getNickname)
     *  .forEach(System.out::println);
     * <p>
     * query(() -> "SELECT * FROM players;");
     *
     * @param query The query to use while constructing query string.
     * @param typeClass Type class of object which will be instantiated and
     *                  populated with column values.
     * @param <T> Type of objects in result.
     *
     * @return Collection of row objects.
     */
    public abstract <T> QueryRowsResult<T> query(Query query, Class<T> typeClass);
    public abstract QueryRowsResult<Row> query(Query query);
    public abstract QueryRowsResult<Row> query(String query);

    /**
     * Executes given query and returns execution result.
     * This result does not contain any rows. If you want to
     * execute query return result of rows, see method
     * {@link SQLDatabaseConnection#query(Query)}
     *
     * @param query Query to use for building query string.
     * @return Blank rows result that only informs
     * about success state of the request.
     */
    public abstract QueryResult exec(Query query);
    public abstract QueryResult exec(String query);
    @ApiStatus.Experimental
    public abstract Transaction beginTransaction();
    @ApiStatus.Experimental
    public abstract void closeTransaction();
    @ApiStatus.Experimental
    @Nullable
    public abstract Transaction getTransaction();
    /**
     * Enabled caching for this connection.
     *
     * @param cacheManager Cache manager to use.
     */
    @ApiStatus.Experimental
    public abstract void enableCaching(CacheManager cacheManager);
    public abstract boolean isTransactionActive();
    protected abstract DefsVals buildDefsVals(Object obj);
    public abstract boolean isLogSqlErrors();
    public abstract boolean isDebug();

    public UpsertQuery save(final @NotNull String table, final @NotNull Object obj) {
        if(buildDefsVals(obj) == null) throw new IllegalArgumentException("Cannot create save query! (defsVals == null)");
        return save(obj).table(table);
    }

    public UpsertQuery save(final @NotNull Object obj) {
        DefsVals defsVals = buildDefsVals(obj);
        if(defsVals == null) return null;
        String[] defs = defsVals.getDefs();
        SQLDatabaseConnectionImpl.UnknownValueWrapper[] vals = defsVals.getVals();
        UpsertQuery upsertQuery = upsert().into(null, defs);
        for(SQLDatabaseConnectionImpl.UnknownValueWrapper wrapper : vals) {
            upsertQuery.appendVal(wrapper.getObject());
        }
        SetStatement<InsertQuery> setStatement = upsertQuery.onDuplicateKey();
        for(int i = 0; i < defs.length; i++) {
            setStatement.and(defs[i], vals[i].getObject());
        }

        return (UpsertQuery) setStatement.getAncestor();
    }

    public QueryResult insert(final @NotNull String table, final @NotNull Object obj) {
        DefsVals defsVals = buildDefsVals(obj);
        if (defsVals == null) return new QueryResultImpl(false);

        InsertQuery query = insert().into(table, defsVals.getDefs());
        for (SQLDatabaseConnectionImpl.UnknownValueWrapper valueWrapper : defsVals.getVals()) {
            query.appendVal(valueWrapper.getObject());
        }

        return query.execute();
    }

    // --***-- Query builders --***--

    public SelectQuery select(String... cols) {
        return new SelectQuery(this, cols);
    }

    public UpdateQuery update() {
        return update(null);
    }

    public UpdateQuery update(@Nullable String table) {
        return new UpdateQuery(this, table);
    }

    public InsertQuery insert() {
        return insert(null);
    }

    public InsertQuery insert(@Nullable String table) {
        return new InsertQuery(this, table);
    }

    public UpsertQuery upsert() {
        return upsert(null);
    }

    public UpsertQuery upsert(@Nullable String table) {
        return new UpsertQuery(this, table);
    }

    public DeleteQuery delete() {
        return new DeleteQuery(this);
    }

    @Override
    public final boolean connect() {
        if(isConnected()) disconnect();
        try {
            connection = connectionFactory.connect();
            lastError = null;
        } catch (SQLException e) {
            logSqlError(e);
            connection = null;
            lastError = e;
        }
        return isConnected();
    }

    @Override
    public final void disconnect() {
        if (!isConnected()) return;
        try {
            connection.close();
            lastError = null;
        } catch (SQLException e) {
            logSqlError(e);
            lastError = e;
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    protected void logSqlError(Exception e) {
        if(isLogSqlErrors()) e.printStackTrace();
    }

    @AllArgsConstructor
    @Getter
    protected static class DefsVals {
        private final String[] defs;
        private final SQLDatabaseConnectionImpl.UnknownValueWrapper[] vals;
    }

    @AllArgsConstructor
    @Data
    public static class UnknownValueWrapper {
        private Object object;
    }
}
