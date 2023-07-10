package me.zort.sqllib.internal.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.zort.sqllib.api.SQLEndpoint;

@AllArgsConstructor
@Getter
public class SQLEndpointImpl implements SQLEndpoint {

  private final String jdbc;
  private final String username;
  private final String password;

  @Override
  public String buildJdbc() {
    return jdbc;
  }

}
