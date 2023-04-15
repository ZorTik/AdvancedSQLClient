package me.zort.sqllib;

import com.google.gson.Gson;
import lombok.*;
import me.zort.sqllib.api.ObjectMapper;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.StatementFactory;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.mapping.StatementMappingFactory;
import me.zort.sqllib.api.mapping.StatementMappingResultAdapter;
import me.zort.sqllib.api.mapping.StatementMappingStrategy;
import me.zort.sqllib.api.options.NamingStrategy;
import me.zort.sqllib.api.repository.SQLTableRepository;
import me.zort.sqllib.internal.Defaults;
import me.zort.sqllib.internal.annotation.JsonField;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.fieldResolver.ConstructorParameterResolver;
import me.zort.sqllib.internal.fieldResolver.LinkedOneFieldResolver;
import me.zort.sqllib.internal.impl.DefaultNamingStrategy;
import me.zort.sqllib.internal.impl.DefaultObjectMapper;
import me.zort.sqllib.internal.impl.QueryResultImpl;
import me.zort.sqllib.internal.query.*;
import me.zort.sqllib.internal.query.part.SetStatement;
import me.zort.sqllib.mapping.DefaultResultAdapter;
import me.zort.sqllib.mapping.DefaultStatementMappingFactory;
import me.zort.sqllib.pool.PooledSQLDatabaseConnection;
import me.zort.sqllib.util.Pair;
import me.zort.sqllib.util.Validator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static me.zort.sqllib.util.ExceptionsUtility.runCatching;

/**
 * Main database client object implementation.
 * This class is responsible for handling requests from query
 * objects of this library.
 *
 * @author ZorTik
 */
@SuppressWarnings("unused")
public class SQLDatabaseConnectionImpl extends PooledSQLDatabaseConnection {

    // --***-- Default Constants --***--

    public static final boolean DEFAULT_AUTO_RECONNECT = true;
    public static final boolean DEFAULT_DEBUG = false;
    public static final boolean DEFAULT_LOG_SQL_ERRORS = true;
    public static final NamingStrategy DEFAULT_NAMING_STRATEGY = new DefaultNamingStrategy();
    public static final Gson DEFAULT_GSON = Defaults.DEFAULT_GSON;

    // --***-- Options & Utilities --***--

    @Getter
    private final SQLDatabaseOptions options;
    @ApiStatus.Experimental
    private final transient StatementMappingFactory mappingFactory;
    @ApiStatus.Experimental
    private final transient StatementMappingResultAdapter mappingResultAdapter;
    private final transient List<ErrorStateObserver> errorStateHandlers;
    private transient ObjectMapper objectMapper;
    @Setter
    private transient Logger logger;

    /**
     * Constructs new instance of this implementation with default
     * options.
     *
     * @see SQLDatabaseConnectionImpl#SQLDatabaseConnectionImpl(SQLConnectionFactory, SQLDatabaseOptions)
     */
    public SQLDatabaseConnectionImpl(SQLConnectionFactory connectionFactory) {
        this(connectionFactory, null);
    }

    /**
     * Constructs new instance of this implementation.
     *
     * @param connectionFactory Factory to use while opening connection.
     * @param options Client options to use.
     */
    public SQLDatabaseConnectionImpl(SQLConnectionFactory connectionFactory, @Nullable SQLDatabaseOptions options) {
        super(connectionFactory);

        if (options == null)
            options = new SQLDatabaseOptions(DEFAULT_AUTO_RECONNECT,
                    DEFAULT_DEBUG,
                    DEFAULT_LOG_SQL_ERRORS,
                    DEFAULT_NAMING_STRATEGY,
                    DEFAULT_GSON
            );

        this.options = options;
        this.objectMapper = new DefaultObjectMapper(this);
        this.mappingFactory = new DefaultStatementMappingFactory();
        this.mappingResultAdapter = new DefaultResultAdapter();
        this.errorStateHandlers = new CopyOnWriteArrayList<>();
        this.logger = Logger.getGlobal();

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
    public void registerBackupValueResolver(@NotNull ObjectMapper.FieldValueResolver resolver) {
        Objects.requireNonNull(resolver, "Resolver cannot be null!");

        objectMapper.registerBackupValueResolver(resolver);
    }

    /**
     * Sets the object mapper to use.
     * Object mapper maps queries to objects, as specified in {@link SQLDatabaseConnection#query(Query, Class)}.
     *
     * @param objectMapper Object mapper to use.
     */
    public void setObjectMapper(@NotNull ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper cannot be null!");
    }

    /**
     * Adds an error state observer to the list of observers to be
     * notified when a fatal error occurs.
     *
     * @param observer Observer to add.
     */
    public void addErrorHandler(@NotNull ErrorStateObserver observer) {
        this.errorStateHandlers.add(observer);
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
    @SuppressWarnings("unchecked")
    @ApiStatus.Experimental
    public final <T> T createGate(Class<T> mappingInterface) {
        Objects.requireNonNull(mappingInterface, "Mapping interface cannot be null!");

        StatementMappingStrategy<T> statementMapping = mappingFactory.create(mappingInterface, this);

        List<Method> pendingMethods = new CopyOnWriteArrayList<>();

        return (T) Proxy.newProxyInstance(mappingInterface.getClassLoader(),
                new Class[]{mappingInterface}, (proxy, method, args) -> {

                    // Allow invokation from interfaces or abstract classes only.
                    Class<?> declaringClass = method.getDeclaringClass();
                    if ((declaringClass.isInterface() || Modifier.isAbstract(declaringClass.getModifiers()))
                            && statementMapping.isMappingMethod(method)) {
                        // Prepare and execute query based on invoked method.
                        QueryResult result = statementMapping.executeQuery(method, args, mappingResultAdapter.retrieveResultType(method));
                        // Adapt QueryResult to method return type.
                        return mappingResultAdapter.adaptResult(method, result);
                    }

                    // Default methods are invoked normally.
                    if (declaringClass.isInterface() && method.isDefault()) {
                        return JVM.getJVM().invokeDefault(declaringClass, proxy, method, args);
                    }

                    throw new UnsupportedOperationException("Method " + method.getName() + " is not supported by this mapping repository!");
                });
    }

    @SuppressWarnings("unchecked, rawtypes")
    @ApiStatus.Experimental
    public final boolean buildEntitySchema(String tableName, Class<?> entityClass) {
        Objects.requireNonNull(entityClass, "Entity class cannot be null!");

        SQLTableRepository repository = new SQLTableRepositoryBuilder()
                .withConnection(this)
                .withTableName(tableName)
                .withTypeClass(entityClass)
                .build();
        return repository.createTable();
    }

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
    @Override
    public <T> QueryRowsResult<T> query(Query query, Class<T> typeClass) {
        Objects.requireNonNull(query);
        Objects.requireNonNull(typeClass);

        QueryRowsResult<Row> resultRows = query(query.getAncestor());
        QueryRowsResult<T> result = new QueryRowsResult<>(resultRows.isSuccessful());

        for(Row row : resultRows) {
            Optional.ofNullable(objectMapper.assignValues(row, typeClass))
                    .ifPresent(result::add);
        }
        return result;
    }

    /**
     * Performs new query and returns the result. This result is never null.
     *
     * @see SQLDatabaseConnection#query(Query, Class)
     */
    @Override
    public QueryRowsResult<Row> query(Query query) {
        return doQuery(query, false);
    }

    private QueryRowsResult<Row> doQuery(Query query, boolean isRetry) {
        Objects.requireNonNull(query);

        if(!handleAutoReconnect()) {
            return new QueryRowsResult<>(false, "Cannot connect to database!");
        }

        try(PreparedStatement stmt = buildStatement(query);
            ResultSet resultSet = stmt.executeQuery()) {
            QueryRowsResult<Row> result = new QueryRowsResult<>(true);

            while(resultSet.next()) {
                ResultSetMetaData meta = resultSet.getMetaData();
                Row row = new Row();
                for(int i = 1; i <= meta.getColumnCount(); i++) {
                    Object obj = resultSet.getObject(i);
                    if(obj instanceof String) {
                        obj = ((String) obj).replaceAll("''", "'");
                    }
                    row.put(meta.getColumnName(i), obj);
                }
                result.add(row);
            }

            return result;
        } catch (SQLException e) {
            if (!isRetry && e.getMessage().contains("database connection closed")) {
                reconnect();
                return doQuery(query, true);
            }

            logSqlError(e);
            notifyError(ErrorCode.QUERY_FATAL);
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
    public QueryResult exec(Query query) {
        return doExec(query, false);
    }

    private QueryResult doExec(Query query, boolean isRetry) {
        if(!handleAutoReconnect()) {
            return new QueryResultImpl(false, "Cannot connect to database!");
        }
        try(PreparedStatement stmt = buildStatement(query)) {
            stmt.execute();
            return new QueryResultImpl(true);
        } catch (SQLException e) {
            if (!isRetry && e.getMessage().contains("database connection closed")) {
                reconnect();
                return doExec(query, true);
            }

            logSqlError(e);
            notifyError(ErrorCode.QUERY_FATAL);
            return new QueryResultImpl(false, e.getMessage());
        }
    }

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
    @Override
    public QueryResult save(String table, Object obj) { // by default, it creates and upsert request.
        Pair<String[], UnknownValueWrapper[]> data = buildDefsVals(obj);

        if(data == null) {
            return new QueryResultImpl(false);
        }

        return save(obj).table(table).execute();
    }

    public QueryResult insert(String table, Object obj) {
        Pair<String[], UnknownValueWrapper[]> data = buildDefsVals(obj);

        if (data == null)
            return new QueryResultImpl(false);

        InsertQuery query = insert().into(table, data.getFirst());
        for (UnknownValueWrapper valueWrapper : data.getSecond()) {
            query.appendVal(valueWrapper.getObject());
        }

        return query.execute();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected Pair<String[], UnknownValueWrapper[]> buildDefsVals(Object obj) {
        Objects.requireNonNull(obj);

        Class<?> aClass = obj.getClass();

        Map<String, Object> fields = new HashMap<>();
        for(Field field : aClass.getDeclaredFields()) {

            if(Modifier.isTransient(field.getModifiers())) {
                // Transient fields are ignored.
                continue;
            }

            try {
                field.setAccessible(true);
                Object o = field.get(obj);
                if(field.isAnnotationPresent(JsonField.class)) {
                    o = options.getGson().toJson(o);
                } else if(Validator.validateAutoIncrement(field) && field.get(obj) == null) {
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
        UnknownValueWrapper[] vals = new UnknownValueWrapper[entryArray.length];
        for(int i = 0; i < entryArray.length; i++) {
            defs[i] = entryArray[i].getKey();
            vals[i] = new UnknownValueWrapper(entryArray[i].getValue());
        }
        return new Pair<>(defs, vals);
    }

    @SuppressWarnings("all")
    private boolean handleAutoReconnect() {
        if(options.isAutoReconnect() && !isConnected()) {
            return reconnect();
        }
        return true;
    }

    private boolean reconnect() {
        debug("Trying to make a new connection with the database!");
        if(!connect()) {
            debug("Cannot make new connection!");
            return false;
        }
        return true;
    }

    public UpsertQuery save(Object obj) {
        Pair<String[], UnknownValueWrapper[]> data = buildDefsVals(obj);
        if(data == null) return null;

        String[] defs = data.getFirst();
        UnknownValueWrapper[] vals = data.getSecond();
        UpsertQuery upsert = upsert().into(null, defs);
        for(UnknownValueWrapper wrapper : vals) {
            upsert.appendVal(wrapper.getObject());
        }
        SetStatement<InsertQuery> setStmt = upsert.onDuplicateKey();
        for(int i = 0; i < defs.length; i++) {
            setStmt.and(defs[i], vals[i].getObject());
        }

        return (UpsertQuery) setStmt.getAncestor();
    }

    public void debug(String message) {
        if(options.isDebug()) logger.info(message);
    }

    @Override
    public final boolean isLogSqlErrors() {
        return options.isLogSqlErrors();
    }

    @Override
    public final boolean isDebug() {
        return options.isDebug();
    }

    private void notifyError(int code) {
        this.errorStateHandlers.forEach(handler -> runCatching(() -> handler.onErrorState(code)));
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
    private static class DefaultStatementFactory implements StatementFactory<PreparedStatement> {

        private final Query query;

        @Override
        public PreparedStatement prepare(Connection connection) throws SQLException {
            String queryString = query.getAncestor().buildQuery();

            SQLConnectionRegistry.debug(connection, "Query: " + queryString);
            return connection.prepareStatement(queryString);
        }
    }

    @AllArgsConstructor
    @Data
    public static class UnknownValueWrapper {
        private Object object;
    }

    public interface ErrorStateObserver {
        void onErrorState(int code);
    }

    public static final class ErrorCode {
        public static final int QUERY_FATAL = 0;
    }

}
