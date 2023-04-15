package me.zort.sqllib.pool;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.zort.sqllib.SQLConnectionBuilder;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.SQLDatabaseConnectionImpl;
import org.jetbrains.annotations.ApiStatus;
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
@ApiStatus.AvailableSince("0.6.0")
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

    private int errorCount = 0;

    // --***-- Pooled connection caches --***--
    private final Queue<PooledSQLDatabaseConnection> freeConnections = new ConcurrentLinkedQueue<>();
    private final List<PooledSQLDatabaseConnection> usedConnections = new CopyOnWriteArrayList<>();

    @SuppressWarnings("unused")
    public SQLConnectionPool(final @NotNull SQLConnectionBuilder from) {
        this(from, new Options());
    }

    /**
     * Creates a new connection pool.
     * A builder is used as a factory for creating new connections.
     *
     * @param from The builder used to create new connections.
     * @param poolOptions The pool options.
     */
    public SQLConnectionPool(final @NotNull SQLConnectionBuilder from, final @NotNull Options poolOptions) {
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
    public SQLDatabaseConnection getResource() throws SQLException {
        freeConnections.removeIf(this::expired);
        PooledSQLDatabaseConnection polled = freeConnections.poll();
        if (polled == null && size() < maxConnections) {
            polled = establishObject();
        } else if(polled == null) {

            if (!blockWhenExhausted) {
                throw new SQLException("No connections available.");
            }

            long start = System.currentTimeMillis();
            while ((polled = freeConnections.poll()) == null) {
                if (System.currentTimeMillis() - start > borrowObjectTimeout) {
                    throw new SQLException("Timeout while waiting for a connection.");
                } else if(size() < maxConnections) {
                    polled = establishObject();
                    break;
                }
            }
        }
        usedConnections.add(polled);
        return polled;
    }

    private PooledSQLDatabaseConnection establishObject() throws SQLException {
        SQLDatabaseConnection polled_ = builder.build();
        if (!(polled_ instanceof PooledSQLDatabaseConnection)) {
            throw new SQLException("Builder does not produce a pooled connection.");
        }
        PooledSQLDatabaseConnection polled = (PooledSQLDatabaseConnection) polled_;
        polled.setAssignedPool(this);
        polled.connect();

        SQLException error = polled.getLastError();
        if (error != null) throw error;

        if (polled instanceof SQLDatabaseConnectionImpl) {
            ((SQLDatabaseConnectionImpl) polled).addErrorHandler(code -> {
                errorCount++;
                // Remove the connection from the pool and disconnect
                // on fatal errors.
                freeConnections.remove(polled);
                usedConnections.remove(polled);
                polled.disconnect();
            });
        }

        return polled;
    }

    void releaseObject(PooledSQLDatabaseConnection connection) {
        connection.setLastUsed(System.currentTimeMillis());
        freeConnections.add(connection);
        usedConnections.remove(connection);
    }

    public int size() {
        return usedConnections.size() + freeConnections.size();
    }

    public int errorCount() {
        return errorCount;
    }

    /**
     * Closes all connections in the pool and
     * clears the caches.
     */
    public void close() {
        usedConnections.forEach(SQLDatabaseConnection::disconnect);
        freeConnections.forEach(SQLDatabaseConnection::disconnect);
        usedConnections.clear();
        freeConnections.clear();
    }

    private boolean expired(PooledSQLDatabaseConnection connection) {
        return System.currentTimeMillis() - connection.getLastUsed() > 1000 * 60 * 5;
    }

}
