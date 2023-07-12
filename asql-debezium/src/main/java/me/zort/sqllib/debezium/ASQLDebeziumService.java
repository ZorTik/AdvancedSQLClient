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

import java.lang.reflect.AnnotatedElement;
import java.net.URI;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public final class ASQLDebeziumService implements
        DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>>, Runnable {

  @SneakyThrows
  public static @NotNull Builder configure(@NotNull SQLDatabaseConnection connection) {
    if (!(connection instanceof SQLDatabaseConnectionImpl)) {
      throw new IllegalArgumentException("Connection does not contain options!");
    } else if (!connection.isConnected()) {
      throw new IllegalArgumentException("Connection is not connected!");
    }
    Connection rawConnection = connection.getConnection();
    URI uri = new URI(rawConnection.getMetaData().getURL());
    Configuration.Builder configBuilder = Configuration.create()
            .with("database.hostname", uri.getHost())
            .with("database.port", uri.getPort());
    // TODO: Build configuration builder from raw connection details
    return new Builder(configBuilder);
  }

  private final DebeziumEngine<ChangeEvent<String, String>> engine;
  private final Map<RecordFilter, Consumer<ChangeEvent<String, String>>> handlers;

  private ASQLDebeziumService(DebeziumEngine.Builder<ChangeEvent<String, String>> builder) {
    this.engine = builder.notifying(this).build();
    this.handlers = null;
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

  @Override
  public void run() {
    engine.run();
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
              .with("name", "Asql-Debezium-" + (++serviceCount)));
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

    public @NotNull ASQLDebeziumService build() {
      DebeziumEngine.Builder<ChangeEvent<String, String>> builder = DebeziumEngine.create(Json.class)
              .using(config.build().asProperties());
      return new ASQLDebeziumService(builder);
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
