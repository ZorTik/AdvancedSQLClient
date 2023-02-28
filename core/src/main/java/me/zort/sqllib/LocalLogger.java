package me.zort.sqllib;

import java.sql.Connection;

public final class LocalLogger {

    private LocalLogger() {
    }

    public static void debug(Connection connection, String message) {
        SQLConnectionPool.find(connection)
                .filter(c -> c instanceof SQLDatabaseConnectionImpl)
                .ifPresent(c -> debug((SQLDatabaseConnectionImpl) c, message));
    }

    public static void debug(SQLDatabaseConnectionImpl connection, String message) {
        if (connection.isDebug())
            connection.debug(message);
    }

}
