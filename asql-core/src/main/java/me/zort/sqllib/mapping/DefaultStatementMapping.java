package me.zort.sqllib.mapping;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.mapping.StatementMappingOptions;
import me.zort.sqllib.api.mapping.StatementMappingStrategy;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.internal.query.ResultSetAware;
import me.zort.sqllib.mapping.annotation.Append;
import me.zort.sqllib.mapping.exception.SQLMappingException;
import me.zort.sqllib.util.ParameterPair;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Supplier;

/**
 * This mapping strategy uses annotations from me.zort.sqllib.mapping.annotation
 * to build queries. It is the default mapping strategy.
 *
 * @param <T> The type of the proxy instance.
 * @author ZorTik
 */
public class DefaultStatementMapping<T> implements StatementMappingStrategy<T> {

  private final Supplier<SQLConnection> connectionFactory;

  public DefaultStatementMapping(Supplier<SQLConnection> connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  @SuppressWarnings("unchecked")
  @Override
  public QueryResult executeQuery(StatementMappingOptions options, Method method, Object[] args, @Nullable Class<?> mapTo) {
    ParameterPair[] parameters = new ParameterPair[method.getParameters().length];
    int i = 0;
    for (Parameter parameter : method.getParameters()) {
      parameters[i] = new ParameterPair(parameter, args[i]);
      i++;
    }

    SQLConnection connection = connectionFactory.get();

    try {
      Annotation queryAnnotation = filterQueryAnnotation(method, args);
      QueryAnnotation wrappedAnnotation = QueryAnnotation.wrap(queryAnnotation);

      if (wrappedAnnotation == null) {
        throw new SQLMappingException("No query builder found for method " + method.getName() + "! Is query annotation present?", method, args);
      } else if (!(connection instanceof SQLDatabaseConnection)) {
        throw new SQLMappingException("Connection is not a SQLDatabaseConnection!", method, args);
      }

      QueryNode<?> node = wrappedAnnotation.getQueryBuilder().build(
              new QueryAnnotation.DefaultMappingDetails(connection, options), queryAnnotation,
              method, parameters);
      if (method.isAnnotationPresent(Append.class)) {
        Append append = method.getAnnotation(Append.class);
        node.then(new PlaceholderMapper(parameters).assignValues(append.value()));
      }

      if (mapTo != null && wrappedAnnotation.isProducesResult() && QueryRowsResult.class.isAssignableFrom(mapTo)) {
        return ((SQLDatabaseConnection) connection).query(node);
      }

      if (wrappedAnnotation.isProducesResult() && node instanceof ResultSetAware) {
        return mapTo != null
                ? ((SQLDatabaseConnection) connection).query(node, mapTo)
                : ((SQLDatabaseConnection) connection).query(node);
      } else {
        return ((SQLDatabaseConnection) connection).exec(node);
      }
    } finally {
      if (connection instanceof SQLDatabaseConnection) {
        ((SQLDatabaseConnection) connection).close();
      } else {
        connection.disconnect();
      }
    }
  }

  private static Annotation filterQueryAnnotation(Method method, Object[] args) {
    Annotation queryAnnotation = null;
    for (Annotation annotation : method.getAnnotations()) {
      boolean isQueryAnnot = QueryAnnotation.isQueryAnnotation(annotation);
      if (isQueryAnnot && queryAnnotation == null) {
        queryAnnotation = annotation;
      } else if (isQueryAnnot) {
        String errMessage = String.format("Multiple query annotations (Select/Insert/...) found on method %s!", method.getName());
        throw new SQLMappingException(errMessage, method, args);
      }
    }
    return queryAnnotation;
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
