package me.zort.sqllib.test;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import me.zort.sqllib.SQLConnectionBuilder;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.SQLDatabaseOptions;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.mapping.annotation.Delete;
import me.zort.sqllib.mapping.annotation.Limit;
import me.zort.sqllib.mapping.annotation.Select;
import me.zort.sqllib.mapping.annotation.Table;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@EnabledOnOs(value = {OS.LINUX, OS.WINDOWS})
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCase2 { // Experimental features

    private SQLDatabaseConnection connection;

    @BeforeAll
    public void prepare() {
        String host = System.getenv("D_MYSQL_HOST");

        if(host == null)
            host = "localhost";

        SQLDatabaseOptions options = new SQLDatabaseOptions();
        options.setDebug(true);

        connection = new SQLConnectionBuilder(host, 3306, "test", "test", "test")
                .withDriver("com.mysql.cj.jdbc.Driver")
                .build(options);

        assertTrue(connection.connect());
        assertTrue(connection.isConnected());

        assertNull(connection.exec(() -> "CREATE TABLE IF NOT EXISTS users (nickname VARCHAR(64) PRIMARY KEY NOT NULL, points INT NOT NULL);").getRejectMessage());
        assertNull(connection.exec(() -> "TRUNCATE TABLE users;").getRejectMessage());
    }

    @Timeout(10)
    @Test
    public void test1_Mapping() {
        DatabaseRepository repository = connection.createMapping(DatabaseRepository.class);
        // TODO: Insert
        assertTrue(repository.selectOne().isPresent());
        assertNull(repository.deleteAll().getRejectMessage());
    }

    @Timeout(5)
    @Test
    public void test2_Close() {
        connection.disconnect();
    }

    public interface DatabaseRepository {

        @Select
        @Table("users")
        @Limit(1)
        Optional<User> selectOne();

        @Delete
        @Table("users")
        QueryResult deleteAll();
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

            TestCase2.User user = (TestCase2.User) o;

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
