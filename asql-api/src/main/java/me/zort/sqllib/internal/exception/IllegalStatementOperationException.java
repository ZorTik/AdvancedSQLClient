package me.zort.sqllib.internal.exception;

import lombok.Getter;
import me.zort.sqllib.api.Query;
import org.jetbrains.annotations.Nullable;

public class IllegalStatementOperationException extends RuntimeException {

  @Getter(onMethod_ = {@Nullable})
  private final Query location;

  public IllegalStatementOperationException(String message) {
    this(message, null);
  }

  public IllegalStatementOperationException(String message, Query location) {
    super(message);
    this.location = location;
  }

}
