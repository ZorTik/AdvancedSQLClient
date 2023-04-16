package me.zort.sqllib;

import lombok.Getter;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.mapping.StatementMappingOptions;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.query.*;
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
    public abstract <T> T createGate(Class<T> mappingInterface);
    public abstract <T> T createGate(Class<T> mappingInterface, @NotNull StatementMappingOptions options);
    public abstract boolean buildEntitySchema(String tableName, Class<?> entityClass);

    /**
     * Saves this mapping object into database using upsert query.
     * <p>
     * All mapping strategies are described in:
     * {@link SQLDatabaseConnection#query(Query, Class)}.
     *
     * @param table Table to save into.
     * @param obj The object to save.
     * @return Result of the query.
     */
    public abstract QueryResult save(String table, Object obj);

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
    public abstract boolean isLogSqlErrors();
    public abstract boolean isDebug();

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
        if(isLogSqlErrors()) {
            e.printStackTrace();
        }
    }

}
