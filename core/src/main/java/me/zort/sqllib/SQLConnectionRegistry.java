package me.zort.sqllib;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SQLConnectionRegistry {

    private SQLConnectionRegistry() {
    }

    private static final List<SQLDatabaseConnection> CONNECTIONS = new CopyOnWriteArrayList<>();

    static void register(SQLDatabaseConnection connection) {
        CONNECTIONS.add(connection);
    }

    static Optional<SQLDatabaseConnection> find(Connection connection) {
        return CONNECTIONS.stream().filter(c -> c.getConnection() == connection).findFirst();
    }

    public static void debug(Connection connection, String message) {
        find(connection)
                .filter(c -> c instanceof SQLDatabaseConnectionImpl)
                .ifPresent(c -> {
                    if (c.isDebug()) ((SQLDatabaseConnectionImpl) c).debug(message);
                });
    }
}
