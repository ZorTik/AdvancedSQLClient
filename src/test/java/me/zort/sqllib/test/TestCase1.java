package me.zort.sqllib.test;

import me.zort.sqllib.SQLConnectionBuilder;
import me.zort.sqllib.SQLDatabaseOptions;
import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.internal.impl.DefaultSQLEndpoint;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledOnOs(OS.LINUX)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCase1 {

    private SQLDatabaseConnection connection;

    @BeforeAll
    public void prepare() {
        String host = System.getenv("D_MYSQL_HOST");

        if(host == null)
            host = "localhost";

        System.out.println("Using host: " + host);

        SQLDatabaseOptions options = new SQLDatabaseOptions();
        options.setDebug(true);

        DefaultSQLEndpoint endpoint = new DefaultSQLEndpoint(String.format("%s:3306", host), "test", "root", "test");

        connection = SQLConnectionBuilder.of(endpoint)
                .withDriver("com.mysql.cj.jdbc.Driver")
                .build(options);

        assertEquals(endpoint.buildJdbc(), String.format("jdbc:mysql://%s:3306/test", host));
        assertTrue(connection.connect());
        assertTrue(connection.isConnected());
    }

    @AfterAll
    public void close() {
        connection.disconnect();
    }

    @Test
    public void testUpsert() {

    }

    @Test
    public void testSelect() {

    }

}
