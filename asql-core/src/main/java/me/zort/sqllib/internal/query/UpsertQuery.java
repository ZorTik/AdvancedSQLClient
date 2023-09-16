package me.zort.sqllib.internal.query;

import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.internal.query.part.SetStatement;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * Represents an upsert (update or insert) query.
 */
public class UpsertQuery extends InsertQuery {

  @Setter
  @Getter
  private Object assignedSaveObject = null; // Can be null, only relevant in SQLite mode.

  public UpsertQuery(SQLDatabaseConnection connection) {
    super(connection);
  }

  public UpsertQuery(SQLDatabaseConnection connection, @Nullable String table) {
    super(connection, table);
  }

  @Override
  public UpsertQuery into(String table, String... defs) {
    return (UpsertQuery) super.into(table, defs);
  }

  @Override
  public UpsertQuery table(String table) {
    return (UpsertQuery) super.table(table);
  }

  @Override
  public UpsertQuery values(Object... values) {
    return (UpsertQuery) super.values(values);
  }

  public SetStatement<InsertQuery> onDuplicateKey(String column, Object value) {
    SetStatement<InsertQuery> stmt = onDuplicateKey();
    stmt.and(column, value);
    return stmt;
  }

  public SetStatement<InsertQuery> onDuplicateKey() {
    SetStatement<InsertQuery> stmt = new SetStatement<InsertQuery>(this, 3) {
      @Override
      public QueryDetails buildQueryDetails() {
        QueryDetails details = new QueryDetails(" ON DUPLICATE KEY UPDATE", new HashMap<>());

        QueryDetails superDetails = super.buildQueryDetails();
        superDetails.setQueryStr(superDetails.getQueryStr().replaceAll("SET ", ""));
        details.append(superDetails);

        return details;
      }
    };
    then(stmt);
    return stmt;
  }

  @Override
  public UpsertQuery then(String part) {
    return (UpsertQuery) super.then(part);
  }

}
