package me.zort.sqllib.internal.query;

import lombok.*;
import me.zort.sqllib.Logger;
import me.zort.sqllib.util.Pair;
import me.zort.sqllib.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;

@AllArgsConstructor
@Getter
public class QueryDetails {

    public static QueryDetails empty() {
        return new QueryDetails();
    }

    @Setter(AccessLevel.PROTECTED)
    private String queryStr;
    private final Map<String, Object> values;

    public QueryDetails() { // Equiv to empty()
        this("", new HashMap<>());
    }

    public QueryDetails append(QueryDetails other) {
        return append("", other);
    }

    public QueryDetails append(String prefix, QueryDetails other) {
        Objects.requireNonNull(other, "QueryDetails cannot be null!");

        append(prefix + other.queryStr);
        other.values.forEach(values::putIfAbsent);
        return this;
    }

    public QueryDetails append(String s) {
        queryStr += s;
        return this;
    }

    // Creates prepared statement for execution in SQlDatabaseConnectionImpl class.
    protected PreparedStatement prepare(Connection connection) throws SQLException {
        Pair<String, Object[]> requirements = buildStatementDetails();

        // Shows plain query for prepared statement.
        Logger.debug(connection, String.format("P-Query: %s", requirements.getFirst()));
        Logger.debug(connection, String.format("P-Values: %s", Arrays.toString(requirements.getSecond())));

        PreparedStatement statement = connection.prepareStatement(requirements.getFirst());
        Object[] values = requirements.getSecond();
        for (int i = 0; i < values.length; i++) {
            set(statement, i + 1, values[i]);
        }
        return statement;
    }

    protected Pair<String, Object[]> buildStatementDetails() {
        String query = queryStr;
        Map<Integer, Object> valuesUnsorted = new HashMap<>();

        int i = 0;
        String queryCloned = query;
        for (String placeholder : this.values.keySet()) {
            Object value = this.values.get(placeholder);

            placeholder = String.format("<%s>", placeholder);

            if (Util.count(queryStr, placeholder) != 1)
                throw new RuntimeException("Placeholder " + placeholder + " is not unique in query " + queryStr);

            valuesUnsorted.put(queryCloned.indexOf(placeholder), value);
            query = query.replaceAll(placeholder, "?");

            i++;
        }

        Object[] values = new Object[valuesUnsorted.size()];
        valuesUnsorted.keySet()
                .stream()
                .mapToInt(Integer::intValue)
                .sorted().forEach(new IntConsumer() {
                    private int index = 0;
                    @Override
                    public void accept(int value) {
                        values[index] = valuesUnsorted.get(value);
                        index++;
                    }
                });

        return new Pair<>(query, values);
    }

    private static void set(PreparedStatement statement, int index, Object value) throws SQLException {
        switch(value.getClass().getSimpleName().toLowerCase()) {
            case "string":
                statement.setString(index, (String) value);
                break;
            case "integer":
            case "int":
                statement.setInt(index, (int) value);
                break;
            case "long":
                statement.setLong(index, (long) value);
                break;
            case "double":
                statement.setDouble(index, (double) value);
                break;
            case "float":
                statement.setFloat(index, (float) value);
                break;
            case "boolean":
                statement.setBoolean(index, (boolean) value);
                break;
            default:
                statement.setObject(index, value);
        }
    }

    public int length() {
        return queryStr.length();
    }

    @RequiredArgsConstructor
    public static class Builder {

        private final String query;
        private final Map<String, Object> values = new HashMap<>();

        // Name without brackets
        public Builder placeholder(String name, Object value) {
            if(!query.contains("<" + name + ">"))
                throw new IllegalArgumentException("Placeholder <" + name + "> not found in query!");

            values.put(name, value);
            return this;
        }

        public QueryDetails build() {
            return new QueryDetails(query, values);
        }

    }

}
