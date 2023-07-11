package me.zort.sqllib;

import lombok.RequiredArgsConstructor;
import me.zort.sqllib.api.ISQLConnectionBuilder;
import me.zort.sqllib.api.ISQLDatabaseOptions;
import me.zort.sqllib.api.SQLEndpoint;
import me.zort.sqllib.api.cache.CacheManager;
import me.zort.sqllib.cache.ExpireWriteCacheManager;
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
public final class SQLConnectionBuilder implements ISQLConnectionBuilder<SQLDatabaseConnection>, Cloneable {

  public static @NotNull SQLConnectionBuilder of(String address, int port, String database, String username, String password) {
    return of(new DefaultSQLEndpoint(address + ":" + port, database, username, password));
  }

  public static @NotNull SQLConnectionBuilder of(String jdbc, String username, String password) {
    return of(new SQLEndpointImpl(jdbc, username, password));
  }

  public static @NotNull SQLConnectionBuilder ofSQLite(String path) {
    return of(new SQLEndpointImpl("jdbc:sqlite:" + path, null, null)).withDriver("org.sqlite.JDBC");
  }

  public static @NotNull SQLConnectionBuilder of(SQLEndpoint endpoint) {
    if (!endpoint.isValid()) throw new SQLEndpointNotValidException(endpoint);
    return new SQLConnectionBuilder(endpoint);
  }

  private SQLEndpoint endpoint;
  private String jdbc;
  private String driver = null;
  private CacheManager cacheManager = null;

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

  public @NotNull SQLConnectionBuilder withCacheManager(final @Nullable CacheManager cacheManager) {
    this.cacheManager = cacheManager;
    return this;
  }

  public @NotNull SQLConnectionBuilder cacheQueries(final long expirationMillis) {
    return withCacheManager(new ExpireWriteCacheManager(expirationMillis));
  }

  public @NotNull SQLDatabaseConnection build() {
    return build(null);
  }

  public @NotNull SQLDatabaseConnection build(@Nullable ISQLDatabaseOptions options) {
    return build(driver, options);
  }

  public @NotNull SQLDatabaseConnection build(@Nullable String driver, @Nullable ISQLDatabaseOptions options) {
    SQLDatabaseConnection connection = buildConnection(driver, options);
    if (cacheManager != null) connection.enableCaching(cacheManager);
    return connection;
  }

  private @NotNull SQLDatabaseConnection buildConnection(@Nullable String driver, @Nullable ISQLDatabaseOptions options) {
    Objects.requireNonNull(endpoint, "Endpoint must be set!");
    Objects.requireNonNull(jdbc);
    if (driver == null) driver = SQLDatabaseConnectionImpl.DEFAULT_DRIVER;
    SQLConnectionFactory connectionFactory = new LocalConnectionFactory(driver);
    return jdbc.contains("jdbc:sqlite")
            ? new SQLiteDatabaseConnection(connectionFactory, options)
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
  class LocalConnectionFactory implements SQLConnectionFactory {

    private final String localDriver;

    @Nullable
    @Override
    public Connection connect() throws SQLException {
      try {
        Class.forName(localDriver);
      } catch (ClassNotFoundException e) {
        throw new SQLDriverNotFoundException(localDriver, e);
      }
      String usr = endpoint.getUsername();
      String pwd = endpoint.getPassword();
      return DriverManager.getConnection(jdbc, usr, pwd);
    }

  }

}
