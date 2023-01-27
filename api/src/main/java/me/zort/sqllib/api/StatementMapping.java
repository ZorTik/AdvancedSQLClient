package me.zort.sqllib.api;

import me.zort.sqllib.api.data.QueryResult;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Represents an abstract proxy mapping that is used for execution of
 * queries using mapped methods in a proxy instance.
 *
 * @param <T> The type of the proxy instance.
 * @author ZorTik
 */
public interface StatementMapping<T> {

    /**
     * Executes query based on invoked method with invoked args from
     * proxy instance. MapTo is used as type of entity to be mapped in the
     * request. If there is no mapping required, MapTo can be null.
     *
     * @param method The method that was executed in the proxy.
     * @param args The arguments passed.
     * @param mapTo The type of entity that needs to be mapped.
     * @return The QueryResult of the executed query.
     */
    QueryResult executeQuery(Method method, Object[] args, @Nullable Class<?> mapTo);

    /**
     * Checks if the method is eligible for mapping. If this returns false,
     * the method is executed in proxy mapping as normal method, eg. does not
     * return any mapping results.
     *
     * @param method The method that was executed in the proxy.
     * @return True if the method is eligible for mapping, or false.
     */
    boolean isMappingMethod(Method method);

}
