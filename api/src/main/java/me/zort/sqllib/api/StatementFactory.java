package me.zort.sqllib.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public interface StatementFactory<T extends Statement> {

    T prepare(Connection connection) throws SQLException;

}
