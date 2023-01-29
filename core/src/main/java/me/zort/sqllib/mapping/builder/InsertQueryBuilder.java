package me.zort.sqllib.mapping.builder;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.internal.query.InsertQuery;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.mapping.PlaceholderMapper;
import me.zort.sqllib.mapping.QueryAnnotation;
import me.zort.sqllib.mapping.annotation.Insert;
import me.zort.sqllib.mapping.annotation.Table;
import me.zort.sqllib.util.ParameterPair;

import java.lang.reflect.Method;

public class InsertQueryBuilder implements QueryAnnotation.QueryBuilder<Insert> {
    @Override
    public QueryNode<?> build(SQLConnection connection, Insert queryAnnotation, Method method, ParameterPair[] parameters) {
        if (!(connection instanceof SQLDatabaseConnection))
            throw new IllegalArgumentException("The connection must be a SQLDatabaseConnection");

        String table = Table.Util.getFromContext(method, parameters);
        InsertQuery query = ((SQLDatabaseConnection) connection).insert();
        query.into(table, queryAnnotation.cols());

        PlaceholderMapper mapper = new PlaceholderMapper(parameters);

        String[] vals = queryAnnotation.vals();
        for (int i = 0; i < vals.length; i++) {
            vals[i] = mapper.assignValues(vals[i]);
        }

        query.values((Object[]) vals);
        return query;
    }
}
