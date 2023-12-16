package me.zort.sqllib;

import me.zort.sqllib.api.DefsVals;
import me.zort.sqllib.api.ISQLDatabaseOptions;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import me.zort.sqllib.internal.query.*;
import me.zort.sqllib.internal.query.part.SetStatement;
import me.zort.sqllib.model.builder.SQLiteColumnQueryBuilder;
import me.zort.sqllib.model.adjuster.SQLiteColumnTypeAdjuster;
import me.zort.sqllib.model.SQLSchemaSynchronizer;
import me.zort.sqllib.util.PrimaryKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * SQLite database connection that changes some operations
 * since SQLite does not have support for some SQL statements.
 *
 * @author ZorTik
 */
public class SQLiteDatabaseConnection extends SQLDatabaseConnectionImpl {
  private final SQLiteDatabaseConnection identity = this;

  @SuppressWarnings("unused")
  public SQLiteDatabaseConnection(final @NotNull SQLConnectionFactory connectionFactory) {
    super(connectionFactory);
    setup();
  }

  public SQLiteDatabaseConnection(final @NotNull SQLConnectionFactory connectionFactory, @Nullable ISQLDatabaseOptions options) {
    super(connectionFactory, options);
    setup();
  }

  // Adjust behaviour to be compatible with SQLite
  private void setup() {
    if (getSchemaSynchronizer() instanceof SQLSchemaSynchronizer) {
      SQLSchemaSynchronizer schemaSynchronizer = (SQLSchemaSynchronizer) getSchemaSynchronizer();
      schemaSynchronizer.setColumnQueryBuilder(new SQLiteColumnQueryBuilder(this));
      schemaSynchronizer.setColumnTypeAdjuster(new SQLiteColumnTypeAdjuster());
      schemaSynchronizer.setSeparateQueries(true);
    }
  }

  /**
   * Performs an upsert query for defined object
   * as stated in {@link SQLDatabaseConnection#save(String, Object)}.
   * <p>
   * Object needs to have {@link me.zort.sqllib.internal.annotation.PrimaryKey} annotation
   * set to determine which column is a primary key.
   *
   * @param table Table to save into.
   * @param obj   The object to save.
   * @return Result of the query.
   */
  @NotNull
  @Override
  public final UpsertQuery save(@NotNull String table, @NotNull Object obj) {
    DefsVals defsVals = getObjectMapper().serializeValues(obj);
    if (defsVals == null) throw new IllegalArgumentException("Cannot create save query! (defsVals == null)");
    String[] defs = defsVals.getDefs();
    AtomicReference<Object>[] vals = defsVals.getVals();

    debug("Saving object into table " + table + " with definitions " + Arrays.toString(defs) + " and values " + Arrays.toString(vals));

    PrimaryKey primaryKey = null;
    for (Field field : obj.getClass().getDeclaredFields()) {
      if (Modifier.isTransient(field.getModifiers())) {
        continue;
      }
      if (field.isAnnotationPresent(me.zort.sqllib.internal.annotation.PrimaryKey.class)) {
        String colName = getOptions().getNamingStrategy().fieldNameToColumn(field.getName());
        //int index = Arrays.binarySearch(defs, colName);
        int index = -1;
        int i = 0;
        for (String def : defs) {
          if (def.equals(colName)) {
            index = i;
            break;
          }
          i++;
        }
        if (index >= 0) {
          primaryKey = new PrimaryKey(colName, vals[index].get() instanceof String
                  ? (String) vals[index].get() : String.valueOf(vals[index].get()));
          break;
        }
      }
    }
    InsertQuery insert = insert().into(table, defs);
    for (AtomicReference<Object> val : vals) {
      insert.appendVal(val.get());
    }
    Function<QueryNode<?>, UpsertQuery> decor;
    decor = node -> new UpsertQuery(identity) {
      @Override
      public QueryDetails buildQueryDetails() {
        return node.buildQueryDetails();
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
    };

    if (primaryKey == null) {
      debug("No primary key found for object " + obj.getClass().getName() + ", so we can't build update condition.");
      debug("Performing insert query instead: " + insert.buildQuery());
      return decor.apply(insert);
    }

    SetStatement<UpdateQuery> setStmt = update().table(table).set();
    for (int i = 0; i < defs.length; i++) {
      setStmt.and(defs[i], vals[i].get());
    }
    UpdateQuery update = setStmt.also()
            .where().isEqual(primaryKey.getColumn(), primaryKey.getValue())
            .also();
    return decor.apply(upsert(table, primaryKey, insert, update));
  }

  /**
   * Builds an upsert query for defined table and primary key.
   * This returns either a provided insert or update query depending
   * on upsert situation.
   *
   * @param table      Table to upsert into.
   * @param primaryKey Primary key to use.
   * @param insert     Insert query to use.
   * @param update     Update query to use.
   * @return Either insert or update query.
   */
  @Nullable
  public final QueryNode<?> upsert(
          final @NotNull String table,
          final @NotNull PrimaryKey primaryKey,
          final @NotNull InsertQuery insert,
          final @NotNull UpdateQuery update
  ) {

    QueryRowsResult<Row> selectResult = select("*")
            .from(table)
            .where().isEqual(primaryKey.getColumn(), primaryKey.getValue())
            .also().limit(1)
            .obtainAll();
    if (!selectResult.isSuccessful()) {
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
}
