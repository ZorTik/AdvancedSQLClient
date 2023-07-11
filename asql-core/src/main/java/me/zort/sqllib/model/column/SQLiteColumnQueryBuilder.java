package me.zort.sqllib.model.column;

import lombok.AllArgsConstructor;
import me.zort.sqllib.SQLiteDatabaseConnection;
import me.zort.sqllib.api.model.ColumnDefinition;
import me.zort.sqllib.api.model.TableSchema;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class SQLiteColumnQueryBuilder extends InnoColumnQueryBuilder {
  private final SQLiteDatabaseConnection connection;

  @Override
  public List<String> buildActionQuery(ColumnAction action, String table, ColumnDefinition from, ColumnDefinition to) {
    if (action.equals(ColumnAction.MODIFY) || action.equals(ColumnAction.RENAME)) {
      TableSchema schema = connection.getSchemaBuilder(table).buildTableSchema();
      String[] newDefinitions = new String[schema.size()];
      for (int i = 0; i < schema.size(); i++) {
        if (schema.getDefinitionName(i).equals(to.getName())) {
          newDefinitions[i] = from.getName() + " " + from.getType();
        } else {
          newDefinitions[i] = schema.getDefinition(i);
        }
      }
      List<String> queries = new ArrayList<>();
      queries.add("ALTER TABLE " + table + " RENAME TO " + table + "_old;");
      queries.add("CREATE TABLE " + table + "(" + String.join(", ", newDefinitions) + ");");
      queries.add("INSERT INTO " + table + "(" + String.join(", ", schema.getDefinitionNames()) + ") SELECT " + String.join(", ", schema.getDefinitionNames()) + " FROM " + table + "_old;");
      queries.add("DROP TABLE " + table + "_old;");
      return queries;
    }
    return super.buildActionQuery(action, table, from, to);
  }
}
