package me.zort.sqllib.mapping;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.mapping.StatementMappingOptions;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.mapping.annotation.*;
import me.zort.sqllib.mapping.builder.*;
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
 * {@link me.zort.sqllib.SQLDatabaseConnection#createGate(Class)}
 *
 * @author ZorTik
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
    QUERY_ANNOT.put(Save.class, new QueryAnnotation(false, new SaveQueryBuilder()));
    QUERY_ANNOT.put(Insert.class, new QueryAnnotation(false, new InsertQueryBuilder()));
    QUERY_ANNOT.put(Query.class, new QueryAnnotation(true, new NativeQueryBuilder()));
    QUERY_ANNOT.put(Exec.class, new QueryAnnotation(false, new NativeQueryBuilder()));
  }

  public static <T extends Annotation> void register(Class<T> annotation, boolean producesResult, QueryBuilder<T> builder) {
    QUERY_ANNOT.put(annotation, new QueryAnnotation(producesResult, builder));
  }

  @Nullable
  public static QueryAnnotation wrap(Annotation annotation) {
    if (annotation == null) return null;
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

    QueryNode<?> build(DefaultMappingDetails details, T queryAnnotation, Method method, ParameterPair[] parameters);
  }

  @AllArgsConstructor
  @Getter
  public static class DefaultMappingDetails {
    private final SQLConnection connection;
    private final StatementMappingOptions options;
  }

  public static class Validator {
    public static void requireWhereDefinition(Method method) {
      if (!method.isAnnotationPresent(Where.class))
        throw new SQLMappingException("Method " + method.getName() + " requires @Where annotation", method, null);
    }
  }

}
