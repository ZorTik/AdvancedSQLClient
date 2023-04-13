package me.zort.sqllib;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
public final class SQLConnectionPool {

    @Data
    public static final class Options {
        private int maxConnections = 10;
    }

    private final SQLConnectionBuilder from;
    private final int maxConnections;
    private final Queue<SQLPooledConnection> freeConnections = new ConcurrentLinkedQueue<>();
    private final List<SQLPooledConnection> usedConnections = new CopyOnWriteArrayList<>();

    public SQLConnectionPool(@NotNull SQLConnectionBuilder from) {
        this(from, new Options());
    }

    public SQLConnectionPool(@NotNull SQLConnectionBuilder from, @NotNull Options poolOptions) {
        this.from = from;
        this.maxConnections = poolOptions.maxConnections;
    }

    @NotNull
    public Resource getResource() throws SQLException {
        freeConnections.removeIf(SQLPooledConnection::expired);
        SQLPooledConnection polled = freeConnections.poll();
        if (polled == null && usedConnections.size() < maxConnections) {
            polled = new SQLPooledConnection(from.build());
            polled.connection.connect();

            SQLException error = polled.connection.getLastError();
            if (error != null) throw error;
        } else if(polled == null) {
            throw new IllegalStateException("Connection limit reached!");
        }
        usedConnections.add(polled);
        return new Resource(this, polled);
    }

    public void close() {
        usedConnections.forEach(c -> c.connection.disconnect());
        freeConnections.forEach(c -> c.connection.disconnect());
        usedConnections.clear();
        freeConnections.clear();
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class SQLPooledConnection {
        private final SQLDatabaseConnection connection;
        private long lastUsed = System.currentTimeMillis();

        public boolean expired() {
            return System.currentTimeMillis() - lastUsed > 1000 * 60 * 5;
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Resource implements AutoCloseable {

        private final SQLConnectionPool pool;
        private final SQLPooledConnection connection;

        public SQLDatabaseConnection getConnection() {
            return connection.connection;
        }

        @Override
        public void close() {
            connection.lastUsed = System.currentTimeMillis();
            pool.freeConnections.add(connection);
            pool.usedConnections.remove(connection);
        }
    }

}
