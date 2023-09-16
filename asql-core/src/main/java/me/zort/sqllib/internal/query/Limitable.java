package me.zort.sqllib.internal.query;

public interface Limitable<P extends QueryNode<?> & Limitable<P>> { // P = self

  P offset(int offset);
  P limit(int limit);

}
