package me.zort.sqllib.mapping.builder;

import me.zort.sqllib.internal.query.Conditional;
import me.zort.sqllib.internal.query.DeleteQuery;
import me.zort.sqllib.internal.query.Limitable;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.mapping.QueryAnnotation;
import me.zort.sqllib.mapping.annotation.Delete;
import me.zort.sqllib.mapping.annotation.Limit;
import me.zort.sqllib.mapping.annotation.Table;
import me.zort.sqllib.mapping.annotation.Where;
import me.zort.sqllib.util.ParameterPair;

import java.lang.reflect.Method;

public class DeleteQueryBuilder implements QueryAnnotation.QueryBuilder<Delete> {
    @Override
    public QueryNode<?> build(Delete queryAnnotation, Method method, ParameterPair[] parameters) {
        QueryAnnotation.Validator.requireTableDefinition(method);
        QueryAnnotation.Validator.requireWhereDefinition(method);

        String table = Table.Util.getFromContext(method);

        QueryNode<?> node = new DeleteQuery(null, table);
        if (method.isAnnotationPresent(Where.class)) {
            node = Where.Builder.build((Conditional<?>) node, method.getAnnotation(Where.class));
            node = node.getAncestor();
        }
        if (method.isAnnotationPresent(Limit.class)) {
            node = (QueryNode<?>) Limit.Builder.build((Limitable<?>) node, method.getAnnotation(Limit.class));
            node = node.getAncestor();
        }
        return node;
    }
}
