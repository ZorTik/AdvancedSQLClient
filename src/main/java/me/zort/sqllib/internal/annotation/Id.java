package me.zort.sqllib.internal.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Serves as definitive identity of entity id field.
 * This is useful if you have different PrimaryKey than
 * you want to use as id in {@link me.zort.sqllib.api.repository.SQLTableRepository},
 * otherwise a {@link PrimaryKey} is used as ID.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {
}
