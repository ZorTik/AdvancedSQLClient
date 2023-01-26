package me.zort.sqllib;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SQLConnectionPool {

    private SQLConnectionPool() {
    }

    private static final List<SQLDatabaseConnection> CONNECTIONS = new CopyOnWriteArrayList<>();

    static void register(SQLDatabaseConnection connection) {
        CONNECTIONS.add(connection);
    }

    static Optional<SQLDatabaseConnection> find(Connection connection) {
        return CONNECTIONS.stream().filter(c -> c.getConnection() == connection).findFirst();
    }

}
