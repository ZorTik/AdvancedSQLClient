package me.zort.sqllib.api;

/**
 * The StatementMappingFactory is responsible for creating new StatementMapping
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
    <T> StatementMapping<T> create(Class<T> interfaceClass, SQLConnection connection);

}
