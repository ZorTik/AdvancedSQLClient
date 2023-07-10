package me.zort.sqllib.internal.exception;

import lombok.Getter;
import me.zort.sqllib.api.SQLEndpoint;

public class SQLEndpointNotValidException extends RuntimeException {

  @Getter
  private final SQLEndpoint endpoint;

  public SQLEndpointNotValidException(SQLEndpoint endpoint) {
    super("Provided SQL endpoint is not valid!");
    this.endpoint = endpoint;
  }

}
