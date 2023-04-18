package me.zort.sqllib;

import com.google.gson.internal.Primitives;
import lombok.RequiredArgsConstructor;
import me.zort.sqllib.internal.annotation.JsonField;
import me.zort.sqllib.internal.annotation.NullableField;
import me.zort.sqllib.internal.annotation.PrimaryKey;
import me.zort.sqllib.util.Arrays;
import me.zort.sqllib.util.Validator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

@RequiredArgsConstructor
final class ObjectTableConverter {

    private final SQLDatabaseConnection connection;
    private final String tableName;
    private final Class<?> typeClass;
    private static final Class<?>[] supportedTypes = new Class<?>[] {
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            String.class
    };

    public String buildTableQuery() {
        return String.format("CREATE TABLE IF NOT EXISTS %s(%s);", tableName, String.join(", ", buildDefsFromType()));
    }

    public String[] buildDefsFromType() {
        Objects.requireNonNull(typeClass, "Type class must be set before building repository!");

        if(!(connection instanceof SQLDatabaseConnectionImpl)) {
            throw new IllegalStateException("We can build defs only from SQLDatabaseConnectionImpl child-classes.");
        }
        debug("Building defs from type class: " + typeClass.getName());

        SQLDatabaseOptions options = ((SQLDatabaseConnectionImpl) connection).getOptions();
        String[] defs = new String[0];

        for (Field field : typeClass.getDeclaredFields()) {
            debug("Building def for field: " + field.getName() + " (" + field.getType().getName() + ")");
            if(Modifier.isTransient(field.getModifiers())) {
                debug(String.format("Field %s is transient, skipping.", field.getName()));
                continue;
            }

            String colName = options.getNamingStrategy().fieldNameToColumn(field.getName());
            String colType = recognizeFieldTypeToDbType(field);

            if(colType != null && !colType.contains("NOT NULL") && field.isAnnotationPresent(NullableField.class)) {
                if(!field.getAnnotation(NullableField.class).nullable()) {
                    colType += " NOT NULL";
                }
            }

            defs = Arrays.add(defs, colName + " " + colType);

            debug("Added def: " + colName + " " + colType);
        }

        debug("Built defs: " + java.util.Arrays.toString(defs));

        return defs;
    }

    private String recognizeFieldTypeToDbType(Field field) {
        Class<?> fieldType = Primitives.wrap(field.getType());
        boolean isSupported = isSupported(fieldType);

        if(field.isAnnotationPresent(JsonField.class) && !isSupported) {
            return "TEXT";
        }

        if(!isSupported)
            throw new RuntimeException(String.format("We don't support %s types in SQLTableRepositoryBuilder yet.", fieldType.getSimpleName()));

        String dbType = null;
        if(fieldType.equals(String.class)) {
            dbType = "VARCHAR(255)";
        } else if(Primitives.wrap(fieldType).equals(Integer.class)) {
            dbType = "INTEGER";
        } else if(Primitives.wrap(fieldType).equals(Long.class)) {
            dbType = "BIGINT";
        } else if(Primitives.wrap(fieldType).equals(Double.class)) {
            dbType = "DOUBLE";
        } else if(Primitives.wrap(fieldType).equals(Float.class)) {
            dbType = "FLOAT";
        }

        if (field.isAnnotationPresent(PrimaryKey.class))
            dbType += " PRIMARY KEY";

        if(Validator.validateAutoIncrement(field))
            dbType += " " + (isSQLite() ? "AUTOINCREMENT" : "AUTO_INCREMENT");

        return dbType;
    }

    private void debug(String message) {
        if(connection instanceof SQLDatabaseConnectionImpl && ((SQLDatabaseConnectionImpl) connection).getOptions().isDebug())
            ((SQLDatabaseConnectionImpl) connection).debug(message);
    }

    private boolean isSQLite() {
        return connection instanceof SQLiteDatabaseConnectionImpl;
    }

    public static boolean isSupported(Class<?> type) {
        for (Class<?> aClass : supportedTypes) {
            if(aClass.equals(type)) {
                return true;
            }
        }
        return false;
    }

}
