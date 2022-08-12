package me.zort.sqllib;

import lombok.RequiredArgsConstructor;
import me.zort.sqllib.api.SQLEndpoint;
import me.zort.sqllib.internal.Constants;
import me.zort.sqllib.internal.exception.SQLDriverNotFoundException;
import me.zort.sqllib.internal.exception.SQLEndpointNotValidException;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.impl.DefaultSQLEndpoint;
import me.zort.sqllib.internal.impl.SQLEndpointImpl;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class SQLConnectionBuilder {

    public static SQLConnectionBuilder of(String address,
                                          int port,
                                          String database,
                                          String username,
                                          String password) {
        return of(new DefaultSQLEndpoint(address + ":" + port, database, username, password));
    }

    public static SQLConnectionBuilder of(String jdbc, String username, String password) {
        return of(new SQLEndpointImpl(jdbc, username, password));
    }

    public static SQLConnectionBuilder of(SQLEndpoint endpoint) {
        if(!endpoint.isValid()) {
            throw new SQLEndpointNotValidException(endpoint);
        }
        return new SQLConnectionBuilder(endpoint);
    }

    private SQLEndpoint endpoint;
    private String jdbc;
    private String driver = null;

    public SQLConnectionBuilder() {
        this(null);
    }

    public SQLConnectionBuilder(@Nullable SQLEndpoint endpoint) {
        this.endpoint = endpoint;
        this.jdbc = endpoint != null
                ? endpoint.buildJdbc()
                : null;
    }

    public SQLConnectionBuilder withEndpoint(SQLEndpoint endpoint) {
        this.endpoint = endpoint;
        this.jdbc = endpoint.buildJdbc();
        return this;
    }

    public SQLConnectionBuilder withParam(String key, String value) {
        Optional.ofNullable(endpoint)
                .ifPresent(endpoint -> {
                    jdbc += (jdbc.contains("?") ? "&" : "?");
                    jdbc += key + "=" + value;
                });
        return this;
    }

    public SQLConnectionBuilder withDriver(String driver) {
        this.driver = driver;
        return this;
    }

    public SQLDatabaseConnectionImpl build() {
        return build(new SQLDatabaseOptions());
    }

    public SQLDatabaseConnectionImpl build(SQLDatabaseOptions options) {
        return build(driver, options);
    }

    public SQLDatabaseConnectionImpl build(@Nullable String driver, SQLDatabaseOptions options) {
        Objects.requireNonNull(endpoint, "Endpoint must be set!");
        Objects.requireNonNull(jdbc);
        if(driver == null) {
            driver = Constants.DEFAULT_DRIVER;
        }
        SQLConnectionFactory connectionFactory = new BuilderSQLConnectionFactory(this, driver);
        return new SQLDatabaseConnectionImpl(connectionFactory, options);
    }

    @RequiredArgsConstructor
    public static class BuilderSQLConnectionFactory implements SQLConnectionFactory {

        private final SQLConnectionBuilder builder;
        private final String driver;

        @Nullable
        @Override
        public Connection connect() {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new SQLDriverNotFoundException(driver, e);
            }
            String jdbc = builder.jdbc;
            String usr = builder.endpoint.getUsername();
            String pwd = builder.endpoint.getPassword();
            try {
                return DriverManager.getConnection(jdbc, usr, pwd);
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

    }

}
