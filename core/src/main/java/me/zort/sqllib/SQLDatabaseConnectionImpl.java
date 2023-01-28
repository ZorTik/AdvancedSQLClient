package me.zort.sqllib;

import com.google.gson.Gson;
import lombok.*;
import me.zort.sqllib.api.*;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.options.NamingStrategy;
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
import me.zort.sqllib.util.Pair;
import me.zort.sqllib.util.Validator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/**
 * Main database client object implementation.
 * This class is responsible for handling requests from query
 * objects of this library.
 *
 * @author ZorTik
 */
public class SQLDatabaseConnectionImpl extends SQLDatabaseConnection {

    // --***-- Default Constants --***--

    public static boolean DEFAULT_AUTO_RECONNECT = true;
    public static boolean DEFAULT_DEBUG = false;
    public static boolean DEFAULT_LOG_SQL_ERRORS = true;
    public static NamingStrategy DEFAULT_NAMING_STRATEGY = new DefaultNamingStrategy();
    public static Gson DEFAULT_GSON = Defaults.DEFAULT_GSON;

    // --***-- Options & Utilities --***--

    @Getter
    private final SQLDatabaseOptions options;
    @ApiStatus.Experimental
    private final transient StatementMappingFactory mappingFactory;
    @ApiStatus.Experimental
    private final transient StatementMappingResultAdapter mappingResultAdapter;
    private transient ObjectMapper objectMapper;

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
     * Constructs a mapping repository based on provided interface.
     * The interface should follow rules for creating mapping repositories
     * in this library.
     *
     * @param mappingInterface Interface to create mapping repository for.
     * @return Mapping repository.
     * @param <T> Type of mapping repository.
     */
    @SuppressWarnings("unchecked")
    @ApiStatus.Experimental
    public <T> T createMapping(Class<T> mappingInterface) {
        StatementMappingStrategy<T> statementMapping = mappingFactory.create(mappingInterface, this);
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

                    return method.invoke(this, args);
                });
    }

    /**
     * @see SQLDatabaseConnection#query(Query, Class)
     */
    @Override
    public <T> QueryRowsResult<T> query(Query query, Class<T> typeClass) {
        QueryRowsResult<Row> resultRows = query(query.getAncestor());
        QueryRowsResult<T> result = new QueryRowsResult<>(resultRows.isSuccessful());

        for(Row row : resultRows) {
            Optional.ofNullable(objectMapper.assignValues(row, typeClass))
                    .ifPresent(result::add);
        }
        return result;
    }

    /**
     * @see SQLDatabaseConnectionImpl#query(Query, Class)
     */
    @Override
    public QueryRowsResult<Row> query(Query query) {
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
            logSqlError(e);
            return new QueryRowsResult<>(false, e.getMessage());
        }
    }

    /**
     * @see SQLDatabaseConnection#exec(Query)
     */
    public QueryResult exec(Query query) {
        if(!handleAutoReconnect()) {
            return new QueryResultImpl(false, "Cannot connect to database!");
        }
        try(PreparedStatement stmt = buildStatement(query)) {
            stmt.execute();
            return new QueryResultImpl(true);
        } catch (SQLException e) {
            logSqlError(e);
            return new QueryResultImpl(false, e.getMessage());
        }
    }

    /**
     * @see SQLDatabaseConnection#save(String, Object)
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

    private boolean handleAutoReconnect() {
        if(options.isAutoReconnect() && !isConnected()) {
            debug("Trying to make a new connection with the database!");
            if(!connect()) {
                debug("Cannot make new connection!");
                return false;
            }
        }
        return true;
    }

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

    public UpsertQuery save(Object obj) {
        Pair<String[], UnknownValueWrapper[]> data = buildDefsVals(obj);

        if(data == null) {
            return null;
        }

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
        if(options.isDebug()) {
            System.out.println(message);
        }
    }

    @Override
    public boolean isLogSqlErrors() {
        return options.isLogSqlErrors();
    }

    @Override
    public boolean isDebug() {
        return options.isDebug();
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

            Logger.debug(connection, "Query: " + queryString);
            return connection.prepareStatement(queryString);
        }
    }

    @AllArgsConstructor
    @Data
    public static class UnknownValueWrapper {
        private Object object;
    }

}
