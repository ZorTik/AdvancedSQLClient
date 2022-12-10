package me.zort.sqllib.internal.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark a field as a primary key
 * for some operations.
 * @author ZorTik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PrimaryKey {

    /**
     * Tells SQLLib that this primary key is auto-incremented.
     * !!! This ONLY works for {@link Integer} type, ans also not for
     * primitive int. !!!
     * @return If this field should be auto-incremented.
     */
    boolean autoIncrement() default false;
}
