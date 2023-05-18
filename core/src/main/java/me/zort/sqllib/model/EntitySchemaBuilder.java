package me.zort.sqllib.model;

import com.google.gson.internal.Primitives;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.zort.sqllib.api.model.ColumnDefinition;
import me.zort.sqllib.api.model.TableSchema;
import me.zort.sqllib.api.model.TableSchemaBuilder;
import me.zort.sqllib.api.options.NamingStrategy;
import me.zort.sqllib.internal.annotation.Default;
import me.zort.sqllib.internal.annotation.JsonField;
import me.zort.sqllib.internal.annotation.NullableField;
import me.zort.sqllib.internal.annotation.PrimaryKey;
import me.zort.sqllib.util.Arrays;
import me.zort.sqllib.util.Validator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * Builds table schema from provided entity class.
 *
 * @author ZorTik
 */
@RequiredArgsConstructor
public final class EntitySchemaBuilder implements TableSchemaBuilder {

    private final String tableName;
    private final Class<?> typeClass;
    private final NamingStrategy namingStrategy;
    private final boolean sqLite;
    private static final Class<?>[] supportedTypes = new Class<?>[] {
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            String.class
    };

    @Setter
    private boolean debug = false;

    public String buildTableQuery() {
        return String.format("CREATE TABLE IF NOT EXISTS %s(%s);", tableName, String.join(", ", buildTableSchema().getDefinitions()));
    }

    @Override
    public TableSchema buildTableSchema() {
        Objects.requireNonNull(typeClass, "Type class must be set before building repository!");

        debug("Building defs from type class: " + typeClass.getName());

        ColumnDefinition[] defs = new ColumnDefinition[0];
        for (Field field : typeClass.getDeclaredFields()) {
            debug("Building def for field: " + field.getName() + " (" + field.getType().getName() + ")");
            if(Modifier.isTransient(field.getModifiers())) {
                debug(String.format("Field %s is transient, skipping.", field.getName()));
                continue;
            }

            String colName = namingStrategy.fieldNameToColumn(field.getName());
            String colType = recognizeFieldTypeToDbType(field);

            if(colType != null && !colType.contains("NOT NULL") && field.isAnnotationPresent(NullableField.class)) {
                if(!field.getAnnotation(NullableField.class).nullable()) {
                    colType += " NOT NULL";
                }
            }
            if (colType != null && field.isAnnotationPresent(Default.class)) {
                String defaultValue = field.getAnnotation(Default.class).value();
                if ((field.getType().equals(String.class) || field.isAnnotationPresent(JsonField.class))
                        && !(defaultValue.startsWith("'") && defaultValue.endsWith("'")))
                    defaultValue = "'" + defaultValue + "'";
                colType += " DEFAULT " + defaultValue;
            }

            defs = Arrays.add(defs, new ColumnDefinition(colName, colType));

            debug("Added def: " + colName + " " + colType);
        }

        debug("Built defs: " + java.util.Arrays.toString(defs));

        return new TableSchema(tableName, defs);
    }

    private String recognizeFieldTypeToDbType(Field field) {
        Class<?> fieldType = Primitives.wrap(field.getType());
        boolean isSupported = isSupported(fieldType);

        if(field.isAnnotationPresent(JsonField.class) && !isSupported) {
            return "TEXT";
        }

        if(!isSupported)
            throw new RuntimeException(String.format("We don't support %s types in SQLTableRepositoryBuilder yet. (%s)",
                    fieldType.getSimpleName(),
                    field.getDeclaringClass().getName()));

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
        if (debug) System.out.println(message);
    }

    private boolean isSQLite() {
        return sqLite;
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
