package me.zort.sqllib.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Pair<F, S> {

  private final F first;
  private final S second;

}
