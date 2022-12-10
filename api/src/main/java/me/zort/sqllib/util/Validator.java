package me.zort.sqllib.util;

import lombok.experimental.UtilityClass;
import me.zort.sqllib.internal.annotation.PrimaryKey;

import java.lang.reflect.Field;

@UtilityClass
public final class Validator {

    public static boolean validateAutoIncrement(Field field) {
        return field.isAnnotationPresent(PrimaryKey.class)
                && field.getDeclaredAnnotation(PrimaryKey.class).autoIncrement()
                && field.getType().equals(Integer.class);
    }

}
