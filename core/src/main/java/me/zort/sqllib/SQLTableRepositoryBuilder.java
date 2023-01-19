package me.zort.sqllib;

import com.google.gson.internal.Primitives;
import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.api.repository.CachingSQLTableRepository;
import me.zort.sqllib.api.repository.SQLTableRepository;
import me.zort.sqllib.internal.annotation.JsonField;
import me.zort.sqllib.internal.annotation.NullableField;
import me.zort.sqllib.internal.annotation.PrimaryKey;
import me.zort.sqllib.util.Arrays;
import me.zort.sqllib.util.Validator;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

@ApiStatus.Experimental // Still in development. So it can change in the future.
public class SQLTableRepositoryBuilder<T, ID> {

    private final SQLTableRepository.RepositoryInfo<T, ID> info;
    private SQLDatabaseConnection connection;

    public SQLTableRepositoryBuilder() {
        this.info = new SQLTableRepository.RepositoryInfo<>();
        this.connection = null;
    }

    public SQLTableRepositoryBuilder<T, ID> withConnection(SQLDatabaseConnection connection) {
        this.connection = connection;
        return this;
    }

    public SQLTableRepositoryBuilder<T, ID> withTypeClass(Class<T> typeClass) {
        info.setTypeClass(typeClass);
        return this;
    }

    public SQLTableRepositoryBuilder<T, ID> withIdClass(Class<ID> idClass) {
        info.setIdClass(idClass);
        return this;
    }

    public SQLTableRepositoryBuilder<T, ID> withTableName(String tableName) {
        info.setTableName(tableName);
        return this;
    }

    public SQLTableRepositoryBuilder<T, ID> withDefs(String... defs) {
        info.setDefs(defs);
        return this;
    }

    public SQLTableRepository<T, ID> build() {
        return build(info -> new SQLTableRepository<>(connection, info));
    }

    public CachingSQLTableRepository<T, ID> buildCaching() {
        return build(info -> new CachingSQLTableRepository<>(connection, info));
    }

    public <R extends SQLTableRepository<T, ID>> R build(RepoFactory<T, ID, R> factory) {
        if(info.getDefs() == null)
            buildDefsFromType();

        return factory.create(info);
    }

    private void buildDefsFromType() {
        Objects.requireNonNull(info.getTypeClass(), "Type class must be set before building repository!");

        if(!(connection instanceof SQLDatabaseConnectionImpl)) {
            throw new IllegalStateException("We can build defs only from SQLDatabaseConnectionImpl child-classes.");
        }
        debug("Building defs from type class: " + info.getTypeClass().getName());

        SQLDatabaseOptions options = ((SQLDatabaseConnectionImpl) connection).getOptions();
        String[] defs = new String[0];

        for (Field field : info.getTypeClass().getDeclaredFields()) {
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

        info.setDefs(defs);
        debug("Built defs: " + java.util.Arrays.toString(defs));
    }

    private void debug(String message) {
        if(connection instanceof SQLDatabaseConnectionImpl && ((SQLDatabaseConnectionImpl) connection).getOptions().isDebug())
            ((SQLDatabaseConnectionImpl) connection).debug(message);
    }

    private String recognizeFieldTypeToDbType(Field field) {
        Class<?>[] supportedTypes = new Class<?>[] {
                Integer.class,
                Long.class,
                Float.class,
                Double.class,
                String.class
        };
        Class<?> fieldType = Primitives.wrap(field.getType());

        boolean isSupported = false;
        for (Class<?> aClass : supportedTypes) {
            if(aClass.equals(fieldType)) {
                isSupported = true;
                break;
            }
        }

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

        if(Validator.validateAutoIncrement(field))
            dbType += " PRIMARY KEY " + (isSQLite() ? "AUTOINCREMENT" : "AUTO_INCREMENT");

        return dbType;
    }

    private boolean isSQLite() {
        return connection instanceof SQLiteDatabaseConnectionImpl;
    }

    public interface RepoFactory<T, ID, R extends SQLTableRepository<T, ID>> {
        R create(SQLTableRepository.RepositoryInfo<T, ID> info);
    }

}
