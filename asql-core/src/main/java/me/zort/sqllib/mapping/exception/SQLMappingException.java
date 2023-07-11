package me.zort.sqllib.mapping.exception;

import lombok.Getter;

import java.lang.reflect.Method;

@Getter
public class SQLMappingException extends RuntimeException {

  private final Method method;
  private final Object[] args;

  public SQLMappingException(String message, Method method, Object[] args) {
    super(message);
    this.method = method;
    this.args = args;
  }
}
