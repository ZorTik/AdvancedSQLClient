package me.zort.sqllib.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ColumnDefinition {

  private final String name;
  private final String type;

  @Override
  public String toString() {
    return name + " " + type;
  }

}
