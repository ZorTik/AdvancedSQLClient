package me.zort.sqllib.api;

import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;

public interface SQLDatabaseConnection extends SQLConnection {

    QueryRowsResult<Row> query(Query query);
    // TODO

}
