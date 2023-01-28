package me.zort.sqllib.mapping.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a value to be declared to placeholder on some mapping
 * annotations in a default proxy mapping.
 *
 * @author ZorTik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Placeholder {

    // The placeholder, without brackets
    String value();

}
