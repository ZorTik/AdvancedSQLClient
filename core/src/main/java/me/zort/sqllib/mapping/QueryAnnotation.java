package me.zort.sqllib.mapping;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.mapping.annotation.Delete;
import me.zort.sqllib.mapping.annotation.Select;
import me.zort.sqllib.mapping.annotation.Table;
import me.zort.sqllib.mapping.annotation.Where;
import me.zort.sqllib.mapping.builder.DeleteQueryBuilder;
import me.zort.sqllib.mapping.builder.SelectQueryBuilder;
import me.zort.sqllib.mapping.exception.SQLMappingException;
import me.zort.sqllib.util.ParameterPair;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a query annotation wrapper used in default statement
 * mapping proxy. Query that is wrapped appears on abstract methods
 * of the proxy instance created using:
 * {@link me.zort.sqllib.SQLDatabaseConnection#createMapping(Class)}
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class QueryAnnotation {

    private final boolean producesResult;
    @SuppressWarnings("rawtypes")
    private final QueryBuilder queryBuilder;

    private static final Map<Class<? extends Annotation>, QueryAnnotation> QUERY_ANNOT = new ConcurrentHashMap<>();

    static {
        QUERY_ANNOT.put(Select.class, new QueryAnnotation(true, new SelectQueryBuilder()));
        QUERY_ANNOT.put(Delete.class, new QueryAnnotation(false, new DeleteQueryBuilder()));
        // TODO: Populate
    }

    @Nullable
    public static QueryAnnotation wrap(Annotation annotation) {
        return isQueryAnnotation(annotation) ? QUERY_ANNOT.get(annotation.annotationType()) : null;
    }

    public static boolean isQueryAnnotation(Annotation annotation) {
        return isQueryAnnotation(annotation.annotationType());
    }

    public static boolean isQueryAnnotation(Class<? extends Annotation> annotation) {
        return QUERY_ANNOT.containsKey(annotation);
    }

    /**
     * Interface that is used for building query in mapping proxy based on
     * method annotations and parameters, to be executed by {@link DefaultStatementMapping}.
     */
    public interface QueryBuilder<T extends Annotation> {
        QueryNode<?> build(T queryAnnotation, Method method, ParameterPair[] parameters);
    }

    public static class Validator {
        public static void requireTableDefinition(Method method) {
            if (Table.Util.getFromContext(method) == null)
                throw new SQLMappingException("Method " + method.getName() + " requires @Table annotation", method, null);
        }
        public static void requireWhereDefinition(Method method) {
            if (!method.isAnnotationPresent(Where.class))
                throw new SQLMappingException("Method " + method.getName() + " requires @Where annotation", method, null);
        }
    }

}
