package me.zort.sqllib.internal.query;

import lombok.Getter;
import me.zort.sqllib.api.Executive;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.internal.query.part.LimitStatement;
import me.zort.sqllib.internal.query.part.OffsetStatement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public class DeleteQuery extends AncestorQueryNode implements Executive, Conditional<DeleteQuery>, Limitable<DeleteQuery> {

  private String table;

  @Getter
  private final SQLDatabaseConnection connection;

  public DeleteQuery() {
    this(null);
  }

  public DeleteQuery(@Nullable SQLDatabaseConnection connection) {
    this(connection, null);
  }

  public DeleteQuery(@Nullable SQLDatabaseConnection connection, @Nullable String table) {
    super(new ArrayList<>());
    this.table = table;
    this.connection = connection;
  }

  public DeleteQuery from(String table) {
    this.table = table;
    return this;
  }

  @Override
  public DeleteQuery offset(int offset) {
    then(new OffsetStatement<>(this, new ArrayList<>(), offset));
    return this;
  }

  public DeleteQuery limit(int limit) {
    then(new LimitStatement<>(this, new ArrayList<>(), limit));
    return this;
  }

  @Override
  public QueryDetails buildQueryDetails() {
    Objects.requireNonNull(table, "Table cannot be null!");

    return new QueryDetails.Builder("DELETE FROM " + table)
            .build()
            .append(buildInnerQuery());
  }

  @Override
  public DeleteQuery then(String part) {
    return (DeleteQuery) super.then(part);
  }

}
