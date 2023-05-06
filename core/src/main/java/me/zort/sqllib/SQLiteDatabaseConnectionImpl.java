package me.zort.sqllib;

import me.zort.sqllib.api.ISQLDatabaseOptions;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.query.*;
import me.zort.sqllib.internal.query.part.SetStatement;
import me.zort.sqllib.util.PrimaryKey;
import org.jetbrains.annotations.NotNull;
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
    private final SQLiteDatabaseConnectionImpl identity = this;

    @SuppressWarnings("unused")
    public SQLiteDatabaseConnectionImpl(final @NotNull SQLConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public SQLiteDatabaseConnectionImpl(final @NotNull SQLConnectionFactory connectionFactory, @Nullable ISQLDatabaseOptions options) {
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
    @NotNull
    @Override
    public final UpsertQuery save(@NotNull String table, @NotNull Object obj) {
        DefsVals defsVals = buildDefsVals(obj);
        if(defsVals == null) throw new IllegalArgumentException("Cannot create save query! (defsVals == null)");
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
            return new UpsertQueryDecorator(insert);
        }

        SetStatement<UpdateQuery> setStmt = update().table(table).set();
        for(int i = 0; i < defs.length; i++) {
            setStmt.and(defs[i], vals[i].getObject());
        }
        UpdateQuery update = setStmt.also()
                .where().isEqual(primaryKey.getColumn(), primaryKey.getValue())
                .also();
        return new UpsertQueryDecorator(upsert(table, primaryKey, insert, update));
    }

    /**
     * Builds an upsert query for defined table and primary key.
     * This returns either a provided insert or update query depending
     * on upsert situation.
     *
     * @param table Table to upsert into.
     * @param primaryKey Primary key to use.
     * @param insert Insert query to use.
     * @param update Update query to use.
     * @return Either insert or update query.
     */
    @Nullable
    public final QueryNode<?> upsert(final @NotNull String table,
                                    final @NotNull PrimaryKey primaryKey,
                                    final @NotNull InsertQuery insert,
                                    final @NotNull UpdateQuery update) {

        QueryRowsResult<Row> selectResult = select("*")
                .from(table)
                .where().isEqual(primaryKey.getColumn(), primaryKey.getValue())
                .also().limit(1)
                .obtainAll();
        if(!selectResult.isSuccessful()) {
            // Not successful, we'll skip other queries.
            return null;
        }
        return selectResult.isEmpty() ? insert : update;
    }

    @NotNull
    @Override
    public QueryResult exec(@NotNull Query query) {
        if (query instanceof UpsertQuery && ((UpsertQuery) query).getAssignedSaveObject() != null)
            query = save(((UpsertQuery) query).getTable(), ((UpsertQuery) query).getAssignedSaveObject());

        return super.exec(query);
    }

    class UpsertQueryDecorator extends UpsertQuery {
        private final QueryNode<?> query;

        public UpsertQueryDecorator(QueryNode<?> query) {
            super(identity);
            this.query = query;
        }

        @Override
        public QueryDetails buildQueryDetails() {
            return query.buildQueryDetails();
        }

        @Override
        public UpsertQuery into(String table, String... defs) {
            notAvailable();
            return null;
        }
        @Override
        public UpsertQuery table(String table) {
            notAvailable();
            return null;
        }
        @Override
        public UpsertQuery values(Object... values) {
            notAvailable();
            return null;
        }
        private void notAvailable() {
            throw new UnsupportedOperationException("You can't modify upsert query in SQLite mode!");
        }
    }
}
