package me.zort.sqllib.api;

import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.query.*;
import org.jetbrains.annotations.Nullable;

/**
 * Database connection object able to handle queries
 * from this library.
 *
 * @author ZorTik
 */
public interface SQLDatabaseConnection extends SQLConnection {

    /**
     * Performs new query and returns the result. This result is never null.
     * See: {@link QueryRowsResult#isSuccessful()}
     *
     * Examples:
     * <p>
     * query(Select.of().from("players"), Player.class)
     *  .stream()
     *  .map(Player::getNickname)
     *  .forEach(System.out::println);
     * <p>
     * query(() -> "SELECT * FROM players;");
     *
     * @param query The query to use while constructing query string.
     * @param typeClass Type class of object which will be instantinated and
     *                  populated with column values.
     * @param <T> Type of objects in result.
     *
     * @return Collection of row objects.
     */
    <T> QueryRowsResult<T> query(Query query, Class<T> typeClass);

    /**
     * @see SQLDatabaseConnection#query(Query, Class)
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

    SelectQuery select(String... cols);

    UpdateQuery update();

    UpdateQuery update(@Nullable String table);

    InsertQuery insert();

    InsertQuery insert(@Nullable String table);

    UpsertQuery upsert();

    UpsertQuery upsert(@Nullable String table);

    DeleteQuery delete();

}
