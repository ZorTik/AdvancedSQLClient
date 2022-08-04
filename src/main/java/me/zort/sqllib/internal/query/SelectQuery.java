package me.zort.sqllib.internal.query;

import lombok.Getter;
import me.zort.sqllib.api.Executive;
import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.internal.query.part.WhereStatement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SelectQuery extends QueryPartQuery<QueryPart<?>> implements Executive {

    private final List<String> cols;
    private String table;

    @Getter
    private final SQLDatabaseConnection connection;

    public SelectQuery(SQLDatabaseConnection connection, String... cols) {
        this(connection, null, Arrays.asList(cols));
    }

    public SelectQuery(SQLDatabaseConnection connection, @Nullable String table, List<String> cols) {
        super(null, new ArrayList<>(), QueryPriority.GENERAL);
        this.table = table;
        this.cols = cols;
        this.connection = connection;
    }

    public SelectQuery from(String table) {
        this.table = table;
        return this;
    }

    public WhereStatement<SelectQuery> where() {
        return where(this);
    }

    @Override
    public String buildQuery() {
        Objects.requireNonNull(table, "Table cannot be null!");
        String cols = this.cols.isEmpty() ? "*" : String.join(", ", this.cols);
        return String.format("SELECT %s FROM %s%s;", cols, table, buildInnerQuery());
    }

}
