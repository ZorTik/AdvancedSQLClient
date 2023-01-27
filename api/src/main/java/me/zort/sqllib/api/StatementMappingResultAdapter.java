package me.zort.sqllib.api;

import me.zort.sqllib.api.data.QueryResult;

import java.lang.reflect.Method;

/**
 * Result adapter used for handling operations between {@link SQLConnection}
 * and {@link StatementMapping}.
 *
 * @author ZorTik
 */
public interface StatementMappingResultAdapter {

    /**
     * Adapts invoked {@link StatementMapping} method QueryResult to
     * the final result that can be passed to proxy instance.
     *
     * @param method The invoked proxy method.
     * @param result The QueryResult of the invoked method.
     * @return The adapted result.
     */
    Object adaptResult(Method method, QueryResult result);

    /**
     * Retrieves type of entity that needs to be mapped in the request
     * to be passed in adaptResult as type in QueryResult.
     *
     * @param method The invoked proxy method.
     * @return The type of entity that needs to be mapped.
     */
    Class<?> retrieveResultType(Method method);

}
