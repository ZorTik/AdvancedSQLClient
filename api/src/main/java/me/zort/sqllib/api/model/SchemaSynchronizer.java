package me.zort.sqllib.api.model;

/**
 * Synchronized (updates) table schema (column definitions) in provided source.
 *
 * @param <S> The target source
 */
public interface SchemaSynchronizer<S> {

    /**
     * Table synchronization logic in provided source.
     * This should update the 'to' schema to be synchronized with the 'from'
     * schema.
     *
     * @param source The source where to apply changes
     * @param from The template schema
     * @param to The schema to be <u>updated</u> to match 'from' schema
     */
    void synchronize(S source, TableSchema from, TableSchema to);

}
