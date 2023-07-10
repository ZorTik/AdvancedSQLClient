package me.zort.sqllib.model.column;

import me.zort.sqllib.api.model.ColumnDefinition;

import java.util.ArrayList;
import java.util.List;

public class InnoColumnQueryBuilder implements SQLColumnQueryBuilder {
  @Override
  public List<String> buildActionQuery(SQLColumnQueryBuilder.ColumnAction action, String table, ColumnDefinition from, ColumnDefinition to) {
    List<String> queries = new ArrayList<>();
    if (action == SQLColumnQueryBuilder.ColumnAction.ADD) {
      queries.add("ALTER TABLE " + table + " ADD COLUMN " + from + ";");
    } else if (action == SQLColumnQueryBuilder.ColumnAction.DROP) {
      queries.add("ALTER TABLE " + table + " DROP COLUMN " + to.getName() + ";");
    } else if (action == SQLColumnQueryBuilder.ColumnAction.RENAME) {
      queries.add("ALTER TABLE " + table + " RENAME COLUMN " + to.getName() + " TO " + from.getName() + ";");
    } else if (action == SQLColumnQueryBuilder.ColumnAction.MODIFY) {
      queries.add("ALTER TABLE " + table + " MODIFY COLUMN " + from.getName() + " " + from.getType() + ";");
    }
    if (queries.size() > 0) return queries;
    throw new RuntimeException("Unknown action: " + action);
  }
}
