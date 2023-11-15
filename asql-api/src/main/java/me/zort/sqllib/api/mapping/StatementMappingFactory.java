package me.zort.sqllib.api.mapping;

import me.zort.sqllib.api.SQLConnection;

import java.util.function.Supplier;

/**
 * The StatementMappingFactory is responsible for creating new {@link StatementMappingStrategy}
 * for defined interfaces.
 *
 * @author ZorTik
 */
public interface StatementMappingFactory {

  /**
   * Creates a new StatementMapping for the given interface class that
   * is responsible for handling that specific interface.
   *
   * @param interfaceClass The interface class
   * @param connectionFactory     The connection factory to use.
   * @param <T>            The interface class type.
   * @return The StatementMapping.
   */
  <T> StatementMappingStrategy<T> strategy(
          Class<T> interfaceClass,
          Supplier<SQLConnection> connectionFactory
  );

  StatementMappingResultAdapter resultAdapter();

}
