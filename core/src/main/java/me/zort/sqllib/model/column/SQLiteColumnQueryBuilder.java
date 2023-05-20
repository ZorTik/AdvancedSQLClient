package me.zort.sqllib.model.column;

import lombok.AllArgsConstructor;
import me.zort.sqllib.SQLiteDatabaseConnection;
import me.zort.sqllib.api.model.ColumnDefinition;
import me.zort.sqllib.api.model.TableSchema;

@AllArgsConstructor
public class SQLiteColumnQueryBuilder extends InnoColumnQueryBuilder {
    private final SQLiteDatabaseConnection connection;
    @Override
    public String buildActionQuery(ColumnAction action, String table, ColumnDefinition from, ColumnDefinition to) {
        if (action.equals(ColumnAction.MODIFY)) {
            TableSchema schema = connection.getSchemaBuilder(table).buildTableSchema();
            String queries = "ALTER TABLE " + table + " RENAME TO " + table + "_old;";
            queries += "CREATE TABLE " + table + "(" + String.join(", ", schema.getDefinitions()) + ");";
            queries += "INSERT INTO " + table + "(" + String.join(", ", schema.getDefinitionNames()) + ") SELECT " + String.join(", ", schema.getDefinitionNames()) + " FROM " + table + "_old;";
            queries += "DROP TABLE " + table + "_old;";
            return queries;
        }
        return super.buildActionQuery(action, table, from, to);
    }
}
