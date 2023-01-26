package me.zort.sqllib;

import java.sql.Connection;

public final class SQLClient {

    private SQLClient() {
    }

    public static boolean isDebug(Connection connection) {
        return SQLConnectionPool.find(connection)
                .map(SQLDatabaseConnection::isDebug)
                .orElse(false);
    }

}
