package me.zort.sqllib.model.column;

import me.zort.sqllib.api.model.ColumnDefinition;

import java.util.List;

/**
 * This interface is used to build queries for column actions that are
 * used while synchronizing models.
 *
 * @author ZorTik
 */
public interface SQLColumnQueryBuilder {

  /**
   * This method should return queries that result in modifying the 'to' column
   * to the 'from'. In simple terms, 'to' should be changed to 'from' definition
   * using the returned queries.
   *
   * @param action The column action
   * @param table  Table name
   * @param from   The new column definition
   * @param to     The old column definition
   * @return List of SQL queries
   */
  List<String> buildActionQuery(ColumnAction action, String table, ColumnDefinition from, ColumnDefinition to);

  enum ColumnAction {
    ADD, DROP, MODIFY, RENAME
  }

}
