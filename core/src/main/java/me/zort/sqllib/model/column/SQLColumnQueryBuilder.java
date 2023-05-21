package me.zort.sqllib.model.column;

import me.zort.sqllib.api.model.ColumnDefinition;

import java.util.List;

public interface SQLColumnQueryBuilder {

    List<String> buildActionQuery(ColumnAction action, String table, ColumnDefinition from, ColumnDefinition to);

    enum ColumnAction {
        ADD, DROP, MODIFY, RENAME
    }

}
