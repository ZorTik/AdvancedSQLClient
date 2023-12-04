package me.zort.sqllib.internal.impl;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnectionImpl;
import me.zort.sqllib.api.DefsVals;
import me.zort.sqllib.api.ISQLDatabaseOptions;
import me.zort.sqllib.api.ObjectMapper;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.annotation.JsonField;
import me.zort.sqllib.util.Validator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultObjectMapper implements ObjectMapper {

  // Resolvers used after no value is found for the field
  // in mapped object as backup.
  @Getter(AccessLevel.PROTECTED)
  private final List<ObjectMapper.FieldValueResolver> backupValueResolvers;
  private final Map<Class<?>, ObjectMapper.TypeAdapter<?>> typeAdapters;
  private final SQLDatabaseConnectionImpl connectionWrapper;

  public DefaultObjectMapper(SQLDatabaseConnectionImpl connectionWrapper) {
    this.backupValueResolvers = new CopyOnWriteArrayList<>();
    this.typeAdapters = new ConcurrentHashMap<>();
    this.connectionWrapper = connectionWrapper;
  }

  @Override
  public void registerBackupValueResolver(@NotNull FieldValueResolver resolver) {
    this.backupValueResolvers.add(resolver);
  }

  @Override
  public void registerAdapter(@NotNull Class<?> typeClass, @NotNull TypeAdapter<?> adapter) {
    this.typeAdapters.put(typeClass, adapter);
  }

  @Nullable
  public <T> T deserializeValues(Row row, Class<T> typeClass) {
    T instance = null;
    try {
      try {
        Constructor<T> c = typeClass.getConstructor();
        c.setAccessible(true);
        instance = c.newInstance();
      } catch (NoSuchMethodException e) {
        for (Constructor<?> c : typeClass.getConstructors()) {
          if (c.getParameterCount() == row.size()) {
            Parameter[] params = c.getParameters();
            Object[] vals = new Object[c.getParameterCount()];
            for (int i = 0; i < row.size(); i++) {
              Parameter param = params[i];
              vals[i] = buildElementValue(param, row);
            }
            try {
              c.setAccessible(true);
              instance = (T) c.newInstance(vals);
            } catch (Exception ignored) {
            }
          }
        }
      }
      for (Field field : typeClass.getDeclaredFields()) {

        if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
          continue;
        }

        try {
          field.setAccessible(true);
          field.set(instance, buildElementValue(field, row));
        } catch (SecurityException ignored) {
          debug(String.format("Field %s on class %s cannot be set accessible!",
                  field.getName(),
                  typeClass.getName()));
        } catch (Exception ignored) {
        }
      }
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      debug("Cannot instantinate " + typeClass.getName() + " for assigning attributes from row!");
      e.printStackTrace();
      return null;
    }
    return instance;
  }

  @Nullable
  private Object buildElementValue(AnnotatedElement element, Row row) {
    String name;
    Type type;
    Class<?> declaringClass;
    if (element instanceof Field) {
      name = ((Field) element).getName();
      type = ((Field) element).getGenericType();
      declaringClass = ((Field) element).getDeclaringClass();
    } else if (element instanceof Parameter) {
      name = ((Parameter) element).getName();
      type = ((Parameter) element).getType();
      declaringClass = ((Parameter) element).getDeclaringExecutable().getDeclaringClass();
    } else {
      return null;
    }
    Object obj = row.get(name);
    if (obj == null) {
      String converted;
      if ((obj = row.get(converted = connectionWrapper.getOptions().getNamingStrategy().fieldNameToColumn(name))) == null) {

        // Now backup resolvers come.
        for (ObjectMapper.FieldValueResolver resolver : backupValueResolvers) {
          Object backupValue = resolver.obtainValue(connectionWrapper, element, row, name, converted, type);
          if (backupValue != null) {
            return backupValue;
          }
        }

        debug(String.format("Cannot find column for class %s target %s (%s)", declaringClass.getName(), name, converted));
        return null;
      }
    } else {
      TypeAdapter<?> typeAdapter = typeAdapters.get(type.getClass());
      if (typeAdapter != null) {
        return typeAdapter.deserialize(element, row, obj);
      }
    }
    if (element.isAnnotationPresent(JsonField.class) && obj instanceof String) {
      String jsonString = (String) obj;
      Gson gson = connectionWrapper.getOptions().getGson();
      return gson.fromJson(jsonString, type);
    } else {
      return obj;
    }
  }

  @Override
  public DefsVals serializeValues(Object obj) {
    Objects.requireNonNull(obj);

    Class<?> aClass = obj.getClass();

    Map<String, Object> fields = new HashMap<>();
    for (Field field : aClass.getDeclaredFields()) {

      if (Modifier.isTransient(field.getModifiers())) {
        // Transient fields are ignored.
        continue;
      }

      ISQLDatabaseOptions options = connectionWrapper.getOptions();

      try {
        field.setAccessible(true);
        Object o = field.get(obj);
        if (typeAdapters.containsKey(field.getType())) {
          o = typeAdapters.get(field.getType()).serialize(field, o);
        } else if (field.isAnnotationPresent(JsonField.class)) {
          o = options.getGson().toJson(o);
        } else if (Validator.validateAutoIncrement(field) && field.get(obj) == null) {
          // If field is PrimaryKey and autoIncrement true and is null,
          // We will skip this to use auto increment strategy on SQL server.
          continue;
        }
        fields.put(options.getNamingStrategy().fieldNameToColumn(field.getName()), o);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
        return null;
      }
    }
    // I make entry array for indexing safety.
    Map.Entry<String, Object>[] entryArray = fields.entrySet().toArray(new Map.Entry[0]);
    String[] defs = new String[entryArray.length];
    AtomicReference<Object>[] vals = new AtomicReference[entryArray.length];
    for (int i = 0; i < entryArray.length; i++) {
      defs[i] = entryArray[i].getKey();
      vals[i] = new AtomicReference<>(entryArray[i].getValue());
    }
    return new DefsVals(defs, vals);
  }

  private void debug(String message) {
    connectionWrapper.debug(message);
  }

}
