package me.zort.sqllib.api;

import me.zort.sqllib.api.data.Row;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

public interface ObjectMapper {

    void registerBackupValueResolver(@NotNull FieldValueResolver resolver);
    <T> T assignValues(Row row, Class<T> typeClass);

    interface FieldValueResolver {
        Object obtainValue(SQLConnection connection,
                           AnnotatedElement element,
                           Row row,
                           String fieldName,
                           String convertedName,
                           Type type);
    }
}
