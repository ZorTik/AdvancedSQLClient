package me.zort.sqllib.api;

public interface ISQLConnectionBuilder<C extends SQLConnection> {
    
    C build(ISQLDatabaseOptions options);
    
}
