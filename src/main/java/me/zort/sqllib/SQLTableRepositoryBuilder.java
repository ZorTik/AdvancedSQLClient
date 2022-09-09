package me.zort.sqllib;

import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.api.repository.CachingSQLTableRepository;
import me.zort.sqllib.api.repository.SQLTableRepository;

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
        return factory.create(info);
    }

    public interface RepoFactory<T, ID, R extends SQLTableRepository<T, ID>> {
        R create(SQLTableRepository.RepositoryInfo<T, ID> info);
    }

}
