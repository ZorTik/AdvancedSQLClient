package me.zort.sqllib.test;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import me.zort.sqllib.SQLConnectionBuilder;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.SQLDatabaseConnectionImpl;
import me.zort.sqllib.SQLDatabaseOptions;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.model.TableSchema;
import me.zort.sqllib.internal.annotation.Default;
import me.zort.sqllib.internal.annotation.PrimaryKey;
import me.zort.sqllib.mapping.annotation.*;
import me.zort.sqllib.model.schema.EntitySchemaBuilder;
import me.zort.sqllib.model.schema.SQLSchemaSynchronizer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@EnabledOnOs(value = {OS.LINUX, OS.WINDOWS})
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCase2 { // Experimental features

    private SQLDatabaseConnection connection;
    private SQLDatabaseConnection sqliteConnection;

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

        doPrepareConnection(connection);
    }

    @BeforeAll
    public void prepareSqlite() throws IOException {
        File file = new File(System.getProperty("user.dir") + "/test.db");
        file.delete();
        file.getParentFile().mkdirs();
        file.createNewFile();
        SQLDatabaseOptions options = new SQLDatabaseOptions();
        options.setDebug(true);
        sqliteConnection = SQLConnectionBuilder.ofSQLite(file.getAbsolutePath())
                .build(options);
        doPrepareConnection(sqliteConnection);
    }

    private static void doPrepareConnection(SQLDatabaseConnection connection) {
        assertTrue(connection.connect());
        assertTrue(connection.isConnected());

        assertNull(connection.exec(() -> "DROP TABLE IF EXISTS users;").getRejectMessage());
        assertTrue(connection.buildEntitySchema("users", User.class));
    }

    @Timeout(10)
    @Test
    public void test1_Mapping() {
        DatabaseRepository repository = connection.createProxy(DatabaseRepository.class);

        User user1 = new User("User1", 1000);

        assertNull(repository.save(user1).getRejectMessage());
        assertTrue(repository.selectOne("User1").isPresent());
        assertNull(repository.insertNew("User4", 800).getRejectMessage());
        assertTrue(repository.selectOne("User4").isPresent());
        assertFalse(repository.selectAll().isEmpty());
        assertNotEquals(0L, repository.count().get(0).get("COUNT(*)"));
        assertTrue(repository.saveUser(user1));

        assertNull(repository.deleteAll().getRejectMessage());
        assertTrue(repository.selectAll().isEmpty());
    }

    @Timeout(5)
    @Test
    public void test2_Synchronization() {
        doTestSynchronization(connection);
        doTestSynchronization(sqliteConnection);
    }

    private static void doTestSynchronization(SQLDatabaseConnection connection) {
        System.out.println(connection.getClass().getSimpleName() + ":");
        TableSchema schema = new EntitySchemaBuilder("users", User.class, ((SQLDatabaseConnectionImpl) connection).getOptions().getNamingStrategy(), false).buildTableSchema();
        assertEquals(2, schema.getDefinitions().length);
        assertEquals("nickname VARCHAR(255) PRIMARY KEY", schema.getDefinitions()[0]);
        assertEquals("points INTEGER", schema.getDefinitions()[1]);

        TableSchema dbSchema = connection.getSchemaBuilder("users").buildTableSchema();
        assertEquals(2, dbSchema.getDefinitions().length);
        assertEquals("nickname " + adjustColumnType(connection, "VARCHAR(255) PRIMARY KEY"), dbSchema.getDefinitions()[0]);
        assertEquals("points " + adjustColumnType(connection, "INTEGER"), dbSchema.getDefinitions()[1]);
        assertFalse(connection.synchronizeModel(schema, "users"));
        assertFalse(connection.synchronizeModel());

        assertTrue(connection.synchronizeModel(UserCopy.class, "users"));

        TableSchema copySchema = connection.getSchemaBuilder("users").buildTableSchema();
        assertEquals(2, copySchema.getDefinitions().length);
        assertEquals("nickname " + adjustColumnType(connection, "VARCHAR(255) PRIMARY KEY"), copySchema.getDefinitions()[0]);
        assertEquals("points " + adjustColumnType(connection, "INTEGER DEFAULT 0"), copySchema.getDefinitions()[1]);
    }

    private static String adjustColumnType(SQLDatabaseConnection connection, String type) {
        return ((SQLSchemaSynchronizer) connection.getSchemaSynchronizer()).getColumnTypeAdjuster().adjust(type);
    }

    @Timeout(5)
    @Test
    public void test3_Close() {
        connection.disconnect();
    }

    @Table("users")
    public interface DatabaseRepository {

        @Save // Upsert
        QueryResult save(User user);

        @Insert(cols = {"nickname", "points"}, vals = {"{name}", "{points}"})
        QueryResult insertNew(@Placeholder("name") String nickname, @Placeholder("points") int points);

        @Select
        @Where(value = {
                @Where.Condition(column = "nickname", value = "{name}"),
                @Where.Condition(column = "points", value = "500", type = Where.Condition.Type.BT)
        })
        @Limit(1)
        Optional<User> selectOne(@Placeholder("name") String nickname);

        @Select
        List<User> selectAll();

        @Delete
        QueryResult deleteAll();

        @Query("SELECT COUNT(*) FROM users;")
        QueryRowsResult<Row> count();

        default boolean saveUser(User user) {
            System.out.println("Saving user...");
            return save(user).isSuccessful();
        }
    }

    @AllArgsConstructor
    private static class User {
        @PrimaryKey
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

    @AllArgsConstructor
    private static class UserCopy {
        @PrimaryKey
        private final String nickname;
        @Default("0")
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

            TestCase2.UserCopy user = (TestCase2.UserCopy) o;

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
