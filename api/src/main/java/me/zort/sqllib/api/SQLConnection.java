package me.zort.sqllib.api;

import org.jetbrains.annotations.Nullable;

import java.sql.Connection;

/**
 * Main library SQL connection object. This interface
 * is able to establish connection with remote SQL
 * server.
 *
 * @author ZorTik
 */
public interface SQLConnection {

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
    void disconnect();

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
