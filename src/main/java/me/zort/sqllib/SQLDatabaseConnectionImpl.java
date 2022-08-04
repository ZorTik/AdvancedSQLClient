package me.zort.sqllib;

import com.google.gson.Gson;
import lombok.Getter;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.annotation.JsonField;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.query.SelectQuery;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.sql.*;
import java.util.Optional;

public class SQLDatabaseConnectionImpl implements SQLDatabaseConnection {

    private final SQLConnectionFactory connectionFactory;
    @Getter
    private final SQLDatabaseOptions options;

    @Getter(onMethod_ = {@Nullable})
    private Connection connection;

    public SQLDatabaseConnectionImpl(SQLConnectionFactory connectionFactory) {
        this(connectionFactory, new SQLDatabaseOptions());
    }

    public SQLDatabaseConnectionImpl(SQLConnectionFactory connectionFactory, SQLDatabaseOptions options) {
        this.connectionFactory = connectionFactory;
        this.options = options;
        this.connection = null;
    }

    public SelectQuery select(String... cols) {
        return new SelectQuery(this, cols);
    }

    public <T> QueryRowsResult<T> query(Query query, Class<T> typeClass) {
        QueryRowsResult<Row> resultRows = query(query);
        QueryRowsResult<T> result = new QueryRowsResult<>(resultRows.isSuccessful());
        for(Row row : resultRows) {
            Optional.ofNullable(assignValues(row, typeClass))
                    .ifPresent(result::add);
        }
        return result;
    }

    @Override
    public QueryRowsResult<Row> query(Query query) {
        if(options.isAutoReconnect() && !isConnected()) {
            debug("Trying to make a new connection with the database!");
            if(!connect()) {
                debug("Cannot make new connection!");
                return new QueryRowsResult<>(false);
            }
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

    private void debug(String message) {
        if(options.isDebug()) {
            System.out.println(message);
        }
    }

}
