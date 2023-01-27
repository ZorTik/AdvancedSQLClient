package me.zort.sqllib.mapping.builder;

import me.zort.sqllib.internal.query.Conditional;
import me.zort.sqllib.internal.query.Limitable;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.internal.query.SelectQuery;
import me.zort.sqllib.mapping.QueryAnnotation;
import me.zort.sqllib.mapping.annotation.Limit;
import me.zort.sqllib.mapping.annotation.Select;
import me.zort.sqllib.mapping.annotation.Table;
import me.zort.sqllib.mapping.annotation.Where;
import me.zort.sqllib.util.ParameterPair;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class SelectQueryBuilder implements QueryAnnotation.QueryBuilder<Select> {
    @Override
    public QueryNode<?> build(Select queryAnnotation, Method method, ParameterPair[] parameters) {
        QueryAnnotation.Validator.requireTableDefinition(method);

        String table = Table.Util.getFromContext(method);

        QueryNode<?> node = new SelectQuery(null, table, queryAnnotation.value().equals("*")
        ? new ArrayList<>() : Arrays.asList(queryAnnotation.value().replaceAll(" ", "").split(",")));
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
