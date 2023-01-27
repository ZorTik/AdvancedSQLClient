package me.zort.sqllib.mapping;

import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.StatementMapping;
import me.zort.sqllib.api.StatementMappingFactory;

import java.lang.reflect.Modifier;

public class DefaultStatementMappingFactory implements StatementMappingFactory {
    @Override
    public <T> StatementMapping<T> create(Class<T> interfaceClass, SQLConnection connection) {
        if (!interfaceClass.isInterface() || !Modifier.isAbstract(interfaceClass.getModifiers()))
            throw new IllegalArgumentException("The given class is not an interface or is not abstract");
        return new DefaultStatementMapping<>(connection);
    }
}
