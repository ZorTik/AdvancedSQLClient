package me.zort.sqllib.model.builder;

import me.zort.sqllib.api.model.ColumnDefinition;
import me.zort.sqllib.api.model.TableSchema;
import me.zort.sqllib.api.model.TableSchemaBuilder;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Builds table schema from relational SQL database using JDBC query.
 *
 * @author ZorTik
 */
public final class DatabaseSchemaBuilder implements TableSchemaBuilder {

  private final Function<String, PreparedStatement> statementFactory;
  private final String table;

  public DatabaseSchemaBuilder(Function<String, PreparedStatement> statementFactory, String table) {
    this.statementFactory = statementFactory;
    this.table = table;
  }

  @Override
  public TableSchema buildTableSchema() {
    try (PreparedStatement statement = statementFactory.apply("SELECT * FROM " + table + " LIMIT 0;");
         ResultSet rs = statement.executeQuery()) {
      ResultSetMetaData rsMeta = rs.getMetaData();
      DatabaseMetaData connectionMeta = statement.getConnection().getMetaData();
      ResultSet primaryKeysRS = connectionMeta.getPrimaryKeys(null, null, table);
      ResultSet defaultValuesRS = connectionMeta.getColumns(null, null, table, null);
      Set<String> primaryKeys = new HashSet<>();
      Map<String, String> defaultValues = new HashMap<>();
      while (primaryKeysRS.next()) {
        primaryKeys.add(primaryKeysRS.getString("COLUMN_NAME").toUpperCase());
      }
      while (defaultValuesRS.next()) {
        String defaultValue = defaultValuesRS.getString("COLUMN_DEF");
        if (defaultValue != null) {
          defaultValues.put(defaultValuesRS.getString("COLUMN_NAME").toUpperCase(), defaultValue);
        }
      }
      primaryKeysRS.close();
      defaultValuesRS.close();

      ColumnDefinition[] definitions = new ColumnDefinition[rsMeta.getColumnCount()];
      for (int i = 0; i < definitions.length; i++) {
        String name = rsMeta.getColumnName(i + 1);
        String type = prepareColumnType(rsMeta.getColumnTypeName(i + 1));
        if (rsMeta.getColumnClassName(i + 1).equals(String.class.getName()) && rsMeta.getColumnDisplaySize(i + 1) > 0) {
          type += "(" + rsMeta.getColumnDisplaySize(i + 1) + ")";
        }
        if (primaryKeys.contains(name.toUpperCase())) {
          type += " PRIMARY KEY";
        }
        if (defaultValues.containsKey(name.toUpperCase())) {
          type += " DEFAULT " + defaultValues.get(name.toUpperCase());
        }
        definitions[i] = new ColumnDefinition(name, type);
      }
      return new TableSchema(table, definitions);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static String prepareColumnType(String type) {
    if (type.equalsIgnoreCase("INT")) type = "INTEGER";
    return type;
  }
}
