package me.zort.sqllib;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.cache.CacheManager;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.mapping.StatementMappingFactory;
import me.zort.sqllib.api.mapping.StatementMappingOptions;
import me.zort.sqllib.api.model.SchemaSynchronizer;
import me.zort.sqllib.api.model.TableSchema;
import me.zort.sqllib.api.model.TableSchemaBuilder;
import me.zort.sqllib.cache.ExpireWriteCacheManager;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.impl.QueryResultImpl;
import me.zort.sqllib.internal.query.*;
import me.zort.sqllib.internal.query.part.SetStatement;
import me.zort.sqllib.mapping.MappingProvider;
import me.zort.sqllib.transaction.Transaction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static me.zort.sqllib.SQLConnectionRegistry.registerConnection;
import static me.zort.sqllib.util.ExceptionsUtility.runCatching;

/**
 * Database connection object able to handle queries
 * from this library.
 *
 * @author ZorTik
 */
@SuppressWarnings("unused")
public abstract class SQLDatabaseConnection
        extends MappingProvider implements SQLConnection, Closeable {

  private final SQLConnectionFactory connectionFactory;
  private final transient List<SQLDatabaseConnectionImpl.CodeObserver> codeHandlers;
  @Getter(onMethod_ = {@Nullable})
  private Connection connection;
  @Getter(onMethod_ = {@Nullable})
  private SQLException lastError = null;
  @Getter
  private int errorCount = 0;

  public SQLDatabaseConnection(final @NotNull SQLConnectionFactory connectionFactory) {
    super();
    this.connectionFactory = connectionFactory;
    this.connection = null;
    this.codeHandlers = new CopyOnWriteArrayList<>();

    setMConnectionFactory(() -> this);
    registerConnection(this);
  }

  /**
   * Sets the schema synchronizer to use with synchronizeModel methods.
   *
   * @param synchronizer Schema synchronizer to use
   */
  @ApiStatus.Experimental
  public abstract void setSchemaSynchronizer(SchemaSynchronizer<SQLDatabaseConnection> synchronizer);

  public abstract boolean buildEntitySchema(String tableName, Class<?> entityClass);

  /**
   * Synchronizes proxy mapping models with database.<br>
   * <i>Warning:</i> This tries to update all tables that are part of mapping
   * proxies. Should be used carefully.
   *
   * @return True if synchronization was successful
   */
  @ApiStatus.Experimental
  public abstract boolean synchronizeModel();

  /**
   * Synchronizes provided entity schema with the database
   * In other words, it tries to update database schema (table) to match
   * the provided entity schema.
   *
   * @param entitySchema Entity schema
   * @param table        Table name
   * @return True if synchronization was successful
   */
  @ApiStatus.Experimental
  public abstract boolean synchronizeModel(TableSchema entitySchema, String table);

  /**
   * Synchronizes provided entity class with the database
   * In other words, it tries to update database schema (table) to match
   * the provided entity schema. This method uses synchronizeModel method
   * with schema generated from the provided entity class.
   *
   * @param entity The entity (model) class
   * @param table  Table name
   * @return True if synchronization was successful
   */
  @ApiStatus.Experimental
  public abstract boolean synchronizeModel(Class<?> entity, String table);

  /**
   * Performs new query and returns the result. This result is never null.
   * See: {@link QueryRowsResult#isSuccessful()}
   * <p>
   * Examples:
   * <p>
   * query(Select.of().from("players"), Player.class)
   * .stream()
   * .map(Player::getNickname)
   * .forEach(System.out::println);
   * <p>
   * query(() -> "SELECT * FROM players;");
   *
   * @param query     The query to use while constructing query string.
   * @param typeClass Type class of object which will be instantiated and
   *                  populated with column values.
   * @param <T>       Type of objects in result.
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

  @ApiStatus.Experimental
  public abstract TableSchemaBuilder getSchemaBuilder(String table);

  @ApiStatus.Experimental
  public abstract SchemaSynchronizer<SQLDatabaseConnection> getSchemaSynchronizer();

  /**
   * Adds an error state observer to the list of observers to be
   * notified when a fatal error occurs.
   *
   * @param observer Observer to add.
   */
  public void addCodeHandler(final @NotNull SQLDatabaseConnection.CodeObserver observer) {
    this.codeHandlers.add(observer);
  }

  /**
   * Adds an error state observer to the list of observers to be
   * notified when a fatal error occurs. The provided observer will be notified
   * only to calls with the provided code.
   *
   * @param observer Observer to add.
   */
  public void addCodeHandler(final int code, final @NotNull SQLDatabaseConnection.CodeObserver observer) {
    addCodeHandler(code1 -> {
      if (code1 != code) return;
      observer.onNotified(code1);
    });
  }

  public List<SQLDatabaseConnection.CodeObserver> getCodeHandlers() {
    return new ArrayList<>(codeHandlers);
  }

  /**
   * Enables caching for provided milliseconds.
   * This uses {@link ExpireWriteCacheManager} cache manager.
   *
   * @param millis The entries expiration in milliseconds
   * @return This instance
   */
  public SQLDatabaseConnection cacheFor(long millis) {
    enableCaching(new ExpireWriteCacheManager(millis));
    return this;
  }

  public UpsertQuery save(final @NotNull String table, final @NotNull Object obj) {
    if (buildDefsVals(obj) == null) throw new IllegalArgumentException("Cannot create save query! (defsVals == null)");
    return save(obj).table(table);
  }

  public UpsertQuery save(final @NotNull Object obj) {
    DefsVals defsVals = buildDefsVals(obj);
    if (defsVals == null) return null;
    String[] defs = defsVals.getDefs();
    AtomicReference<Object>[] vals = defsVals.getVals();
    UpsertQuery upsertQuery = upsert().into(null, defs);
    for (AtomicReference<Object> wrapper : vals) {
      upsertQuery.appendVal(wrapper.get());
    }
    SetStatement<InsertQuery> setStatement = upsertQuery.onDuplicateKey();
    for (int i = 0; i < defs.length; i++) {
      setStatement.and(defs[i], vals[i].get());
    }

    return (UpsertQuery) setStatement.getAncestor();
  }

  public QueryResult insert(final @NotNull String table, final @NotNull Object obj) {
    DefsVals defsVals = buildDefsVals(obj);
    if (defsVals == null) return new QueryResultImpl(false);

    InsertQuery query = insert().into(table, defsVals.getDefs());
    for (AtomicReference<Object> valueWrapper : defsVals.getVals()) {
      query.appendVal(valueWrapper.get());
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

  @Deprecated public UpdateQuery update(@Nullable String table) {
    return new UpdateQuery(this, table);
  }

  public InsertQuery insert() {
    return insert(null);
  }

  @Deprecated public InsertQuery insert(@Nullable String table) {
    return new InsertQuery(this, table);
  }

  public UpsertQuery upsert() {
    return upsert(null);
  }

  @Deprecated public UpsertQuery upsert(@Nullable String table) {
    return new UpsertQuery(this, table);
  }

  public DeleteQuery delete() {
    return new DeleteQuery(this);
  }

  @Override
  public final boolean connect() {
    if (isConnected()) disconnect();
    try {
      connection = connectionFactory.connect();
      lastError = null;
      notifyHandlers(Code.CONNECTED);
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
    notifyHandlers(Code.CLOSED);
  }

  @Override
  public void close() {
    disconnect();
  }

  protected void logSqlError(Exception e) {
    if (isLogSqlErrors()) e.printStackTrace();
  }

  @SuppressWarnings("all")
  protected void notifyHandlers(int code) {
    if (code >= 100) {
      // Code is higher than 99, so it's an error
      errorCount++;
    }
    this.codeHandlers.forEach(handler -> runCatching(() -> handler.onNotified(code)));
  }

  @Override
  protected void finalize() throws Throwable {
    disconnect();
    notifyHandlers(Code.CLEARING);
  }

  public interface CodeObserver {
    void onNotified(int code);
  }

  @AllArgsConstructor
  @Getter
  protected static class DefsVals {
    private final String[] defs;
    private final AtomicReference<Object>[] vals;
  }

  public static final class Code {
    public static final int CONNECTED = 1;
    public static final int CLOSED = 2;
    public static final int CLEARING = 3;

    // Error codes start on 100
    public static final int QUERY_FATAL = 100;
  }
}
