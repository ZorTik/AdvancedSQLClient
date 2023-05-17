package me.zort.sqllib.model;

import me.zort.sqllib.api.model.TableSchema;
import me.zort.sqllib.api.model.TableSchemaBuilder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
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
        try(PreparedStatement statement = statementFactory.apply("SELECT * FROM " + table + " LIMIT 0;");
            ResultSet rs = statement.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            ResultSet primaryKeysRS = statement.getConnection().getMetaData().getPrimaryKeys(null, null, table);
            Set<String> primaryKeys = new HashSet<>();
            while (primaryKeysRS.next()) {
                primaryKeys.add(primaryKeysRS.getString("COLUMN_NAME").toUpperCase());
            }
            primaryKeysRS.close();

            String[] definitions = new String[meta.getColumnCount()];
            for (int i = 0; i < definitions.length; i++) {
                definitions[i] = meta.getColumnName(i + 1) + " " + meta.getColumnTypeName(i + 1);
                definitions[i] = definitions[i].replace(" INT", " INTEGER");

                if (meta.getColumnClassName(i + 1).equals(String.class.getName()) && meta.getColumnDisplaySize(i + 1) > 0) {
                    definitions[i] += "(" + meta.getColumnDisplaySize(i + 1) + ")";
                }
                if (primaryKeys.contains(meta.getColumnName(i + 1).toUpperCase())) {
                    definitions[i] += " PRIMARY KEY";
                }
            }
            return new TableSchema(table, definitions);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
