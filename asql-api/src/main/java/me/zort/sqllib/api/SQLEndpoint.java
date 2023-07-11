package me.zort.sqllib.api;

public interface SQLEndpoint {

  String buildJdbc();

  String getUsername();

  String getPassword();

  default boolean isValid() {
    return true;
  }

}
