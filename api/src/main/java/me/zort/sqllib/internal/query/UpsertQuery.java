package me.zort.sqllib.internal.query;

import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.internal.query.part.SetStatement;
import org.jetbrains.annotations.Nullable;

public class UpsertQuery extends InsertQuery {

    public UpsertQuery(SQLDatabaseConnection connection) {
        super(connection);
    }

    public UpsertQuery(SQLDatabaseConnection connection, @Nullable String table) {
        super(connection, table);
    }

    @Override
    public UpsertQuery into(String table, String... defs) {
        return (UpsertQuery) super.into(table, defs);
    }

    @Override
    public UpsertQuery values(Object... values) {
        return (UpsertQuery) super.values(values);
    }

    public SetStatement<InsertQuery> onDuplicateKey(String column, Object value) {
        SetStatement<InsertQuery> stmt = onDuplicateKey();
        stmt.and(column, value);
        return stmt;
    }

    public SetStatement<InsertQuery> onDuplicateKey() {
        SetStatement<InsertQuery> stmt = new SetStatement<InsertQuery>(this, 3) {
            @Override
            public String buildQuery() {
                return " ON DUPLICATE KEY UPDATE" + super.buildQuery().replaceAll("SET ", "");
            }
        };
        then(stmt);
        return stmt;
    }

    @Override
    public String buildQuery() {
        return super.buildQuery();
    }

    @Override
    public UpsertQuery then(String part) {
        return (UpsertQuery) super.then(part);
    }

}
