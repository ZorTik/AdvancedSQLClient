package me.zort.sqllib.debezium.filter;

import io.debezium.data.Envelope;
import io.debezium.engine.ChangeEvent;
import me.zort.sqllib.debezium.RecordFilter;

import java.util.function.Predicate;

public class EnvelopeOpRecordFilter extends RecordFilter {

  private final Envelope.Operation operation;

  public EnvelopeOpRecordFilter(Predicate<ChangeEvent<String, String>> testFunction,
                                Envelope.Operation operation) {
    super(testFunction);
    this.operation = operation;
  }

  @Override
  public boolean test(ChangeEvent<String, String> record) {
    // TODO
    return super.test(record);
  }
}
