package me.zort.sqllib.mapping;

import me.zort.sqllib.api.mapping.MappingProxyInstance;
import me.zort.sqllib.api.mapping.StatementMappingRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MappingRegistryImpl implements StatementMappingRegistry {

  private final Map<Class<?>, MappingProxyInstance<?>> proxyWrappers = new ConcurrentHashMap<>();

  @Override
  public void registerProxy(MappingProxyInstance<?> proxyInstance) {
    proxyWrappers.put(proxyInstance.getTypeClass(), proxyInstance);
  }

  @Override
  public List<MappingProxyInstance<?>> getProxyInstances() {
    return new ArrayList<>(proxyWrappers.values());
  }

}
