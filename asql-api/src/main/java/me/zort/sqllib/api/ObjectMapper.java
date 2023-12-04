package me.zort.sqllib.api;

import me.zort.sqllib.api.data.Row;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

public interface ObjectMapper {

  void registerBackupValueResolver(@NotNull FieldValueResolver resolver);
  void registerAdapter(@NotNull Class<?> typeClass, @NotNull TypeAdapter<?> adapter);

  <T> T deserializeValues(Row row, Class<T> typeClass);
  DefsVals serializeValues(Object obj);

  interface FieldValueResolver {
    Object obtainValue(SQLConnection connection,
                       AnnotatedElement element,
                       Row row,
                       String fieldName,
                       String convertedName,
                       Type type);
  }

  interface TypeAdapter<T> {
    /**
     * Deserializes value from database to Java value.
     * This value is commonly set to field value in corresponding
     * object. Don't set the value to the element manually!
     *
     * @param element Element to deserialize value for.
     * @param row Row to deserialize value from.
     * @param raw Raw value to deserialize.
     * @return Deserialized value that will be set to the element.
     */
    T deserialize(AnnotatedElement element, Row row, Object raw);

    /**
     * Serializes value to be inserted into database.
     *
     * @param element Element to serialize value for.
     * @param value Value to serialize.
     *              This value should be suitable to the database type,
     *              so you should always check if the value corresponds to
     *              the column type, etc.
     * @return The serialized value that will be inserted into database.
     */
    Object serialize(AnnotatedElement element, Object value);
  }
}
