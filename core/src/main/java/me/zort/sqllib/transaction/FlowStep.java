package me.zort.sqllib.transaction;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryResult;

public interface FlowStep {

    Status execute(SQLDatabaseConnection connection);
    QueryResult getResult(); // Result if the #execute returned SUCCESS, otherwise null
    boolean isOptional();

    enum Status {
        SUCCESS, BREAK, CONTINUE
    }

}
