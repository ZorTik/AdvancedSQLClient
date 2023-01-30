package me.zort.sqllib.mapping;

import lombok.RequiredArgsConstructor;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.mapping.StatementMappingStrategy;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.internal.query.QueryNodeRequest;
import me.zort.sqllib.mapping.exception.SQLMappingException;
import me.zort.sqllib.util.ParameterPair;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * This mapping strategy uses annotations from me.zort.sqllib.mapping.annotation
 * to build queries. It is the default mapping strategy.
 *
 * @param <T> The type of the proxy instance.
 * @author ZorTik
 */
@RequiredArgsConstructor
public class DefaultStatementMapping<T> implements StatementMappingStrategy<T> {

    private final SQLConnection connection;

    @SuppressWarnings("unchecked")
    @Override
    public QueryResult executeQuery(Method method, Object[] args, @Nullable Class<?> mapTo) {
        ParameterPair[] parameters = new ParameterPair[method.getParameters().length];
        int i = 0;
        for (Parameter parameter : method.getParameters()) {
            parameters[i] = new ParameterPair(parameter, args[i]);
            i++;
        }

        Annotation queryAnnotation = null;
        QueryAnnotation wrappedAnnotation = null;

        for (Annotation annotation : method.getAnnotations()) {
            if (QueryAnnotation.isQueryAnnotation(annotation)) {
                queryAnnotation = annotation;
                wrappedAnnotation = QueryAnnotation.wrap(annotation);
            }
        }

        if (wrappedAnnotation == null) {
            throw new SQLMappingException("No query builder found for method " + method.getName() + "! Is query annotation present?", method, args);
        } else if (!(connection instanceof SQLDatabaseConnection)) {
            throw new SQLMappingException("Connection is not a SQLDatabaseConnection!", method, args);
        }

        QueryNode<?> node = wrappedAnnotation.getQueryBuilder().build(connection, queryAnnotation, method, parameters);

        if (mapTo != null && wrappedAnnotation.isProducesResult() && QueryRowsResult.class.isAssignableFrom(mapTo)) {
            return ((SQLDatabaseConnection) connection).query(node);
        }

        if (wrappedAnnotation.isProducesResult() && node instanceof QueryNodeRequest) {
            return mapTo != null
                    ? ((SQLDatabaseConnection) connection).query(node, mapTo)
                    : ((SQLDatabaseConnection) connection).query(node);
        } else {
            return ((SQLDatabaseConnection) connection).exec(node);
        }
    }

    @Override
    public boolean isMappingMethod(Method method) {
        for (Annotation annot : method.getAnnotations()) {
            if (QueryAnnotation.isQueryAnnotation(annot)) {
                return true;
            }
        }
        return false;
    }

}
