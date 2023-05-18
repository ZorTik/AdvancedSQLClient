package me.zort.sqllib.model.column;

import me.zort.sqllib.api.model.ColumnDefinition;

public interface SQLColumnQueryBuilder {

    String buildActionQuery(ColumnAction action, String table, ColumnDefinition from, ColumnDefinition to);

    enum ColumnAction {
        ADD, DROP, MODIFY, RENAME
    }

}
