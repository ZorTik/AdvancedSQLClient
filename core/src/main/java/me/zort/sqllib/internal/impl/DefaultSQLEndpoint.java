package me.zort.sqllib.internal.impl;

public class DefaultSQLEndpoint extends SQLEndpointImpl {

    public DefaultSQLEndpoint(String url, String database, String username, String password) {
        super(String.format("jdbc:mysql://%s/%s", url, database), username, password);
    }

}
