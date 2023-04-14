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

/**
 * A connection pool.
 * <p></p>
 * This class is used to create a pool of connections to a database.
 * It is recommended to use this class instead of creating a new connection
 * every time you need to execute a query.
 *
 * @author ZorTik
 */
@RequiredArgsConstructor
public final class SQLConnectionPool {

    @Data
    public static final class Options {
        // Max number of connections in the pool
        private int maxConnections = 10;
        // Max wait time for getResource in milliseconds when the pool is exhausted
        private long borrowObjectTimeout = 5000L;
        // Block or throw an exception when the pool is exhausted
        private boolean blockWhenExhausted = true;
    }

    private final SQLConnectionBuilder builder;
    private final int maxConnections;
    private final long borrowObjectTimeout;
    private final boolean blockWhenExhausted;

    // --***-- Pooled connection caches --***--
    private final Queue<SQLPooledConnection> freeConnections = new ConcurrentLinkedQueue<>();
    private final List<SQLPooledConnection> usedConnections = new CopyOnWriteArrayList<>();

    public SQLConnectionPool(@NotNull SQLConnectionBuilder from) {
        this(from, new Options());
    }

    /**
     * Creates a new connection pool.
     * A builder is used as a factory for creating new connections.
     *
     * @param from The builder used to create new connections.
     * @param poolOptions The pool options.
     */
    public SQLConnectionPool(@NotNull SQLConnectionBuilder from, @NotNull Options poolOptions) {
        this.builder = from;
        this.maxConnections = poolOptions.maxConnections;
        this.borrowObjectTimeout = poolOptions.borrowObjectTimeout;
        this.blockWhenExhausted = poolOptions.blockWhenExhausted;
    }

    /**
     * Gets a resource from the pool, or returns an existing one from
     * the pool if there is any available.
     *
     * @return The resource.
     * @throws SQLException Connection error.
     */
    @NotNull
    public Resource getResource() throws SQLException {
        freeConnections.removeIf(SQLPooledConnection::expired);
        SQLPooledConnection polled = freeConnections.poll();
        if (polled == null && usedConnections.size() < maxConnections) {
            polled = establishObject();
        } else if(polled == null) {

            if (!blockWhenExhausted) {
                throw new SQLException("No connections available.");
            }

            long start = System.currentTimeMillis();
            while ((polled = freeConnections.poll()) == null) {
                if (System.currentTimeMillis() - start > borrowObjectTimeout) {
                    throw new SQLException("Timeout while waiting for a connection.");
                } else if(usedConnections.size() < maxConnections) {
                    polled = establishObject();
                    break;
                }
            }
        }
        usedConnections.add(polled);
        return new Resource(this, polled);
    }

    private SQLPooledConnection establishObject() throws SQLException {
        SQLPooledConnection polled = new SQLPooledConnection(builder.build());
        polled.connection.connect();

        SQLException error = polled.connection.getLastError();
        if (error != null) throw error;
        return polled;
    }

    public int size() {
        return usedConnections.size() + freeConnections.size();
    }

    /**
     * Closes all connections in the pool and
     * clears the caches.
     */
    public void close() {
        usedConnections.forEach(c -> c.connection.disconnect());
        freeConnections.forEach(c -> c.connection.disconnect());
        usedConnections.clear();
        freeConnections.clear();
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class SQLPooledConnection {
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
