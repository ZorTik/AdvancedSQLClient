package me.zort.sqllib.model;

import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.model.SchemaSynchronizer;
import me.zort.sqllib.api.model.TableSchema;

public class SQLSchemaSynchronizer implements SchemaSynchronizer<SQLDatabaseConnection> {
    @Override
    public void synchronize(SQLDatabaseConnection source, TableSchema from, TableSchema to) {
        // TODO
    }
}
