package me.zort.sqllib.debezium.builder;

import me.zort.sqllib.debezium.ASQLDebeziumWatcher;
import me.zort.sqllib.debezium.RecordFilter;
import me.zort.sqllib.debezium.RecordFilterBuilder;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;

/**
 * Provides API for building filters based on provided element that
 * is either part of an entity or proxy.
 *
 * @see RecordFilter
 * @see RecordFilterBuilder
 * @author ZorTik
 */
public class EntityFilterBuilder extends RecordFilterBuilder {

  public EntityFilterBuilder(ASQLDebeziumWatcher service, AnnotatedElement element) {
    super(service);
    super.and(RecordFilter.table(element));
  }

  public @NotNull EntityFilterBuilder expectColumnChange(String column) {
    return (EntityFilterBuilder) super.and(RecordFilter.columnChanged(column));
  }

  public @NotNull EntityFilterBuilder expectColumnChange(String column, String expectedValue) {
    return (EntityFilterBuilder) super.and(RecordFilter.columnChanged(column, expectedValue));
  }

}
