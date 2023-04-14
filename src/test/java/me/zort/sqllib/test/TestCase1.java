package me.zort.sqllib.test;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import me.zort.sqllib.SQLConnectionBuilder;
import me.zort.sqllib.SQLConnectionPool;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.SQLDatabaseOptions;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.provider.Select;
import me.zort.sqllib.internal.impl.DefaultSQLEndpoint;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@EnabledOnOs(value = {OS.LINUX, OS.WINDOWS})
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCase1 { // Basic operations

    private SQLDatabaseConnection connection;
    private SQLConnectionBuilder builder;
    private static final String TABLE_NAME = "users";
    private final User user1 = new User("User1", 100);
    private final User user2 = new User("User2", 200);

    @BeforeAll
    public void prepareLogging() {
        Configurator.setAllLevels("", Level.ALL);
    }

    @Timeout(15)
    @BeforeAll
    public void prepare() {
        System.out.println("Preparing test case...");

        String host = System.getenv("D_MYSQL_HOST");

        if(host == null)
            host = "localhost";

        SQLDatabaseOptions options = new SQLDatabaseOptions();
        options.setDebug(true);

        DefaultSQLEndpoint endpoint = new DefaultSQLEndpoint(String.format("%s:3306", host), "test", "test", "test");

        connection = (builder = SQLConnectionBuilder.of(endpoint)
                .withDriver("com.mysql.cj.jdbc.Driver"))
                .build(options);

        System.out.println("Connection prepared, connecting...");

        assertEquals(endpoint.buildJdbc(), String.format("jdbc:mysql://%s:3306/test", host));
        assertTrue(connection.connect());
        assertTrue(connection.isConnected());

        System.out.println("Connection established, preparing tables...");

        assertNull(connection.exec(() -> "CREATE TABLE IF NOT EXISTS users (nickname VARCHAR(64) PRIMARY KEY NOT NULL, points INT NOT NULL);").getRejectMessage());
        assertNull(connection.exec(() -> "TRUNCATE TABLE users;").getRejectMessage());

        System.out.println("Tables prepared, test cases ready");
    }

    @Timeout(10)
    @Test
    public void test1_Upsert() {
        System.out.println("Testing upsert (save)...");
        assertTrue(connection.save(TABLE_NAME, user1).isSuccessful());
        System.out.println("Save successful");
        System.out.println("Testing upsert...");
        assertTrue(connection.upsert()
                .into(TABLE_NAME, "nickname", "points")
                .values(user2.getNickname(), user2.getPoints())
                .onDuplicateKey()
                .and("nickname", user2.getNickname())
                .and("points", user2.getPoints())
                .execute().isSuccessful());
        System.out.println("Upsert successful");
    }

    @Timeout(10)
    @Test
    public void test2_Select() {
        System.out.println("Testing select...");
        QueryRowsResult<User> result = connection.query(Select.of().from(TABLE_NAME)
                .where()
                .isEqual("nickname", "User1"), User.class);

        assertTrue(connection.select().from(TABLE_NAME).where().isEqual("nickname", "User1").obtainOne(User.class).isPresent());
        assertNull(result.getRejectMessage());
        assertEquals(1, result.size());
        assertEquals(user1.getNickname(), result.get(0).getNickname());
        System.out.println("Select successful");
    }

    @Timeout(10)
    @Test
    public void test3_Update() {
        System.out.println("Testing update...");
        assertNull(connection.update()
                .table(TABLE_NAME)
                .set("points", 300)
                .where()
                .isEqual("nickname", user1.getNickname())
                .execute().getRejectMessage());
        Optional<Row> rowOptional = connection.select("points")
                .from(TABLE_NAME)
                .where()
                .isEqual("nickname", user1.getNickname())
                .obtainOne();

        assertTrue(rowOptional.isPresent());
        assertEquals(300, rowOptional.get().get("points"));
    }

    @Timeout(10)
    @Test
    public void test4_Security() {
        // SQL Injection check
        Optional<Row> rowOptional = connection.select()
                .from(TABLE_NAME)
                .where()
                .isEqual("nickname", "asdfmnaskfopdmko' or '1' = '1").obtainOne();

        assertFalse(rowOptional.isPresent());

        // Check for table
        test2_Select();
    }

    @Timeout(10)
    @Test
    public void test5_Delete() {
        QueryResult result = connection.delete()
                .from(TABLE_NAME)
                .where()
                .isEqual("nickname", "User1").execute();

        assertNull(result.getRejectMessage());
    }

    @Timeout(10)
    @Test
    public void test6_Pool() {
        SQLConnectionPool.Options options = new SQLConnectionPool.Options();
        options.setMaxConnections(10);
        options.setBorrowObjectTimeout(5000L);
        options.setBlockWhenExhausted(false);
        SQLConnectionPool pool = new SQLConnectionPool(builder, options);
        try (SQLConnectionPool.Resource resource = pool.getResource()) {
            System.out.println("Got connection from pool");
            SQLDatabaseConnection connection = resource.getConnection();
            assertTrue(connection.save(TABLE_NAME, user1).isSuccessful());
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
        assertEquals(1, pool.size());
        pool.close();
        assertEquals(0, pool.size());
    }

    @Timeout(5)
    @Test
    public void test7_Close() {
        System.out.println("Closing connection...");
        connection.disconnect();
        System.out.println("Connection closed");
    }

    @AllArgsConstructor
    private static class User {
        private final String nickname;
        private final int points;

        public String getNickname() {
            return nickname;
        }

        public int getPoints() {
            return points;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            User user = (User) o;

            if (points != user.points) return false;
            return nickname.equals(user.nickname);
        }

        @Override
        public int hashCode() {
            int result = nickname.hashCode();
            result = 31 * result + points;
            return result;
        }
    }

}
