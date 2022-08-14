package me.zort.sqllib;

import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.exception.IllegalStatementOperationException;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.impl.QueryResultImpl;
import me.zort.sqllib.internal.query.InsertQuery;
import me.zort.sqllib.internal.query.UpdateQuery;
import me.zort.sqllib.internal.query.UpsertQuery;
import me.zort.sqllib.util.PrimaryKey;
import org.jetbrains.annotations.Nullable;

public class SQLiteDatabaseConnectionImpl extends SQLDatabaseConnectionImpl {

    public SQLiteDatabaseConnectionImpl(SQLConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public SQLiteDatabaseConnectionImpl(SQLConnectionFactory connectionFactory, SQLDatabaseOptions options) {
        super(connectionFactory, options);
    }

    public QueryResult upsert(String table, PrimaryKey primaryKey, InsertQuery insert, UpdateQuery update) {
        QueryRowsResult<Row> slct = select("*")
                .from(table)
                .where().isEqual(primaryKey.getColumn(), primaryKey.getValue())
                .also().limit(1)
                .obtainAll();
        if(!slct.isSuccessful()) {
            // Not successful, we'll skip other queries.
            return new QueryResultImpl(false);
        }
        if(slct.isEmpty()) {
            // No results, we'll insert.
            return exec(insert);
        } else {
            return exec(update);
        }
    }

    @Override
    public UpsertQuery upsert(@Nullable String table) {
        throw new IllegalStatementOperationException("Default upsert is not supported by SQLite!");
    }

}
