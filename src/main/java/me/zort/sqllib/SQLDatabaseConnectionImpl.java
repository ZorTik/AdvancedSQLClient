package me.zort.sqllib;

import com.google.gson.Gson;
import lombok.Getter;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.annotation.JsonField;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.impl.QueryResultImpl;
import me.zort.sqllib.internal.query.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.sql.*;
import java.util.Optional;

/**
 * Main database client object implementation.
 * This class is responsible for handling requests from query
 * objects of this library.
 *
 * @author ZorTik
 */
public class SQLDatabaseConnectionImpl implements SQLDatabaseConnection {

    private final SQLConnectionFactory connectionFactory;
    @Getter
    private final SQLDatabaseOptions options;

    @Getter(onMethod_ = {@Nullable})
    private Connection connection;

    /**
     * Constructs new instance of this implementation with default
     * options.
     *
     * @see SQLDatabaseConnectionImpl#SQLDatabaseConnectionImpl(SQLConnectionFactory, SQLDatabaseOptions)
     */
    public SQLDatabaseConnectionImpl(SQLConnectionFactory connectionFactory) {
        this(connectionFactory, new SQLDatabaseOptions());
    }

    /**
     * Constructs new instance of this implementation.
     *
     * @param connectionFactory Factory to use while opening connection.
     * @param options Client options to use.
     */
    public SQLDatabaseConnectionImpl(SQLConnectionFactory connectionFactory, SQLDatabaseOptions options) {
        this.connectionFactory = connectionFactory;
        this.options = options;
        this.connection = null;
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
     * @param typeClass Type class of object which will be instantinated and
     *                  populated with column values.
     * @param <T> Type of objects in result.
     *
     * @return Collection of row objects.
     */
    public <T> QueryRowsResult<T> query(Query query, Class<T> typeClass) {
        QueryRowsResult<Row> resultRows = query(query);
        QueryRowsResult<T> result = new QueryRowsResult<>(resultRows.isSuccessful());
        for(Row row : resultRows) {
            Optional.ofNullable(assignValues(row, typeClass))
                    .ifPresent(result::add);
        }
        return result;
    }

    /**
     * @see SQLDatabaseConnectionImpl#query(Query, Class)
     */
    @Override
    public QueryRowsResult<Row> query(Query query) {
        if(!handleAutoReconnect()) {
            return new QueryRowsResult<>(false);
        }
        String queryString = query.buildQuery();
        try(PreparedStatement stmt = connection.prepareStatement(queryString);
            ResultSet resultSet = stmt.executeQuery()) {
            QueryRowsResult<Row> result = new QueryRowsResult<>(true);
            while(resultSet.next()) {
                ResultSetMetaData meta = resultSet.getMetaData();
                Row row = new Row();
                for(int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnName(i), resultSet.getObject(i));
                }
                result.add(row);
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return new QueryRowsResult<>(false);
        }
    }

    /**
     * @see SQLDatabaseConnection#exec(Query)
     */
    public QueryResult exec(Query query) {
        if(!handleAutoReconnect()) {
            return new QueryResultImpl(false);
        }
        String queryString = query.buildQuery();
        try(PreparedStatement stmt = connection.prepareStatement(queryString)) {
            stmt.execute();
            return new QueryResultImpl(true);
        } catch (SQLException e) {
            e.printStackTrace();
            return new QueryResultImpl(false);
        }
    }

    @Nullable
    private <T> T assignValues(Row row, Class<T> typeClass) {
        T instance = null;
        try {
            try {
                Constructor<T> c = typeClass.getConstructor();
                instance = c.newInstance();
            } catch (NoSuchMethodException e) {
                for(Constructor<?> c : typeClass.getConstructors()) {
                    if(c.getParameterCount() == row.size()) {
                        Parameter[] params = c.getParameters();
                        Object[] vals = new Object[c.getParameterCount()];
                        for(int i = 0; i < row.size(); i++) {
                            Parameter param = params[i];
                            vals[i] = buildElementValue(param, row);
                        }
                        try {
                            instance = (T) c.newInstance(vals);
                        } catch(Exception ignored) {
                            continue;
                        }
                    }
                }
            }
            for(Field field : typeClass.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    field.set(instance, buildElementValue(field, row));
                } catch(SecurityException ignored) {
                    debug(String.format("Field %s on class %s cannot be set accessible!",
                            field.getName(),
                            typeClass.getName()));
                } catch(Exception ignored) {
                    continue;
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            debug("Cannot instantinate " + typeClass.getName() + " for assigning attributes from row!");
            e.printStackTrace();
            return null;
        }
        return instance;
    }

    @Nullable
    private Object buildElementValue(AnnotatedElement element, Row row) {
        String name;
        Type type;
        if(element instanceof Field) {
            name = ((Field) element).getName();
            type = ((Field) element).getGenericType();
        } else if(element instanceof Parameter) {
            name = ((Parameter) element).getName();
            type = ((Parameter) element).getType();
        } else {
            return null;
        }
        Object obj = row.get(name);
        if(obj == null) {
            String converted;
            if((obj = row.get(converted = options.getNamingStrategy().convert(name))) == null) {
                debug(String.format("Cannot find column for target %s (%s)", name, converted));
                return null;
            }
        }
        if(element.isAnnotationPresent(JsonField.class) && obj instanceof String) {
            String jsonString = (String) obj;
            Gson gson = options.getGson();
            return gson.fromJson(jsonString, type);
        } else {
            return obj;
        }
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

    /**
     * @see SQLConnection#connect()
     */
    @Override
    public boolean connect() {
        if(isConnected()) {
            disconnect();
        }
        try {
            connection = connectionFactory.connect();
        } catch (SQLException e) {
            e.printStackTrace();
            connection = null;
        }
        return isConnected();
    }

    /**
     * @see SQLConnection#disconnect()
     */
    @Override
    public void disconnect() {
        if(isConnected()) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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

    private void debug(String message) {
        if(options.isDebug()) {
            System.out.println(message);
        }
    }

}
