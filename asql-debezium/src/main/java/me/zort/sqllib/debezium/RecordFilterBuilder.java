package me.zort.sqllib.debezium;

import io.debezium.engine.ChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provides easy to use API for building and executing filters
 * in {@link ASQLDebeziumService}.
 *
 * @see RecordFilter
 * @see ASQLDebeziumService
 * @author ZorTik
 */
public class RecordFilterBuilder {

  private final ASQLDebeziumService service;
  private RecordFilter filter;

  public RecordFilterBuilder(ASQLDebeziumService service) {
    this.service = service;
    this.filter = null;
  }

  public @NotNull RecordFilterBuilder and(RecordFilter filter) {
    this.filter = RecordFilter.join(this.filter, filter);
    return this;
  }

  public @NotNull CompletableFuture<ChangeEvent<String, String>> begin() {
    return service.awaitChange(filter);
  }

  public @NotNull ChangeEvent<String, String> block()
          throws InterruptedException, ExecutionException {
    return service.awaitChange(filter).get();
  }

  public @NotNull ChangeEvent<String, String> block(long timeout, TimeUnit millis)
          throws InterruptedException, ExecutionException, TimeoutException {
    return service.awaitChange(filter).get(timeout, millis);
  }

}
