package me.zort.sqllib.api.repository;

import lombok.*;
import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.api.provider.Select;
import me.zort.sqllib.internal.annotation.Id;
import me.zort.sqllib.internal.annotation.PrimaryKey;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

@Getter
public class SQLTableRepository<T, ID> {

    private final SQLDatabaseConnection connection;
    private final RepositoryInfo<T, ID> info;
    private final String idFieldName;

    public SQLTableRepository(SQLDatabaseConnection connection, RepositoryInfo<T, ID> repositoryInfo) {
        Class<T> typeClass = repositoryInfo.getTypeClass();
        Class<ID> idClass = repositoryInfo.getIdClass();
        checkValidTypeClass(typeClass, idClass);
        this.connection = connection;
        this.info = repositoryInfo;
        this.idFieldName = findIdFieldName(typeClass);
    }

    public boolean createTable() {
        String query = String.format("CREATE TABLE IF NOT EXISTS %s(%s);",
                info.getTableName(), String.join(", ", info.getDefs()));
        return connection.exec(() -> query).isSuccessful();
    }

    public boolean save(T object) {
        return connection.save(info.getTableName(), object).isSuccessful();
    }

    public Optional<T> findById(ID id) {
        return connection.query(Select.of("*").from(info.getTableName())
                .where().isEqual(idFieldName, id), info.getTypeClass())
                .stream()
                .findFirst();
    }

    @Nullable
    private String findIdFieldName(Class<T> typeClass) {
        String idFieldName = findIdFieldName(typeClass, Id.class);
        if(idFieldName == null)
            idFieldName = findIdFieldName(typeClass, PrimaryKey.class);

        return idFieldName;
    }

    @Nullable
    private String findIdFieldName(Class<T> typeClass, Class<? extends Annotation> annot) {
        for(Field field : typeClass.getDeclaredFields()) {
            if(field.isAnnotationPresent(annot)) {
                return field.getName();
            }
        }
        return null;
    }

    private void checkValidTypeClass(Class<T> typeClass, Class<ID> idClass) {
        Objects.requireNonNull(typeClass, "Type class cannot be null!");

        String idName = findIdFieldName(typeClass);
        if(idName == null) {
            throw new IllegalArgumentException("The given type class does not have any primary key!");
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor(force = true)
    @Data
    public static class RepositoryInfo<T, ID> {

        private Class<T> typeClass;
        private Class<ID> idClass;

        private String tableName;
        private String[] defs;

    }

}
