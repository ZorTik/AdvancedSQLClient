package me.zort.sqllib.internal.exception;

import lombok.Getter;

public class SQLDriverNotFoundException extends RuntimeException {

  @Getter
  private final String driver;

  public SQLDriverNotFoundException(String driver, Throwable cause) {
    super(String.format("Driver %s not found!", driver), cause);
    this.driver = driver;
  }

}
