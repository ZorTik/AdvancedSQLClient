package me.zort.sqllib.mapping;

import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.SQLDatabaseConnectionImpl;
import me.zort.sqllib.SQLiteDatabaseConnectionImpl;
import me.zort.sqllib.api.mapping.MappingProxyInstance;
import me.zort.sqllib.api.mapping.StatementMappingRegistry;
import me.zort.sqllib.api.model.SchemaSynchronizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MappingRegistryImpl implements StatementMappingRegistry {

    private final Map<Class<?>, MappingProxyInstance<?>> proxyWrappers = new ConcurrentHashMap<>();
    private final SQLDatabaseConnectionImpl connection;
    @Setter
    @Getter
    private SchemaSynchronizer<SQLDatabaseConnection> synchronizer;

    public MappingRegistryImpl(SQLDatabaseConnectionImpl connection, SchemaSynchronizer<SQLDatabaseConnection> synchronizer) {
        this.connection = connection;
        this.synchronizer = synchronizer;
    }

    @Override
    public void registerProxy(MappingProxyInstance<?> proxyInstance) {
        proxyWrappers.put(proxyInstance.getProxyInstance().getClass(), proxyInstance);
    }

    @Override
    public List<MappingProxyInstance<?>> getProxyInstances() {
        return new ArrayList<>(proxyWrappers.values());
    }

}
