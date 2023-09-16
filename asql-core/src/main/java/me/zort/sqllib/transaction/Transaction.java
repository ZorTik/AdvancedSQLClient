package me.zort.sqllib.transaction;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class Transaction {

  private final SQLDatabaseConnection databaseConnection;
  private final Connection connection;
  private boolean committed = false;

  public Transaction(SQLDatabaseConnection connection) {
    this.databaseConnection = connection;
    this.connection = connection.getConnection();
    Objects.requireNonNull(this.connection, "Connection is not established!");
  }

  public void verify() {
    if (databaseConnection.getTransaction() != this)
      throw new IllegalStateException("Transaction is not assigned!");
  }

  public TransactionFlow.Builder flow() {
    verify();
    return new TransactionFlow.Builder(this);
  }

  public TransactionFlow flow(final TransactionFlow.Step[] steps, final TransactionFlow.Options options) {
    verify();
    return new TransactionFlow(this, steps, options);
  }

  public SQLDatabaseConnection getDatabaseConnection() {
    return databaseConnection;
  }

  public void commit() throws SQLException {
    verify();
    if (!committed) {
      connection.commit();
      committed = true;
    }
  }

  public void rollback() throws SQLException {
    verify();
    if (committed) throw new IllegalStateException("Transaction already committed!");
    connection.rollback();
  }

  public QueryResult rollback(String savepoint) {
    verify();
    return databaseConnection.exec(() -> "ROLLBACK TO " + savepoint + ";");
  }

  public QueryResult savepoint(String savepoint) {
    verify();
    return databaseConnection.exec(() -> "SAVEPOINT " + savepoint + ";");
  }

  public boolean isActive() {
    return !committed;
  }

  void close() {
    databaseConnection.closeTransaction();
  }

}
