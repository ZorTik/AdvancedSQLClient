package me.zort.sqllib.api.mapping;

import java.util.List;

/**
 * Holds mapping proxy instances.
 *
 * @author ZorTik
 */
public interface StatementMappingRegistry {

    void registerProxy(MappingProxyInstance<?> proxyInstance);
    List<MappingProxyInstance<?>> getProxyInstances();

}
