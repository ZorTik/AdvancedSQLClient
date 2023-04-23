package me.zort.sqllib;

import lombok.RequiredArgsConstructor;
import me.zort.sqllib.api.SQLEndpoint;
import me.zort.sqllib.internal.Constants;
import me.zort.sqllib.internal.exception.SQLDriverNotFoundException;
import me.zort.sqllib.internal.exception.SQLEndpointNotValidException;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.impl.DefaultSQLEndpoint;
import me.zort.sqllib.internal.impl.SQLEndpointImpl;
import me.zort.sqllib.pool.SQLConnectionPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

@SuppressWarnings("unused")
public final class SQLConnectionBuilder implements Cloneable {

    public static @NotNull SQLConnectionBuilder of(String address, int port, String database, String username, String password) {
        return of(new DefaultSQLEndpoint(address + ":" + port, database, username, password));
    }

    public static @NotNull SQLConnectionBuilder of(String jdbc, String username, String password) {
        return of(new SQLEndpointImpl(jdbc, username, password));
    }

    public static @NotNull SQLConnectionBuilder ofSQLite(String path) {
        return of(new SQLEndpointImpl("jdbc:sqlite:" + path, null, null)).withDriver("org.sqlite.JDBC");
    }

    public static SQLConnectionBuilder of(SQLEndpoint endpoint) {
        if(!endpoint.isValid()) throw new SQLEndpointNotValidException(endpoint);
        return new SQLConnectionBuilder(endpoint);
    }

    private SQLEndpoint endpoint;
    private String jdbc;
    private String driver = null;

    public SQLConnectionBuilder() {
        this(null);
    }

    public SQLConnectionBuilder(@NotNull String address, int port, @NotNull String database, @Nullable String username, @Nullable String password) {
        this(new DefaultSQLEndpoint(address + ":" + port, database, username, password));
    }

    public SQLConnectionBuilder(@Nullable SQLEndpoint endpoint) {
        this.endpoint = endpoint;
        this.jdbc = endpoint != null ? endpoint.buildJdbc() : null;
    }

    public @NotNull SQLConnectionBuilder withEndpoint(final SQLEndpoint endpoint) {
        this.endpoint = endpoint;
        this.jdbc = endpoint.buildJdbc();
        return this;
    }

    public @NotNull SQLConnectionBuilder withParam(final @NotNull String key, final @NotNull String value) {
        if (endpoint != null) jdbc += (jdbc.contains("?") ? "&" : "?") + (key + "=" + value);
        return this;
    }

    public @NotNull SQLConnectionBuilder withDriver(final @Nullable String driver) {
        this.driver = driver;
        return this;
    }

    public @NotNull SQLDatabaseConnection build() {
        return build(null);
    }

    public @NotNull SQLDatabaseConnection build(@Nullable SQLDatabaseOptions options) {
        return build(driver, options);
    }

    public @NotNull SQLDatabaseConnection build(@Nullable String driver, @Nullable SQLDatabaseOptions options) {
        Objects.requireNonNull(endpoint, "Endpoint must be set!");
        Objects.requireNonNull(jdbc);
        if(driver == null) {
            driver = Constants.DEFAULT_DRIVER;
        }
        SQLConnectionFactory connectionFactory = new BuilderSQLConnectionFactory(this, driver);
        return jdbc.contains("jdbc:sqlite")
                ? new SQLiteDatabaseConnectionImpl(connectionFactory, options)
                : new SQLDatabaseConnectionImpl(connectionFactory, options);
    }

    public @NotNull SQLConnectionPool createPool(final @NotNull SQLConnectionPool.Options options) {
        return new SQLConnectionPool(this, options);
    }

    @Override
    protected SQLConnectionBuilder clone() throws CloneNotSupportedException {
        return (SQLConnectionBuilder) super.clone();
    }

    @RequiredArgsConstructor
    public static class BuilderSQLConnectionFactory implements SQLConnectionFactory {

        private final SQLConnectionBuilder builder;
        private final String driver;

        @Nullable
        @Override
        public Connection connect() throws SQLException {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new SQLDriverNotFoundException(driver, e);
            }
            String jdbc = builder.jdbc;
            String usr = builder.endpoint.getUsername();
            String pwd = builder.endpoint.getPassword();
            return DriverManager.getConnection(jdbc, usr, pwd);
        }

    }

}
