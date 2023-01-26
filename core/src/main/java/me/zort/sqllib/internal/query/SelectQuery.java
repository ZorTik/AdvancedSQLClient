package me.zort.sqllib.internal.query;

import lombok.Getter;
import me.zort.sqllib.api.Executive;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.internal.query.part.LimitStatement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SelectQuery extends QueryNodeR<QueryNode<?>> implements Executive, Conditional<SelectQuery> {

    private final List<String> cols;
    private String table;

    @Getter
    private SQLDatabaseConnection connection;

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

    public SelectQuery limit(int limit) {
        then(new LimitStatement<>(this, new ArrayList<>(), limit));
        return this;
    }

    @Override
    public QueryDetails buildQueryDetails() {
        Objects.requireNonNull(table, "Table cannot be null!");

        QueryDetails details = new QueryDetails.Builder(String.format("SELECT %s FROM %s",
                this.cols.isEmpty() ? "*" : String.join(", ", this.cols),
                table))
                .build();

        return details.append(buildInnerQuery());
    }

    @Override
    public SelectQuery then(String part) {
        return (SelectQuery) super.then(part);
    }
}
