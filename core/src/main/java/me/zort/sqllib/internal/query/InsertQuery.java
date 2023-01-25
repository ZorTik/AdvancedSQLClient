package me.zort.sqllib.internal.query;

import lombok.Getter;
import me.zort.sqllib.api.Executive;
import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.internal.exception.IllegalStatementOperationException;
import me.zort.sqllib.util.Encoding;
import me.zort.sqllib.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public class InsertQuery extends QueryNode<QueryNode<?>> implements Executive, Conditional<InsertQuery> {

    @Getter
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
        this.values = new String[0];
    }

    public InsertQuery into(String table, String... defs) {
        this.table = table;
        this.defs = defs;
        return this;
    }

    // Used internally
    public InsertQuery appendVal(Object val) {
        String[] newValues = new String[values.length + 1];
        for(int i = 0; i < values.length; i++) {
            newValues[i] = values[i];
        }
        newValues[values.length] = handleVal(val);
        this.values = newValues;
        return this;
    }

    public InsertQuery values(Object... values) {
        String[] vals = new String[values.length];
        for(int i = 0; i < values.length; i++) {
            Object obj = values[i];
            vals[i] = handleVal(obj);
        }
        this.values = vals;
        return this;
    }

    private String handleVal(Object obj) {
        if(obj instanceof String) {
            obj = Encoding.handleTo((String) obj);
        }
        return Util.buildQuoted(obj);
    }

    @Override
    public QueryDetails buildQueryDetails() {
        Objects.requireNonNull(table, "Table cannot be null!");
        if(defs.length != values.length) {
            throw new IllegalStatementOperationException("Definition count must be same as values count!");
        }

        return new QueryDetails.Builder("INSERT INTO <table> (<defs>) VALUES (<vals>)") // TODO: Transform to array of placeholders
                .placeholder("table", table)
                .placeholder("defs", String.join(", ", defs))
                .placeholder("vals", String.join(", ", values))
                .build()
                .append(buildInnerQuery());
    }

    @Override
    public InsertQuery then(String part) {
        return (InsertQuery) super.then(part);
    }

}
