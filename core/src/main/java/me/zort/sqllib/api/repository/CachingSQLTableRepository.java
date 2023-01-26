package me.zort.sqllib.api.repository;

import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CachingSQLTableRepository<T, ID> extends SQLTableRepository<T, ID> {

    private final Map<ID, T> localCache;
    @Setter
    @Getter
    private int limit;

    public CachingSQLTableRepository(SQLDatabaseConnection connection, RepositoryInfo<T, ID> repositoryInfo) {
        this(connection, repositoryInfo, Integer.MAX_VALUE);
    }

    public CachingSQLTableRepository(SQLDatabaseConnection connection, RepositoryInfo<T, ID> repositoryInfo, int limit) {
        super(connection, repositoryInfo);
        this.localCache = new ConcurrentHashMap<>();
        this.limit = limit;
    }

    @Override
    public boolean save(T object) {
        boolean success = super.save(object);
        if(success) {
            localCache.put(getIdFrom(object), object);
        }
        return success;
    }

    @Override
    public Optional<T> findById(ID id) {
        Optional<T> result = super.findById(id);
        result.ifPresent(obj -> localCache.put(id, obj));
        return result;
    }

    public Optional<T> getCached(ID id) {
        return Optional.ofNullable(localCache.get(id));
    }

    private ID getIdFrom(T obj) {
        try {
            return (ID) obj.getClass().getDeclaredField(getIdFieldName()).get(obj);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

}
