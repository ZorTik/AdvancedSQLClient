package me.zort.sqllib.mapping.annotation;

import me.zort.sqllib.internal.query.Limitable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Limit {

  int value();

  class Builder {
    public static <T extends Limitable<?>> T build(T parent, Limit annotation) {
      return (T) parent.limit(annotation.value());
    }
  }

}
