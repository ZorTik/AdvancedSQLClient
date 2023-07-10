package me.zort.sqllib.api.model;

import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.data.QueryResult;

/**
 * Synchronized (updates) table schema (column definitions) in provided source.
 *
 * @param <S> The target source
 */
public interface SchemaSynchronizer<S extends SQLConnection> {

  /**
   * Table synchronization logic in provided source.
   * This should update the 'to' schema to be synchronized with the 'from'
   * schema.
   *
   * @param source The source where to apply changes
   * @param from   The template schema
   * @param to     The schema to be <u>updated</u> to match 'from' schema
   */
  QueryResult synchronize(S source, TableSchema from, TableSchema to);

}
