package me.zort.sqllib.internal.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zort.sqllib.util.Pair;
import me.zort.sqllib.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

@AllArgsConstructor
@Getter
public class QueryDetails {

    public static QueryDetails empty() {
        return new QueryDetails();
    }

    private String queryStr;
    private final Map<String, Object> values;

    public QueryDetails() {
        this("", Collections.emptyMap());
    }

    public QueryDetails append(QueryDetails other) {
        return append("", other);
    }

    public QueryDetails append(String prefix, QueryDetails other) {
        append(prefix + other.queryStr);
        other.values.forEach(values::putIfAbsent);
        return this;
    }

    public QueryDetails append(String s) {
        queryStr += s;
        return this;
    }

    public int length() {
        return queryStr.length();
    }

    protected PreparedStatement prepare(Connection connection) throws SQLException {
        Pair<String, Object[]> requirements = buildStatementDetails();

        PreparedStatement statement = connection.prepareStatement(requirements.getFirst());
        Object[] values = requirements.getSecond();
        for (int i = 0; i < values.length; i++) {
            statement.setObject(i + 1, values[i]);
        }
        return statement;
    }

    private Pair<String, Object[]> buildStatementDetails() {
        String query = queryStr;
        Map<Integer, Object> valuesUnsorted = new HashMap<>();

        int i = 0;
        for (String placeholder : this.values.keySet()) {
            Object value = this.values.get(placeholder);

            placeholder = "{" + placeholder + "}";

            if (Util.count(queryStr, placeholder) != 1)
                throw new RuntimeException("Placeholder " + placeholder + " is not unique in query " + queryStr);

            valuesUnsorted.put(query.indexOf(placeholder), value);
            query = query.replace(placeholder, "?");

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

    @RequiredArgsConstructor
    public static class Builder {

        private final String query;
        private final Map<String, Object> values = new HashMap<>();

        // Name without brackets
        public Builder placeholder(String name, Object value) {
            if(!query.contains("{" + name + "}"))
                throw new IllegalArgumentException("Placeholder {" + name + "} not found in query!");

            values.put(name, value);
            return this;
        }

        public QueryDetails build() {
            return new QueryDetails(query, values);
        }

    }

}
