package me.zort.sqllib.model.column;

import me.zort.sqllib.api.model.ColumnDefinition;

public class InnoColumnQueryBuilder implements SQLColumnQueryBuilder {
    @Override
    public String buildActionQuery(SQLColumnQueryBuilder.ColumnAction action, String table, ColumnDefinition from, ColumnDefinition to) {
        if (action == SQLColumnQueryBuilder.ColumnAction.ADD) {
            return "ALTER TABLE " + table + " ADD COLUMN " + from + ";";
        } else if (action == SQLColumnQueryBuilder.ColumnAction.DROP) {
            return "ALTER TABLE " + table + " DROP COLUMN " + to.getName() + ";";
        } else if (action == SQLColumnQueryBuilder.ColumnAction.RENAME) {
            return "ALTER TABLE " + table + " RENAME COLUMN " + to.getName() + " TO " + from.getName() + ";";
        } else if (action == SQLColumnQueryBuilder.ColumnAction.MODIFY) {
            return "ALTER TABLE " + table + " MODIFY COLUMN " + from.getName() + " " + from.getType() + ";";
        }
        throw new RuntimeException("Unknown action: " + action);
    }
}
