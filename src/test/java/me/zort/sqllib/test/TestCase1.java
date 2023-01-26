package me.zort.sqllib.test;

import me.zort.sqllib.SQLConnectionBuilder;
import me.zort.sqllib.SQLDatabaseOptions;
import me.zort.sqllib.api.SQLDatabaseConnection;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.provider.Select;
import me.zort.sqllib.internal.impl.DefaultSQLEndpoint;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.LINUX) // TODO: Fix tests, endless run
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCase1 {

    private SQLDatabaseConnection connection;
    private final User user1 = new User("User1", 100);
    private final User user2 = new User("User2", 200);

    @Timeout(15)
    @BeforeAll
    public void prepare() {
        String host = System.getenv("D_MYSQL_HOST");

        if(host == null)
            host = "localhost";

        SQLDatabaseOptions options = new SQLDatabaseOptions();
        options.setDebug(true);

        DefaultSQLEndpoint endpoint = new DefaultSQLEndpoint(String.format("%s:3306", host), "test", "root", "test");

        connection = SQLConnectionBuilder.of(endpoint)
                .withDriver("com.mysql.cj.jdbc.Driver")
                .build(options);

        assertEquals(endpoint.buildJdbc(), String.format("jdbc:mysql://%s:3306/test", host));
        assertTrue(connection.connect());
        assertTrue(connection.isConnected());

        System.out.println("Connection established");

        assertNull(connection.exec(() -> "CREATE TABLE IF NOT EXISTS users (nickname VARCHAR(16) PRIMARY KEY NOT NULL, points INT NOT NULL);").getRejectMessage());
    }

    @Timeout(5)
    @AfterAll
    public void close() {
        connection.disconnect();
    }

    @Timeout(10)
    @Test
    public void testUpsert() {
        assertTrue(connection.save("users", user1).isSuccessful());
        assertTrue(connection.upsert()
                .into("users", "nickname", "points")
                .values(user2.getNickname(), user2.getPoints())
                .onDuplicateKey()
                .and("nickname", user2.getNickname())
                .and("points", user2.getPoints())
                .execute().isSuccessful());
    }

    @Timeout(10)
    @Test
    public void testSelect() {
        QueryRowsResult<User> result = connection.query(Select.of().from("users")
                .where()
                .isEqual("nickname", "User1"), User.class);

        assertTrue(result.isSuccessful());
        assertEquals(1, result.size());
        assertEquals(user1, result.get(0));
    }

    private static class User {
        private final String nickname;
        private final int points;

        public User(String nickname, int points) {
            this.nickname = nickname;
            this.points = points;
        }

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
