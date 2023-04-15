package me.zort.sqllib;

import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.exception.IllegalStatementOperationException;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.impl.QueryResultImpl;
import me.zort.sqllib.internal.query.InsertQuery;
import me.zort.sqllib.internal.query.UpdateQuery;
import me.zort.sqllib.internal.query.UpsertQuery;
import me.zort.sqllib.internal.query.part.SetStatement;
import me.zort.sqllib.util.Pair;
import me.zort.sqllib.util.PrimaryKey;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * SQLite database connection that changes some operations
 * since SQLite does not have support for some SQL statements.
 *
 * @author ZorTik
 */
public class SQLiteDatabaseConnectionImpl extends SQLDatabaseConnectionImpl {

    public SQLiteDatabaseConnectionImpl(SQLConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public SQLiteDatabaseConnectionImpl(SQLConnectionFactory connectionFactory, SQLDatabaseOptions options) {
        super(connectionFactory, options);
    }

    /**
     * Performs an upsert query for defined object
     * as stated in {@link SQLDatabaseConnection#save(String, Object)}.
     * <p>
     * Object needs to have {@link me.zort.sqllib.internal.annotation.PrimaryKey} annotation
     * set to determine which column is a primary key.
     *
     * @param table Table to save into.
     * @param obj The object to save.
     * @return Result of the query.
     */
    @Override
    public QueryResult save(String table, Object obj) {
        DefsVals defsVals = buildDefsVals(obj);
        if(defsVals == null) {
            return new QueryResultImpl(false);
        }
        String[] defs = defsVals.getDefs();
        UnknownValueWrapper[] vals = defsVals.getVals();

        debug("Saving object into table " + table + " with definitions " + Arrays.toString(defs) + " and values " + Arrays.toString(vals));

        PrimaryKey primaryKey = null;
        for(Field field : obj.getClass().getDeclaredFields()) {
            if(Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            if(field.isAnnotationPresent(me.zort.sqllib.internal.annotation.PrimaryKey.class)) {
                String colName = getOptions().getNamingStrategy().fieldNameToColumn(field.getName());
                //int index = Arrays.binarySearch(defs, colName);
                int index = -1;
                int i = 0;
                for (String def : defs) {
                    if(def.equals(colName)) {
                        index = i;
                        break;
                    }
                    i++;
                }
                if(index >= 0) {
                    primaryKey = new PrimaryKey(colName, vals[index].getObject() instanceof String
                            ? (String)vals[index].getObject() : String.valueOf(vals[index].getObject()));
                    break;
                }
            }
        }
        InsertQuery insert = insert().into(table, defs);
        for(UnknownValueWrapper val : vals) {
            insert.appendVal(val.getObject());
        }

        if(primaryKey == null) {
            debug("No primary key found for object " + obj.getClass().getName() + ", so we can't build update condition.");
            debug("Performing insert query instead: " + insert.buildQuery());
            return insert.execute();
        }

        SetStatement<UpdateQuery> setStmt = update().table(table).set();
        for(int i = 0; i < defs.length; i++) {
            setStmt.and(defs[i], vals[i].getObject());
        }
        UpdateQuery update = setStmt.also()
                .where().isEqual(primaryKey.getColumn(), primaryKey.getValue())
                .also();
        return upsert(table, primaryKey, insert, update);
    }

    /**
     * Simulates upsert query for SQLite.
     * It selects rows with limit 1 to check whether there is a row present
     * matching the current request and then it performs either insert or
     * update according to the result.
     *
     * @param table Table to upsert into.
     * @param primaryKey Primary key of the object.
     * @param insert Insert query.
     * @param update Update query.
     * @return Result of the query.
     */
    public QueryResult upsert(String table, PrimaryKey primaryKey, InsertQuery insert, UpdateQuery update) {
        QueryRowsResult<Row> slct = select("*")
                .from(table)
                .where().isEqual(primaryKey.getColumn(), primaryKey.getValue())
                .also().limit(1)
                .obtainAll();
        if(!slct.isSuccessful()) {
            // Not successful, we'll skip other queries.
            return new QueryResultImpl(false);
        }
        if(slct.isEmpty()) {
            // No results, we'll insert.
            return exec(insert);
        } else {
            return exec(update);
        }
    }

    @Override
    public QueryResult exec(Query query) {
        if (query instanceof UpsertQuery && ((UpsertQuery) query).getAssignedSaveObject() != null)
            return save(((UpsertQuery) query).getTable(), ((UpsertQuery) query).getAssignedSaveObject());

        return super.exec(query);
    }
}
