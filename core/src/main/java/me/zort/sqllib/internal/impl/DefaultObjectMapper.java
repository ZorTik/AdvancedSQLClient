package me.zort.sqllib.internal.impl;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnectionImpl;
import me.zort.sqllib.api.ObjectMapper;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.annotation.JsonField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultObjectMapper implements ObjectMapper {

    @Getter(AccessLevel.PROTECTED)
    private final List<ObjectMapper.FieldValueResolver> backupValueResolvers;
    // Resolvers used after no value is found for the field
    // in mapped object as backup.
    private final SQLDatabaseConnectionImpl connectionWrapper;

    public DefaultObjectMapper(SQLDatabaseConnectionImpl connectionWrapper) {
        this.backupValueResolvers = new CopyOnWriteArrayList<>();
        this.connectionWrapper = connectionWrapper;
    }

    @Override
    public void registerBackupValueResolver(@NotNull FieldValueResolver resolver) {
        this.backupValueResolvers.add(resolver);
    }

    @Nullable
    public <T> T assignValues(Row row, Class<T> typeClass) {
        T instance = null;
        try {
            try {
                Constructor<T> c = typeClass.getConstructor();
                c.setAccessible(true);
                instance = c.newInstance();
            } catch (NoSuchMethodException e) {
                for(Constructor<?> c : typeClass.getConstructors()) {
                    if(c.getParameterCount() == row.size()) {
                        Parameter[] params = c.getParameters();
                        Object[] vals = new Object[c.getParameterCount()];
                        for(int i = 0; i < row.size(); i++) {
                            Parameter param = params[i];
                            vals[i] = buildElementValue(param, row);
                        }
                        try {
                            c.setAccessible(true);
                            instance = (T) c.newInstance(vals);
                        } catch(Exception ignored) {
                            continue;
                        }
                    }
                }
            }
            for(Field field : typeClass.getDeclaredFields()) {

                if(Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    field.set(instance, buildElementValue(field, row));
                } catch(SecurityException ignored) {
                    debug(String.format("Field %s on class %s cannot be set accessible!",
                            field.getName(),
                            typeClass.getName()));
                } catch(Exception ignored) {
                    continue;
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
        if(element instanceof Field) {
            name = ((Field) element).getName();
            type = ((Field) element).getGenericType();
        } else if(element instanceof Parameter) { // TODO: Parameter names are arg[a-zA-Z0-9]+, use different strategy.
            name = ((Parameter) element).getName();
            type = ((Parameter) element).getType();
        } else {
            return null;
        }
        Object obj = row.get(name);
        if(obj == null) {
            String converted;
            if((obj = row.get(converted = connectionWrapper.getOptions().getNamingStrategy().fieldNameToColumn(name))) == null) {

                // Now backup resolvers come.
                for(ObjectMapper.FieldValueResolver resolver : backupValueResolvers) {
                    Object backupValue = resolver.obtainValue(connectionWrapper, element, row, name, converted, type);
                    if(backupValue != null) {
                        return backupValue;
                    }
                }

                debug(String.format("Cannot find column for target %s (%s)", name, converted));
                return null;
            }
        }
        if(element.isAnnotationPresent(JsonField.class) && obj instanceof String) {
            String jsonString = (String) obj;
            Gson gson = connectionWrapper.getOptions().getGson();
            return gson.fromJson(jsonString, type);
        } else {
            return obj;
        }
    }

    private void debug(String message) {
        connectionWrapper.debug(message);
    }

}
