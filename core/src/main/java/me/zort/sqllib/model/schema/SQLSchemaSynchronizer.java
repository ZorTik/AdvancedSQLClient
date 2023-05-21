package me.zort.sqllib.model.schema;

import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.model.ColumnDefinition;
import me.zort.sqllib.api.model.SchemaSynchronizer;
import me.zort.sqllib.api.model.TableSchema;
import me.zort.sqllib.internal.impl.QueryResultImpl;
import me.zort.sqllib.model.column.InnoColumnQueryBuilder;
import me.zort.sqllib.model.column.SQLColumnQueryBuilder;
import me.zort.sqllib.model.column.SQLColumnTypeAdjuster;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class SQLSchemaSynchronizer implements SchemaSynchronizer<SQLDatabaseConnection> {

    private SQLColumnQueryBuilder columnQueryBuilder = new InnoColumnQueryBuilder();
    private SQLColumnTypeAdjuster columnTypeAdjuster = type -> type;
    private boolean separateQueries = false;

    @Override
    public QueryResult synchronize(SQLDatabaseConnection source, TableSchema from, TableSchema to) {
        List<String> columnQueries = new ArrayList<>();
        for (int i = 0; i < Math.max(from.size(), to.size()); i++) {
            ColumnDefinition fromDefinition = from.size() > i ? from.getDefinitionDetails(i) : null;
            ColumnDefinition toDefinition = to.size() > i ? to.getDefinitionDetails(i) : null;

            if (fromDefinition == null && toDefinition != null) {
                columnQueries.addAll(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.DROP, from.getTable(), fromDefinition, toDefinition));
            } else if (fromDefinition != null && toDefinition == null) {
                columnQueries.addAll(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.ADD, from.getTable(), fromDefinition, toDefinition));
            } else {
                assert fromDefinition != null;
                if (!fromDefinition.getName().equals(toDefinition.getName())) {
                    columnQueries.addAll(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.RENAME, from.getTable(), fromDefinition, toDefinition));
                } else if(!columnTypeAdjuster.adjust(fromDefinition.getType()).equals(columnTypeAdjuster.adjust(toDefinition.getType()))) {
                    System.out.println("Modifying column " + fromDefinition.getName() + " in table " + from.getTable() + " from " + columnTypeAdjuster.adjust(toDefinition.getType()) + " to " + columnTypeAdjuster.adjust(fromDefinition.getType()));
                    columnQueries.addAll(columnQueryBuilder.buildActionQuery(SQLColumnQueryBuilder.ColumnAction.MODIFY, from.getTable(), fromDefinition, toDefinition));
                }
            }
        }
        if (columnQueries.size() == 0) return QueryResult.noChangesResult;
        List<QueryResult> results = new ArrayList<>();
        if (separateQueries) {
            for (String query : columnQueries) {
                results.add(source.exec(query));
            }
        } else {
            results.add(source.exec(String.join("", columnQueries)));
        }
        return results.stream().allMatch(QueryResult::isSuccessful) ? QueryResult.successful() : new QueryResultImpl(false);
    }
}
