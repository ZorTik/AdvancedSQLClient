package me.zort.sqllib.model;

import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.model.ColumnDefinition;
import me.zort.sqllib.api.model.SchemaSynchronizer;
import me.zort.sqllib.api.model.TableSchema;
import me.zort.sqllib.internal.impl.QueryResultImpl;
import me.zort.sqllib.model.builder.InnoColumnQueryBuilder;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class SQLSchemaSynchronizer implements SchemaSynchronizer<SQLDatabaseConnection> {

  private SQLColumnQueryBuilder columnQueryBuilder = new InnoColumnQueryBuilder();
  private SQLColumnTypeAdjuster columnTypeAdjuster = type -> type;
  // Should we execute the queries ony-by-one?
  private boolean separateQueries = false;

  @Override
  public QueryResult synchronize(SQLDatabaseConnection source, TableSchema from, TableSchema to) {
    List<String> columnQueries = new ArrayList<>();
    // Order definitions to make sure we don't synchronize different columns
    ColumnDefinition[][] definitions = orderDefinitions(from, to);

    for (int i = 0; i < Math.max(definitions[0].length, definitions[1].length); i++) {
      // Try to build statement that will change the column using the provided
      // definitions pair.
      final ColumnDefinition fromDefinition = definitions[0][i];
      final ColumnDefinition toDefinition = definitions[1][i];

      if (fromDefinition == null && toDefinition != null) {
        columnQueries.addAll(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.DROP, from.getTable(), fromDefinition, toDefinition));
      } else if (fromDefinition != null && toDefinition == null) {
        columnQueries.addAll(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.ADD, from.getTable(), fromDefinition, toDefinition));
      } else {
        assert fromDefinition != null;
        if (!fromDefinition.getName().equals(toDefinition.getName())) {
          columnQueries.addAll(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.RENAME, from.getTable(), fromDefinition, toDefinition));
        } else if (!columnTypeAdjuster.adjust(fromDefinition.getType()).equals(columnTypeAdjuster.adjust(toDefinition.getType()))) {
          System.out.println("Modifying column " + fromDefinition.getName() + " in table " + from.getTable() + " from " + columnTypeAdjuster.adjust(toDefinition.getType()) + " to " + columnTypeAdjuster.adjust(fromDefinition.getType()));
          columnQueries.addAll(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.MODIFY, from.getTable(), fromDefinition, toDefinition));
        }
      }
    }
    if (columnQueries.size() == 0) return QueryResult.noChangesResult;
    List<QueryResult> results = new ArrayList<>();
    if (separateQueries) {
      // Execute modification queries one-by-one if requested.
      for (String query : columnQueries) {
        results.add(source.exec(query));
      }
    } else {
      // Execute all queries at once.
      results.add(source.exec(String.join("", columnQueries)));
    }
    return results.stream().allMatch(QueryResult::isSuccessful) ? QueryResult.successful() : new QueryResultImpl(false);
  }

  private static ColumnDefinition[][] orderDefinitions(TableSchema one, TableSchema two) {
    int maxSize = Math.max(one.size(), two.size());
    ColumnDefinition[][] definitions = new ColumnDefinition[2][maxSize];
    for (int i = 0; i < maxSize; i++) {
      if (one.size() > i && two.size() > i && two.getDefinitionDetails(one.getDefinitionName(i)) != null) {
        definitions[0][i] = one.getDefinitionDetails(i);
        definitions[1][i] = two.getDefinitionDetails(one.getDefinitionName(i));
      } else if (one.size() > i && two.size() > i) {
        definitions[0][i] = one.getDefinitionDetails(i);
        definitions[1][i] = two.getDefinitionDetails(i);
      } else if (one.size() > i) {
        definitions[0][i] = one.getDefinitionDetails(i);
        definitions[1][i] = null;
      } else if (two.size() > i) {
        definitions[1][i] = two.getDefinitionDetails(i);
        definitions[0][i] = null;
      }
    }
    return definitions;
  }
}
