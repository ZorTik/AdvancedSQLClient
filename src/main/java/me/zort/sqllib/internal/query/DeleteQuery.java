package me.zort.sqllib.internal.query;

import lombok.Getter;
import me.zort.sqllib.api.Executive;
import me.zort.sqllib.api.SQLDatabaseConnection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public class DeleteQuery extends QueryPart<QueryPart<?>> implements Executive, Conditional<DeleteQuery> {

    private String table;

    @Getter
    private final SQLDatabaseConnection connection;

    public DeleteQuery(SQLDatabaseConnection connection) {
        this(connection, null);
    }

    public DeleteQuery(SQLDatabaseConnection connection, @Nullable String table) {
        super(null, new ArrayList<>(), QueryPriority.GENERAL);
        this.table = table;
        this.connection = connection;
    }

    public DeleteQuery from(String table) {
        this.table = table;
        return this;
    }

    @Override
    public String buildQuery() {
        Objects.requireNonNull(table, "Table cannot be null!");
        return String.format("DELETE FROM %s%s;", table, buildInnerQuery());
    }

    @Override
    public DeleteQuery then(String part) {
        return (DeleteQuery) super.then(part);
    }

}
