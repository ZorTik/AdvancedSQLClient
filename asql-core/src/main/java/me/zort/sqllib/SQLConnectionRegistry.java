package me.zort.sqllib;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SQLConnectionRegistry {

  private SQLConnectionRegistry() {
  }

  private static final List<SQLDatabaseConnection> CONNECTIONS = new CopyOnWriteArrayList<>();

  /**
   * Marks connection to be registered in the registry when connected.
   * Note that only active connections should be present in the registry,
   * this should not necessarily register the connection.
   *
   * @param connection The connection to populate
   */
  public static void registerConnection(@NotNull SQLDatabaseConnection connection) {
    if (connection.getCodeHandlers().stream().anyMatch(o -> o instanceof RegistryCodeHandler)) {
      return;
    }
    connection.addCodeHandler(new RegistryCodeHandler(connection));
  }

  public static Optional<SQLDatabaseConnection> find(Connection connection) {
    return CONNECTIONS.stream().filter(c -> c.getConnection() == connection).findFirst();
  }

  public static List<SQLDatabaseConnection> getActiveConnections() {
    return new ArrayList<>(CONNECTIONS);
  }

  public static void debug(Connection connection, String message) {
    find(connection)
            .filter(c -> c instanceof SQLDatabaseConnectionImpl)
            .ifPresent(c -> {
              if (c.isDebug()) ((SQLDatabaseConnectionImpl) c).debug(message);
            });
  }

  @RequiredArgsConstructor
  private static class RegistryCodeHandler implements SQLDatabaseConnection.CodeObserver {
    private final SQLDatabaseConnection connection;

    // This will ensure that only connected instances are present in the registry
    @Override
    public void onNotified(int code) {
      if (code == SQLDatabaseConnection.Code.CONNECTED) {
        CONNECTIONS.add(connection);
      } else if (code == SQLDatabaseConnection.Code.CLOSED) {
        CONNECTIONS.remove(connection);
      }
    }
  }
}
