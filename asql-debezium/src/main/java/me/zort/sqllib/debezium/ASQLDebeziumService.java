package me.zort.sqllib.debezium;

import com.google.common.annotations.Beta;
import io.debezium.config.Configuration;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.SQLDatabaseConnectionImpl;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Beta
public final class ASQLDebeziumService implements DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> {

  public static @NotNull Builder configure(SQLDatabaseConnection connection) {
    if (!(connection instanceof SQLDatabaseConnectionImpl)) {
      throw new IllegalArgumentException("Connection does not contain options!");
    }
    Connection rawConnection = connection.getConnection();
    Configuration.Builder configBuilder = Configuration.create()
            .with("database.hostname", "TODO");
    // TODO: Build configuration builder from raw connection details
    return new Builder(configBuilder);
  }

  private final DebeziumEngine<ChangeEvent<String, String>> engine;
  private final ExecutorService executor;
  private boolean running = false;

  private ASQLDebeziumService(DebeziumEngine.Builder<ChangeEvent<String, String>> builder, ExecutorService executor) {
    this.engine = builder.notifying(this).build();
    this.executor = executor;
  }

  @Override
  public void handleBatch(
          List<ChangeEvent<String, String>> records,
          DebeziumEngine.RecordCommitter<ChangeEvent<String, String>> committer
  ) throws InterruptedException {
    for (ChangeEvent<String, String> record : records) {
      // TODO
      committer.markProcessed(record);
    }
    committer.markBatchFinished();
  }

  public void start() {
    if (executor.isShutdown() || executor.isTerminated()) {
      throw new IllegalStateException("Executor is not running!");
    } else if (running) {
      throw new IllegalStateException("Debezium service is already running!");
    }
    executor.submit(engine);
    running = true;
  }

  public static class Builder {

    private static int serviceCount = 0;
    private Configuration.Builder config;
    private ExecutorService executor;

    private Builder(Configuration.Builder initialConfig) {
      this.config = initialConfig;
      this.executor = null;
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

    public @NotNull Builder executor(ExecutorService executor) {
      this.executor = executor;
      return this;
    }

    public @NotNull ASQLDebeziumService build() {
      DebeziumEngine.Builder<ChangeEvent<String, String>> builder = DebeziumEngine.create(Json.class)
              .using(config.build().asProperties());
      return new ASQLDebeziumService(
              builder,
              executor != null ? executor : Executors.newCachedThreadPool()
      );
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
