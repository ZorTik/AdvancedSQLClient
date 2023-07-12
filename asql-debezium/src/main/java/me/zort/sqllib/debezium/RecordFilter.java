package me.zort.sqllib.debezium;

import io.debezium.engine.ChangeEvent;
import me.zort.sqllib.mapping.annotation.Table;
import me.zort.sqllib.util.ParameterPair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.util.function.Predicate;

/**
 * Tests a record if the provided record should be notified
 * to the registered consumer in {@link ASQLDebeziumService}.
 *
 * @author ZorTik
 */
public final class RecordFilter {

  private static final long DEFAULT_EXPIRATION = 10000L;

  private final Predicate<ChangeEvent<String, String>> testFunction;
  private long expireAfter;
  private long expireAt = -1;

  public RecordFilter(Predicate<ChangeEvent<String, String>> testFunction) {
    this.testFunction = testFunction;
    setExpireAfter(DEFAULT_EXPIRATION);
  }

  public boolean test(ChangeEvent<String, String> record) {
    return testFunction.test(record);
  }

  public void setExpireAfter(long expireAfter) {
    if (isRegistered()) {
      throw new IllegalStateException("Filter expiration cannot be changed after it has been registered!");
    }
    this.expireAfter = expireAfter;
  }

  public boolean isRegistered() {
    return expireAt != -1;
  }

  public boolean expired() {
    return System.currentTimeMillis() >= expireAt;
  }

  void markRegistered() {
    if (isRegistered()) {
      throw new IllegalStateException("Filter has already been registered!");
    }
    expireAt = System.currentTimeMillis() + expireAfter;
  }

  public static @NotNull RecordFilter table(AnnotatedElement element, ParameterPair... parameters) {
    final String table = Table.Util.getFromContext(element, parameters);
    return new RecordFilter(record -> record.destination().equals(table));
  }

  public static @NotNull RecordFilter column(String column) {
    return new RecordFilter(record -> record.key().equals(column));
  }

  public static @NotNull RecordFilter column(String column, String value) {
    return new RecordFilter(record -> record.key().equals(column) && record.value().equals(value));
  }

  public static @NotNull RecordFilter join(RecordFilter... filters) {
    return new RecordFilter(record -> {
      for (RecordFilter filter : filters) {
        if (!filter.test(record)) {
          return false;
        }
      }
      return true;
    });
  }

}
