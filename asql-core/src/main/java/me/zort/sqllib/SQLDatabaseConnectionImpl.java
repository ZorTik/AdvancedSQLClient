package me.zort.sqllib;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import me.zort.sqllib.api.ISQLDatabaseOptions;
import me.zort.sqllib.api.ObjectMapper;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.StatementFactory;
import me.zort.sqllib.api.cache.CacheManager;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.mapping.MappingProxyInstance;
import me.zort.sqllib.api.mapping.StatementMappingFactory;
import me.zort.sqllib.api.mapping.StatementMappingOptions;
import me.zort.sqllib.api.mapping.StatementMappingRegistry;
import me.zort.sqllib.api.model.SchemaSynchronizer;
import me.zort.sqllib.api.model.TableSchema;
import me.zort.sqllib.api.model.TableSchemaBuilder;
import me.zort.sqllib.api.options.NamingStrategy;
import me.zort.sqllib.internal.Defaults;
import me.zort.sqllib.internal.annotation.JsonField;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.fieldResolver.ConstructorParameterResolver;
import me.zort.sqllib.internal.fieldResolver.LinkedOneFieldResolver;
import me.zort.sqllib.internal.impl.DefaultObjectMapper;
import me.zort.sqllib.internal.impl.QueryResultImpl;
import me.zort.sqllib.mapping.DefaultStatementMappingFactory;
import me.zort.sqllib.mapping.MappingRegistryImpl;
import me.zort.sqllib.mapping.ProxyInstanceImpl;
import me.zort.sqllib.model.schema.DatabaseSchemaBuilder;
import me.zort.sqllib.model.schema.EntitySchemaBuilder;
import me.zort.sqllib.model.schema.SQLSchemaSynchronizer;
import me.zort.sqllib.pool.PooledSQLDatabaseConnection;
import me.zort.sqllib.transaction.Transaction;
import me.zort.sqllib.util.Validator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Main database client object implementation.
 * This class is responsible for handling requests from query
 * objects of this library.
 *
 * @author ZorTik
 */
@SuppressWarnings("unused")
public class SQLDatabaseConnectionImpl extends PooledSQLDatabaseConnection {

  @NotNull
  static SQLDatabaseOptions defaultOptions() {
    return new SQLDatabaseOptions(DEFAULT_AUTO_RECONNECT, DEFAULT_DEBUG, DEFAULT_LOG_SQL_ERRORS, DEFAULT_NAMING_STRATEGY, DEFAULT_GSON);
  }

  // --***-- Default Constants --***--

  public static final String DEFAULT_DRIVER = Defaults.DEFAULT_DRIVER;
  public static final boolean DEFAULT_AUTO_RECONNECT = Defaults.DEFAULT_AUTO_RECONNECT;
  public static final boolean DEFAULT_DEBUG = Defaults.DEFAULT_DEBUG;
  public static final boolean DEFAULT_LOG_SQL_ERRORS = Defaults.DEFAULT_LOG_SQL_ERRORS;
  public static final NamingStrategy DEFAULT_NAMING_STRATEGY = Defaults.DEFAULT_NAMING_STRATEGY;
  public static final Gson DEFAULT_GSON = Defaults.DEFAULT_GSON;

  // --***-- Options & Utilities --***--

  @Getter
  private final ISQLDatabaseOptions options;
  private final transient StatementMappingRegistry mappingRegistry;
  private transient StatementMappingFactory mappingFactory;
  private transient ObjectMapper objectMapper;
  private transient CacheManager cacheManager;
  @Setter
  private transient Logger logger;
  @Getter(onMethod_ = {@Nullable, @ApiStatus.Experimental})
  private transient Transaction transaction;
  private transient SchemaSynchronizer<SQLDatabaseConnection> schemaSynchronizer;

  /**
   * Constructs new instance of this implementation with default
   * options.
   *
   * @see SQLDatabaseConnectionImpl#SQLDatabaseConnectionImpl(SQLConnectionFactory, ISQLDatabaseOptions)
   */
  public SQLDatabaseConnectionImpl(final @NotNull SQLConnectionFactory connectionFactory) {
    this(connectionFactory, null);
  }

  /**
   * Constructs new instance of this implementation.
   *
   * @param connectionFactory Factory to use while opening connection.
   * @param options           Client options to use.
   */
  public SQLDatabaseConnectionImpl(final @NotNull SQLConnectionFactory connectionFactory, @Nullable ISQLDatabaseOptions options) {
    super(connectionFactory);
    this.options = options == null ? defaultOptions() : options;
    this.objectMapper = new DefaultObjectMapper(this);
    this.mappingFactory = new DefaultStatementMappingFactory();
    this.transaction = null;
    this.logger = Logger.getGlobal();
    this.mappingRegistry = new MappingRegistryImpl(this);

    setSchemaSynchronizer(new SQLSchemaSynchronizer());
    enableCaching(CacheManager.noCache());

    // Default backup value resolvers.
    registerBackupValueResolver(new LinkedOneFieldResolver());
    registerBackupValueResolver(new ConstructorParameterResolver());
  }

  /**
   * Registers a backup value resolver to the registry.
   * Backup value resolvers are used when no value is found for mapped
   * field in {@link ObjectMapper}.
   *
   * @param resolver Resolver to register.
   */
  public void registerBackupValueResolver(final @NotNull ObjectMapper.FieldValueResolver resolver) {
    Objects.requireNonNull(resolver, "Resolver cannot be null!");

    objectMapper.registerBackupValueResolver(resolver);
  }

  /**
   * Sets the object mapper to use.
   * Object mapper maps queries to objects, as specified in {@link SQLDatabaseConnection#query(Query, Class)}.
   *
   * @param objectMapper Object mapper to use.
   */
  public void setObjectMapper(final @NotNull ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper cannot be null!");
  }

  /**
   * Sets a mapping to use when using {@link SQLDatabaseConnection#createProxy(Class, StatementMappingOptions)}.
   *
   * @param mappingFactory Mapping factory to use.
   */
  public void setProxyMapping(final @NotNull StatementMappingFactory mappingFactory) {
    this.mappingFactory = Objects.requireNonNull(mappingFactory, "Mapping factory cannot be null!");
  }

  /**
   * Enabled caching for this connection.
   *
   * @param cacheManager Cache manager to use.
   */
  @ApiStatus.Experimental
  @Override
  public void enableCaching(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /**
   * Synchronizes proxy mapping models with database.<br>
   * <i>Warning:</i> This tries to update all tables that are part of mapping
   * proxies. Should be used carefully.
   *
   * @return True if there were any changes
   */
  @ApiStatus.Experimental
  @Override
  public boolean synchronizeModel() {
    return mappingRegistry.getProxyInstances()
            .stream().flatMap(i -> i.getTableSchemas(
                    getOptions().getNamingStrategy(),
                    this instanceof SQLiteDatabaseConnection).stream())
            .anyMatch(schema -> synchronizeModel(schema, schema.getTable()));
  }

  /**
   * Synchronizes provided entity schema with the database
   * In other words, it tries to update database schema (table) to match
   * the provided entity schema.
   *
   * @param entitySchema Entity schema
   * @param table        Table name
   * @return True if there were any changes
   */
  @ApiStatus.Experimental
  @Override
  public boolean synchronizeModel(TableSchema entitySchema, String table) {
    QueryResult result = getSchemaSynchronizer().synchronize(this, entitySchema,
            getSchemaBuilder(table).buildTableSchema());
    return result != QueryResult.noChangesResult && result.isSuccessful();
  }

  /**
   * Synchronizes provided entity class with the database
   * In other words, it tries to update database schema (table) to match
   * the provided entity schema. This method uses synchronizeModel method
   * with schema generated from the provided entity class.
   *
   * @param entity The entity (model) class
   * @param table  Table name
   * @return True if there were any changes
   */
  @ApiStatus.Experimental
  @Override
  public boolean synchronizeModel(Class<?> entity, String table) {
    return synchronizeModel(new EntitySchemaBuilder(table, entity,
            getOptions().getNamingStrategy(),
            this instanceof SQLiteDatabaseConnection).buildTableSchema(), table);
  }

  /**
   * Sets the schema synchronizer to use with synchronizeModel methods.
   *
   * @param synchronizer Schema synchronizer to use
   */
  @ApiStatus.Experimental
  @Override
  public void setSchemaSynchronizer(SchemaSynchronizer<SQLDatabaseConnection> synchronizer) {
    this.schemaSynchronizer = synchronizer;
  }

  @ApiStatus.Experimental
  @Override
  public SchemaSynchronizer<SQLDatabaseConnection> getSchemaSynchronizer() {
    return schemaSynchronizer;
  }

  @ApiStatus.Experimental
  @Override
  public StatementMappingRegistry getMappingRegistry() {
    return mappingRegistry;
  }

  /**
   * Constructs a mapping proxy based on provided interface.
   * The interface should follow rules for creating mapping repositories
   * in this library.
   *
   * @param mappingInterface Interface to create mapping repository for.
   * @param <T>              Type of mapping repository.
   * @return Mapping repository.
   * @see SQLDatabaseConnection#createProxy(Class, StatementMappingOptions)
   */
  public <T> T createProxy(Class<T> mappingInterface) {
    return createProxy(mappingInterface, new StatementMappingOptions.Builder().build());
  }

  /**
   * Replaced with {@link SQLDatabaseConnection#createProxy(Class)}.
   *
   * @deprecated Will be removed in future releases.
   */
  @Deprecated
  @Override
  public <T> T createGate(Class<T> mappingInterface) {
    return createProxy(mappingInterface);
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
   * @param <T>              Type of mapping repository.
   * @return Mapping repository.
   */
  @SuppressWarnings("unchecked")
  public final <T> T createProxy(final @NotNull Class<T> mappingInterface, final @NotNull StatementMappingOptions options) {
    Objects.requireNonNull(mappingInterface, "Mapping interface cannot be null!");
    Objects.requireNonNull(options, "Options cannot be null!");

    AtomicReference<MappingProxyInstance<T>> instanceReference = new AtomicReference<>();
    T rawInstance = (T) Proxy.newProxyInstance(mappingInterface.getClassLoader(),
            new Class[]{mappingInterface}, (proxy, method, args) -> instanceReference.get().invoke(proxy, method, args));
    instanceReference.set(new ProxyInstanceImpl<>(mappingInterface,
            options,
            mappingFactory.strategy(mappingInterface, this),
            mappingFactory.resultAdapter()));

    MappingProxyInstance<T> proxyInstanceWrapper = instanceReference.get();
    mappingRegistry.registerProxy(proxyInstanceWrapper);
    return rawInstance;
  }

  @ApiStatus.Experimental
  public final boolean buildEntitySchema(final @NotNull String tableName, final @NotNull Class<?> entityClass) {
    Objects.requireNonNull(entityClass, "Entity class cannot be null!");

    EntitySchemaBuilder converter = new EntitySchemaBuilder(tableName, entityClass, options.getNamingStrategy(), this instanceof SQLiteDatabaseConnection);
    String query = converter.buildTableQuery();

    return exec(() -> query).isSuccessful();
  }

  /**
   * Performs new query and returns the result. This result is never null.
   * This method also maps the result to the specified type using {@link ObjectMapper}.
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
  @NotNull
  @Override
  public <T> QueryRowsResult<T> query(final @NotNull Query query, final @NotNull Class<T> typeClass) {
    Objects.requireNonNull(query);
    Objects.requireNonNull(typeClass);

    QueryRowsResult<Row> resultRows = query(query.getAncestor());
    QueryRowsResult<T> result = new QueryRowsResult<>(resultRows.isSuccessful());

    for (Row row : resultRows) {
      Optional.ofNullable(objectMapper.assignValues(row, typeClass))
              .ifPresent(result::add);
    }
    return result;
  }

  /**
   * Performs new query and returns the result. This result is never null.
   *
   * @param query The query to use
   */
  @NotNull
  @Override
  public QueryRowsResult<Row> query(final @NotNull Query query) {
    return query(query, false);
  }

  public QueryRowsResult<Row> query(final @NotNull String query) {
    return query(() -> query);
  }

  @SuppressWarnings("unchecked")
  @NotNull
  QueryRowsResult<Row> query(final @NotNull Query query, boolean isRetry) {
    Objects.requireNonNull(query);
    if (!handleAutoReconnect())
      return new QueryRowsResult<>(false, "Cannot connect to database!");

    QueryResult cachedResult = cacheManager.get(query, false);
    if (cachedResult instanceof QueryRowsResult) return (QueryRowsResult<Row>) cachedResult;

    try (PreparedStatement stmt = buildStatement(query);
         ResultSet resultSet = stmt.executeQuery()) {
      QueryRowsResult<Row> result = new QueryRowsResult<>(true);
      while (resultSet.next()) {
        ResultSetMetaData meta = resultSet.getMetaData();
        Row row = new Row();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
          Object obj = resultSet.getObject(i);
          if (obj instanceof String) obj = ((String) obj).replaceAll("''", "'");
          row.put(meta.getColumnName(i), obj);
        }
        result.add(row);
      }

      cacheManager.set(query, result);
      debug(result);
      return result;
    } catch (SQLException e) {
      if (!isRetry && e.getMessage().contains("database connection closed")) {
        reconnect();
        return query(query, true);
      }

      logSqlError(e);
      notifyHandlers(Code.QUERY_FATAL);
      query.errorSignal(e);
      return new QueryRowsResult<>(false, e.getMessage());
    }
  }

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
  public QueryResult exec(final @NotNull Query query) {
    return exec(query, false);
  }

  public QueryResult exec(final @NotNull String query) {
    return exec(() -> query);
  }

  @NotNull QueryResult exec(final @NotNull Query query, boolean isRetry) {
    if (!handleAutoReconnect()) {
      return new QueryResultImpl(false, "Cannot connect to database!");
    }

    QueryResult cachedResult = cacheManager.get(query, true);
    if (cachedResult != null) return cachedResult;

    try (PreparedStatement stmt = buildStatement(query)) {
      stmt.execute();
      QueryResultImpl result = new QueryResultImpl(true);
      cacheManager.set(query, result);
      debug(result);
      return result;
    } catch (SQLException e) {
      if (!isRetry && e.getMessage().contains("database connection closed")) {
        reconnect();
        return exec(query, true);
      }

      logSqlError(e);
      notifyHandlers(Code.QUERY_FATAL);
      query.errorSignal(e);
      return new QueryResultImpl(false, e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  @Nullable
  protected final DefsVals buildDefsVals(Object obj) {
    Objects.requireNonNull(obj);

    Class<?> aClass = obj.getClass();

    Map<String, Object> fields = new HashMap<>();
    for (Field field : aClass.getDeclaredFields()) {

      if (Modifier.isTransient(field.getModifiers())) {
        // Transient fields are ignored.
        continue;
      }

      try {
        field.setAccessible(true);
        Object o = field.get(obj);
        if (field.isAnnotationPresent(JsonField.class)) {
          o = options.getGson().toJson(o);
        } else if (Validator.validateAutoIncrement(field) && field.get(obj) == null) {
          // If field is PrimaryKey and autoIncrement true and is null,
          // We will skip this to use auto increment strategy on SQL server.
          continue;
        }
        fields.put(options.getNamingStrategy().fieldNameToColumn(field.getName()), o);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
        return null;
      }
    }
    // I make entry array for indexing safety.
    Map.Entry<String, Object>[] entryArray = fields.entrySet().toArray(new Map.Entry[0]);
    String[] defs = new String[entryArray.length];
    AtomicReference<Object>[] vals = new AtomicReference[entryArray.length];
    for (int i = 0; i < entryArray.length; i++) {
      defs[i] = entryArray[i].getKey();
      vals[i] = new AtomicReference<>(entryArray[i].getValue());
    }
    return new DefsVals(defs, vals);
  }

  @ApiStatus.Experimental
  @SneakyThrows(SQLException.class)
  public final Transaction beginTransaction() {
    Connection rawConnection = getConnection();
    if (transaction != null && transaction.isActive()) {
      throw new IllegalStateException("There is already an active transaction!");
    } else if (rawConnection == null) {
      throw new IllegalStateException("Connection is not established!");
    }
    rawConnection.setAutoCommit(false);
    transaction = new Transaction(this);
    return transaction;
  }

  @ApiStatus.Experimental
  @SneakyThrows
  public final void closeTransaction() {
    Transaction transaction = getTransaction();
    if (transaction != null && transaction.isActive()) transaction.commit();
    this.transaction = null;
  }

  public final void rollback() throws SQLException {
    if (transaction == null || !transaction.isActive()) {
      throw new IllegalStateException("There is no active transaction!");
    }
    transaction.rollback();
  }

  @SuppressWarnings("all")
  private boolean handleAutoReconnect() {
    if (options.isAutoReconnect() && !isConnected()) {
      return reconnect();
    }
    return true;
  }

  private boolean reconnect() {
    debug("Trying to make a new connection with the database!");
    if (!connect()) {
      debug("Cannot make new connection!");
      return false;
    }
    return true;
  }

  public void debug(String message) {
    if (options.isDebug()) logger.info(message);
  }

  private void debug(QueryResult result) {
    debug("Query result: " + result);
    if (result instanceof QueryRowsResult) {
      debug("Rows: " + ((QueryRowsResult<?>) result).size());
    }
  }

  @Override
  public void close() {
    if (getErrorCount() > 0 && getAssignedPool() != null) {
      // If there was any error and this connection is part of a pool,
      // we won't return object to the pool, but disconnect.
      disconnect();
      return;
    }

    super.close();
  }

  @Override
  public final boolean isLogSqlErrors() {
    return options.isLogSqlErrors();
  }

  @Override
  public final boolean isDebug() {
    return options.isDebug();
  }

  @Override
  public final boolean isTransactionActive() {
    return transaction != null && transaction.isActive();
  }

  @Override
  public TableSchemaBuilder getSchemaBuilder(String table) {
    return new DatabaseSchemaBuilder(q -> {
      try {
        return buildStatement(() -> q);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }, table);
  }

  public final SQLDatabaseOptions cloneOptions() {
    SQLDatabaseOptions cloned = new SQLDatabaseOptions();
    cloned.setDebug(options.isDebug());
    cloned.setLogSqlErrors(options.isLogSqlErrors());
    cloned.setNamingStrategy(options.getNamingStrategy());
    cloned.setGson(options.getGson());
    cloned.setAutoReconnect(options.isAutoReconnect());
    return cloned;
  }

  @SuppressWarnings("unchecked")
  private PreparedStatement buildStatement(Query query) throws SQLException {
    StatementFactory<PreparedStatement> factory = new DefaultStatementFactory(query);
    if (query instanceof StatementFactory)
      factory = (StatementFactory<PreparedStatement>) query;

    return factory.prepare(getConnection());
  }

  @RequiredArgsConstructor
  static class DefaultStatementFactory implements StatementFactory<PreparedStatement> {

    private final Query query;

    @Override
    public PreparedStatement prepare(Connection connection) throws SQLException {
      String queryString = query.getAncestor().buildQuery();

      SQLConnectionRegistry.debug(connection, "Query: " + queryString);
      return connection.prepareStatement(queryString);
    }
  }

}
