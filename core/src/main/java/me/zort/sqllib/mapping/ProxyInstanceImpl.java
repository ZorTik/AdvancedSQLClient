package me.zort.sqllib.mapping;

import lombok.Getter;
import me.zort.sqllib.JVM;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.mapping.MappingProxyInstance;
import me.zort.sqllib.api.mapping.StatementMappingOptions;
import me.zort.sqllib.api.mapping.StatementMappingResultAdapter;
import me.zort.sqllib.api.mapping.StatementMappingStrategy;
import me.zort.sqllib.api.model.TableSchema;
import me.zort.sqllib.api.options.NamingStrategy;
import me.zort.sqllib.mapping.annotation.Table;
import me.zort.sqllib.model.schema.EntitySchemaBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class ProxyInstanceImpl<T> implements MappingProxyInstance<T> {

    private final Class<T> typeClass;
    private final StatementMappingOptions options;
    private final StatementMappingStrategy<T> statementMapping;
    private final StatementMappingResultAdapter mappingResultAdapter;

    private final List<Method> pendingMethods = new CopyOnWriteArrayList<>();

    public ProxyInstanceImpl(Class<T> typeClass,
                             StatementMappingOptions options,
                             StatementMappingStrategy<T> statementMappingStrategy,
                             StatementMappingResultAdapter mappingResultAdapter) {
        this.typeClass = typeClass;
        this.options = options;
        this.statementMapping = statementMappingStrategy;
        this.mappingResultAdapter = mappingResultAdapter;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Allow invokation from interfaces or abstract classes only.
        Class<?> declaringClass = method.getDeclaringClass();
        if ((declaringClass.isInterface() || Modifier.isAbstract(declaringClass.getModifiers()))
                && statementMapping.isMappingMethod(method)) {
            // Prepare and execute query based on invoked method.
            QueryResult result = statementMapping.executeQuery(options, method, args, mappingResultAdapter.retrieveResultType(method));
            // Adapt QueryResult to method return type.
            return mappingResultAdapter.adaptResult(method, result);
        }

        // Default methods are invoked normally.
        if (declaringClass.isInterface() && method.isDefault()) {
            return JVM.getJVM().invokeDefault(declaringClass, proxy, method, args);
        }

        throw new UnsupportedOperationException("Method " + method.getName() + " is not supported by this mapping repository!");
    }

    @Override
    public List<TableSchema> getTableSchemas(NamingStrategy namingStrategy, boolean sqLite) {
        List<TableSchema> schemaList = new ArrayList<>();
        for (Method method : getTypeClass().getDeclaredMethods()) {
            Class<?> resultType = mappingResultAdapter.retrieveResultType(method);

            if (!QueryResult.class.isAssignableFrom(resultType) && statementMapping.isMappingMethod(method)) {
                String table = options.getTable() != null ? options.getTable() : Table.Util.getFromContext(method, null);
                schemaList.add(new EntitySchemaBuilder(table, resultType, namingStrategy, sqLite).buildTableSchema());
            }
        }
        return schemaList;
    }
}
