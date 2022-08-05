package me.zort.sqllib.api;

import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;

/**
 * Database connection object able to handle queries
 * from this library.
 *
 * @author ZorTik
 */
public interface SQLDatabaseConnection extends SQLConnection {

    /**
     * @see me.zort.sqllib.SQLDatabaseConnectionImpl#query(Query, Class)
     */
    QueryRowsResult<Row> query(Query query);

    /**
     * Executes given query and returns execution result.
     * This result does not contain any rows. If you want to
     * execute query return result of rows, see method
     * {@link SQLDatabaseConnection#query(Query)}
     *
     * @param query Query to use for building query string.
     * @return Blank rows result that only informs
     * about success state of the request.
     */
    QueryResult exec(Query query);

}
