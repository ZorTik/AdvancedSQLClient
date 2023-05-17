package me.zort.sqllib.api.mapping;

import me.zort.sqllib.api.model.TableSchema;
import me.zort.sqllib.api.options.NamingStrategy;

import java.lang.reflect.InvocationHandler;
import java.util.List;

public interface MappingProxyInstance<T> extends InvocationHandler {

    Class<T> getTypeClass();
    List<TableSchema> getTableSchemas(NamingStrategy namingStrategy, boolean sqLite);

}
