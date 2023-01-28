package me.zort.sqllib.api.mapping;

import me.zort.sqllib.api.SQLConnection;

/**
 * The StatementMappingFactory is responsible for creating new {@link StatementMappingStrategy}
 * for defined interfaces.
 *
 * @author ZorTik
 */
public interface StatementMappingFactory {

    /**
     * Creates a new StatementMapping for the given interface class that
     * is responsible for handling that specific interface.
     *
     * @param interfaceClass The interface class
     * @param connection The connection to use.
     * @return The StatementMapping.
     * @param <T> The interface class type.
     */
    <T> StatementMappingStrategy<T> create(Class<T> interfaceClass, SQLConnection connection);

}
