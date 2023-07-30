package me.zort.sqllib.internal.query;

import lombok.Getter;
import me.zort.sqllib.api.Executive;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.internal.query.part.SetStatement;
import me.zort.sqllib.internal.query.part.WhereStatement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Represents a UPDATE query.
 */
public class UpdateQuery extends AncestorQueryNode implements Executive, Conditional<UpdateQuery> {

  private String table;

  @Getter
  private final SQLDatabaseConnection connection;

  public UpdateQuery() {
    this(null);
  }

  public UpdateQuery(@Nullable SQLDatabaseConnection connection) {
    this(connection, null);
  }

  public UpdateQuery(@Nullable SQLDatabaseConnection connection, @Nullable String table) {
    super(new ArrayList<>());
    this.table = table;
    this.connection = connection;
  }

  public UpdateQuery table(String table) {
    this.table = table;
    return this;
  }

  public SetStatement<UpdateQuery> set(String column, Object value) {
    SetStatement<UpdateQuery> stmt = set();
    stmt.and(column, value);
    return stmt;
  }

  public SetStatement<UpdateQuery> set() {
    SetStatement<UpdateQuery> stmt = new SetStatement<>(this);
    then(stmt);
    return stmt;
  }

  @Override
  public WhereStatement<UpdateQuery> where() {
    return Conditional.super.where(2);
  }

  @Override
  public QueryDetails buildQueryDetails() {
    Objects.requireNonNull(table, "Table cannot be null!");

    return new QueryDetails.Builder("UPDATE " + table).build().append(buildInnerQuery());
  }

  @Override
  public UpdateQuery then(String part) {
    return (UpdateQuery) super.then(part);
  }

}
