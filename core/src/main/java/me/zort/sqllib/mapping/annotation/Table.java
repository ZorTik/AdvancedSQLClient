package me.zort.sqllib.mapping.annotation;

import me.zort.sqllib.mapping.PlaceholderMapper;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Table {
    String value();

    class Util {
        @Nullable
        public static String getFromContext(Method method, PlaceholderMapper mapper) {
            if (method.isAnnotationPresent(Table.class)) {
                return mapper.assignValues(method.getAnnotation(Table.class).value());
            } else if(method.getDeclaringClass().isAnnotationPresent(Table.class)) {
                return mapper.assignValues(method.getDeclaringClass().getAnnotation(Table.class).value());
            } else {
                return null;
            }
        }
    }
}