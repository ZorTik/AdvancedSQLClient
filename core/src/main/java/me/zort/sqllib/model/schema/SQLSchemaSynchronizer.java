package me.zort.sqllib.model.schema;

import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.model.ColumnDefinition;
import me.zort.sqllib.api.model.SchemaSynchronizer;
import me.zort.sqllib.api.model.TableSchema;
import me.zort.sqllib.model.column.InnoColumnQueryBuilder;
import me.zort.sqllib.model.column.SQLColumnQueryBuilder;

public class SQLSchemaSynchronizer implements SchemaSynchronizer<SQLDatabaseConnection> {

    @Setter
    private SQLColumnQueryBuilder columnQueryBuilder = new InnoColumnQueryBuilder();

    @Override
    public QueryResult synchronize(SQLDatabaseConnection source, TableSchema from, TableSchema to) {
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < Math.max(from.size(), to.size()); i++) {
            ColumnDefinition fromDefinition = from.size() > i ? from.getDefinitionDetails(i) : null;
            ColumnDefinition toDefinition = to.size() > i ? to.getDefinitionDetails(i) : null;

            if (fromDefinition == null && toDefinition != null) {
                query.append(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.DROP, from.getTable(), fromDefinition, toDefinition));
            } else if (fromDefinition != null && toDefinition == null) {
                query.append(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.ADD, from.getTable(), fromDefinition, toDefinition));
            } else {
                assert fromDefinition != null;
                if (!fromDefinition.getName().equals(toDefinition.getName())) {
                    query.append(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.RENAME, from.getTable(), fromDefinition, toDefinition));
                } else if(!fromDefinition.getType().equals(toDefinition.getType())) {
                    query.append(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.MODIFY, from.getTable(), fromDefinition, toDefinition));
                }
            }
        }
        return query.length() == 0 ? QueryResult.noChangesResult : source.exec(query.toString());
    }
}
