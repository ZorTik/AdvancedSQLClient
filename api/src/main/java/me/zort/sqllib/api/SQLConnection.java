package me.zort.sqllib.api;

import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.sql.Connection;

/**
 * Main library SQL connection object. This interface
 * is able to establish connection with remote SQL
 * server.
 *
 * @author ZorTik
 */
@SuppressWarnings("closeable")
public interface SQLConnection extends Closeable {

    /**
     * Tries to connect to remote SQL server.
     *
     * @return True if connection was successful,
     * otherwise false.
     */
    boolean connect();

    /**
     * Tries to disconnect from remote SQL server.
     */
    @Deprecated
    void disconnect();
    void close();

    /**
     * Returns current running connection with
     * SQL server.
     *
     * @return The connection.
     */
    @Nullable
    Connection getConnection();

    default boolean isConnected() {
        return getConnection() != null;
    }

}
