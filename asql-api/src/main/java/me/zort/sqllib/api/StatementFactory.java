package me.zort.sqllib.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The StatementFactory is responsible for creating new Statements for
 * the given connection.
 *
 * @param <T> The Statement type.
 * @author ZorTik
 */
public interface StatementFactory<T extends Statement> {

  /**
   * Prepares the statement for executing in {@link SQLConnection}.
   *
   * @param connection The connection to use.
   * @return The prepared statement.
   * @throws SQLException If an error occurs while preparing.
   */
  T prepare(Connection connection) throws SQLException;

}
