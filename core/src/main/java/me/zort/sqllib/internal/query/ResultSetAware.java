package me.zort.sqllib.internal.query;

import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;

import java.util.Optional;

/**
 * Represents a query node that can handle a result set,
 * simply said can produce a rows result. This is a replacement
 * for legacy QueryNodeRequest.
 * <p></p>
 * Typically, a SELECT query is ResultSetAware, CREATE TABLE
 * query is not.
 *
 * @author ZorTik
 */
public interface ResultSetAware extends Query {

    Optional<Row> obtainOne();
    <T> Optional<T> obtainOne(Class<T> mapTo);
    QueryRowsResult<Row> obtainAll();
    <T> QueryRowsResult<T> obtainAll(Class<T> mapTo);

}
