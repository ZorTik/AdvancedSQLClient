package me.zort.sqllib.internal.query;

import lombok.AccessLevel;
import lombok.Getter;
import me.zort.sqllib.api.Executive;
import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.internal.exception.IllegalStatementOperationException;
import me.zort.sqllib.util.Encoding;
import me.zort.sqllib.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.StringJoiner;

public class InsertQuery extends QueryPart<QueryPart<?>> implements Executive, Conditional<InsertQuery> {

    @Getter(AccessLevel.PROTECTED)
    private String table;
    private String[] defs;
    private String[] values;

    @Getter
    private final SQLDatabaseConnection connection;

    public InsertQuery(SQLDatabaseConnection connection) {
        this(connection, null);
    }

    public InsertQuery(SQLDatabaseConnection connection, @Nullable String table) {
        super(null, new ArrayList<>(), QueryPriority.GENERAL);
        this.table = table;
        this.connection = connection;
        this.defs = new String[0];
    }

    public InsertQuery into(String table, String... defs) {
        this.table = table;
        this.defs = defs;
        return this;
    }

    public InsertQuery values(Object... values) {
        String[] vals = new String[values.length];
        for(int i = 0; i < values.length; i++) {
            Object obj = values[i];
            if(obj instanceof String) {
                obj = Encoding.handleTo((String) obj);
            }
            vals[i] = Util.buildQuoted(obj);
        }
        this.values = vals;
        return this;
    }

    @Override
    public String buildQuery() {
        Objects.requireNonNull(table, "Table cannot be null!");
        if(defs.length != values.length) {
            throw new IllegalStatementOperationException("Definition count must be same as values count!");
        }
        return String.format("INSERT INTO %s %s VALUES %s", table, joinArr(defs), joinArr(values));
    }

    private String joinArr(String[] arr) {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for(String str : arr) {
            joiner.add(str);
        }
        return joiner.toString();
    }

}
