package me.zort.sqllib.api;

import org.jetbrains.annotations.Nullable;

import java.sql.Connection;

public interface SQLConnection {

    boolean connect();
    void disconnect();

    @Nullable
    Connection getConnection();

    default boolean isConnected() {
        return getConnection() != null;
    }

}
