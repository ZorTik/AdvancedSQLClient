package me.zort.sqllib.internal.factory;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLConnectionFactory {

    Connection connect() throws SQLException;

}
