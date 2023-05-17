package me.zort.sqllib.model;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.model.ColumnDefinition;
import me.zort.sqllib.api.model.SchemaSynchronizer;
import me.zort.sqllib.api.model.TableSchema;

public class SQLSchemaSynchronizer implements SchemaSynchronizer<SQLDatabaseConnection> {
    @Override
    public QueryResult synchronize(SQLDatabaseConnection source, TableSchema from, TableSchema to) {
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < Math.max(from.size(), to.size()); i++) {
            ColumnDefinition fromDefinition = from.size() > i ? from.getDefinitionDetails(i) : null;
            ColumnDefinition toDefinition = to.size() > i ? to.getDefinitionDetails(i) : null;

            if (fromDefinition == null && toDefinition != null) {
                query.append("ALTER TABLE ").append(from.getTable()).append(" DROP COLUMN ").append(toDefinition.getName()).append(";");
            } else if (fromDefinition != null && toDefinition == null) {
                query.append("ALTER TABLE ").append(from.getTable()).append(" ADD ").append(fromDefinition).append(";");
            } else {
                assert fromDefinition != null;
                if (!fromDefinition.getName().equals(toDefinition.getName())) {
                    query.append("ALTER TABLE ").append(from.getTable()).append(" RENAME COLUMN ").append(toDefinition.getName()).append(" TO ").append(fromDefinition.getName()).append(";");
                } else if(!fromDefinition.getType().equals(toDefinition.getType())) {
                    query.append("ALTER TABLE ").append(from.getTable()).append(" ALTER COLUMN ").append(fromDefinition.getName()).append(" ").append(fromDefinition.getType()).append(";");
                }
            }
        }
        return query.length() == 0 ? QueryResult.noChangesResult : source.exec(query.toString());
    }
}
