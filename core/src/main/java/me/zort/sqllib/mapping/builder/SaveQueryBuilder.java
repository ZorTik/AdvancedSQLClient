package me.zort.sqllib.mapping.builder;

import com.google.gson.internal.Primitives;
import me.zort.sqllib.SQLDatabaseConnectionImpl;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.internal.query.UpsertQuery;
import me.zort.sqllib.mapping.QueryAnnotation;
import me.zort.sqllib.mapping.annotation.Save;
import me.zort.sqllib.mapping.annotation.Table;
import me.zort.sqllib.util.ParameterPair;

import java.lang.reflect.Method;

public class SaveQueryBuilder implements QueryAnnotation.QueryBuilder<Save> {
    @Override
    public QueryNode<?> build(SQLConnection connection, Save queryAnnotation, Method method, ParameterPair[] parameters) {
        if (!(connection instanceof SQLDatabaseConnectionImpl))
            throw new IllegalArgumentException("The connection must be an instance of SQLDatabaseConnectionImpl");

        String table = Table.Util.getFromContext(method, parameters);

        Object saveableObject = getSaveableObject(parameters);

        UpsertQuery query = ((SQLDatabaseConnectionImpl) connection).save(saveableObject);
        query.table(table);
        query.setAssignedSaveObject(saveableObject);

        return query;
    }

    private static Object getSaveableObject(ParameterPair[] parameters) {
        for (ParameterPair parameter : parameters) {
            Class<?> aClass = parameter.getValue().getClass();
            if (!Primitives.isWrapperType(Primitives.wrap(aClass)))
                return parameter.getValue();
        }

        throw new IllegalArgumentException("No object to save found in paramaters!");
    }
}
