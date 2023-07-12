package me.zort.sqllib.debezium;

import com.google.common.annotations.Beta;
import io.debezium.config.Configuration;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import lombok.SneakyThrows;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.SQLDatabaseConnectionImpl;
import me.zort.sqllib.debezium.builder.EntityFilterBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Service that uses debezium engine to listen for changes of row changes in
 * a database.
 *
 * <p>Example usage:
 * <pre>{@code
 *  ExecutorService executor = Executors.newSingleThreadExecutor();
 *  ASQLDebeziumService service = ASQLDebeziumService.configure(connection)
 *    .connector(ASQLDebeziumService.ConnectorType.MYSQL)
 *    .executor(executor)
 *    .build();
 *
 * </pre>
 *
 * @author ZorTik
 */
@Beta
public final class ASQLDebeziumWatcher
        implements DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> {

  @SneakyThrows
  public static @NotNull Builder configure(
          @NotNull SQLDatabaseConnection connection, String password
  ) {
    if (!(connection instanceof SQLDatabaseConnectionImpl)) {
      throw new IllegalArgumentException("Connection does not contain options!");
    } else if (!connection.isConnected()) {
      throw new IllegalArgumentException("Connection is not connected!");
    }
    DatabaseMetaData rawConnectionMeta = connection.getConnection().getMetaData();
    URI uri = new URI(rawConnectionMeta.getURL());
    return configure(
            uri.getHost(), uri.getPort(), rawConnectionMeta.getUserName(), password
    );
  }

  public static @NotNull Builder configure(
          @NotNull String hostname,
          int port,
          @NotNull String username,
          @NotNull String password
  ) throws URISyntaxException {
    Configuration.Builder configBuilder = Configuration.create()
            .with("database.hostname", hostname)
            .with("database.port", String.valueOf(port))
            .with("database.user", username)
            .with("database.password", password);
    return new Builder(configBuilder);
  }

  private final DebeziumEngine<ChangeEvent<String, String>> engine;
  private final Map<RecordFilter, Consumer<ChangeEvent<String, String>>> handlers;
  private boolean running = false;

  private ASQLDebeziumWatcher(DebeziumEngine.Builder<ChangeEvent<String, String>> builder) {
    this.engine = builder.notifying(this).build();
    this.handlers = new ConcurrentHashMap<>();
  }

  @Override
  public void handleBatch(
          List<ChangeEvent<String, String>> records,
          DebeziumEngine.RecordCommitter<ChangeEvent<String, String>> committer
  ) throws InterruptedException {
    for (ChangeEvent<String, String> record : records) {
      new HashMap<>(handlers).keySet().forEach(filter -> {
        if (filter.expired()) {
          handlers.remove(filter);
        } else if (filter.test(record)) {
          handlers.remove(filter).accept(record);
        }
      });
      committer.markProcessed(record);
    }
    committer.markBatchFinished();
  }

  public void start(ExecutorService executor) {
    if (running) {
      throw new IllegalStateException("Service is already running!");
    }
    executor.submit(engine);
    running = true;
  }

  public void stop() {
    try {
      engine.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      running = false;
    }
  }

  public @NotNull CompletableFuture<ChangeEvent<String, String>> awaitChange() {
    return awaitChange(RecordFilter.any());
  }

  /**
   * This method registers event handler to be executed when provided filter matches
   * the event. Note that the handler will be executed only once, then removed.
   *
   * @param filter Filter to match
   * @return Future accepting the event
   */
  public @NotNull CompletableFuture<ChangeEvent<String, String>> awaitChange(RecordFilter filter) {
    CompletableFuture<ChangeEvent<String, String>> future = new CompletableFuture<>();
    filter.markRegistered();
    handlers.put(filter, future::complete);
    return future;
  }

  public @NotNull EntityFilterBuilder watch(AnnotatedElement entityElement) {
    return new EntityFilterBuilder(this, entityElement);
  }

  public static class Builder {

    private static int serviceCount = 0;
    private Configuration.Builder config;

    private Builder(Configuration.Builder initialConfig) {
      this.config = initialConfig;
      edit(builder -> builder
              .with("name", "Asql-Debezium-" + (++serviceCount))
              .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
              .with("offset.storage.file.filename", System.getProperty("user.dir") + "/offsets.dat")
              .with("server.id", serviceCount)
              .with("database.history", "io.debezium.relational.history.FileDatabaseHistory")
              .with("io.debezium.relational.history.FileDatabaseHistory", System.getProperty("user.dir") + "/dbhistory.dat")
              .with("offset.flush.interval.ms", 1000));
    }

    public @NotNull Builder edit(
            Function<Configuration.Builder, Configuration.Builder> editFunc
    ) {
        this.config = editFunc.apply(this.config);
        return this;
    }

    public @NotNull Builder connector(ConnectorType type) {
      return edit(builder -> builder.with("connector.class", type.getClassName()));
    }

    public @NotNull ASQLDebeziumWatcher build() {
      Configuration configuration = config.build();
      assertProperty(configuration, "connector.class");
      assertProperty(configuration, "database.hostname");
      DebeziumEngine.Builder<ChangeEvent<String, String>> builder = DebeziumEngine.create(Json.class)
              .using(configuration.asProperties());
      return new ASQLDebeziumWatcher(builder);
    }

    private static void assertProperty(Configuration config, String name) {
      if (!config.hasKey(name)) {
        throw new IllegalArgumentException("Configuration requires property " + name);
      }
    }
  }

  public enum ConnectorType {
    MYSQL("io.debezium.connector.mysql.MySqlConnector");

    private final String className;

    ConnectorType(String className) {
      this.className = className;
    }

    public String getClassName() {
      return this.className;
    }
  }

}
