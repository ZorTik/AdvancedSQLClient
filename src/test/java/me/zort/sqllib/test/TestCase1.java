package me.zort.sqllib.test;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import me.zort.sqllib.SQLConnectionBuilder;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.internal.annotation.NullableField;
import me.zort.sqllib.internal.annotation.PrimaryKey;
import me.zort.sqllib.internal.query.QueryDetails;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.pool.SQLConnectionPool;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.SQLDatabaseOptions;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.provider.Select;
import me.zort.sqllib.internal.impl.DefaultSQLEndpoint;
import me.zort.sqllib.transaction.TransactionFlow;
import me.zort.sqllib.util.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.sql.ResultSet;
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
  private static final String table = "users";
  private final User user1 = new User("User1", 100);
  private final User user2 = new User("User2", 200);

  @BeforeAll
  public void prepareLogging() {
    Configurator.setAllLevels("", Level.ALL);
  }

  @BeforeAll
  public void prepare() {
    System.out.println("Preparing test case...");

    String host = System.getenv("D_MYSQL_HOST");

    if (host == null)
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

    assertTrue(connection.buildEntitySchema("users", User.class));
    //assertNull(connection.exec(() -> "CREATE TABLE IF NOT EXISTS users (nickname VARCHAR(64) PRIMARY KEY NOT NULL, points INT NOT NULL);").getRejectMessage());
    assertNull(connection.exec(() -> "TRUNCATE TABLE users;").getRejectMessage());

    System.out.println("Tables prepared, test cases ready");
  }

  @Test
  public void test1_Upsert() {
    System.out.println("Testing upsert (save)...");
    assertTrue(connection.save(table, user1).execute().isSuccessful());
    System.out.println("Save successful");
    System.out.println("Testing upsert...");
    assertTrue(connection.upsert()
            .into(table, "nickname", "points")
            .values(user2.getNickname(), user2.getPoints())
            .onDuplicateKey()
            .and("nickname", user2.getNickname())
            .and("points", user2.getPoints())
            .execute().isSuccessful());
    System.out.println("Upsert successful");
  }

  @Test
  public void test2_Select() {
    System.out.println("Testing select...");
    QueryRowsResult<User> result = connection.query(Select.of().from(table)
            .where()
            .isEqual("nickname", "User1"), User.class);

    assertTrue(connection.select().from(table).where().isEqual("nickname", "User1").obtainOne(User.class).isPresent());
    assertNull(result.getRejectMessage());
    assertEquals(1, result.size());
    assertEquals(user1.getNickname(), result.get(0).getNickname());
    System.out.println("Select successful");
  }

  @Test
  public void test3_Update() {
    System.out.println("Testing update...");
    assertNull(connection.update()
            .table(table)
            .set("points", 300)
            .where()
            .isEqual("nickname", user1.getNickname())
            .execute().getRejectMessage());
    Optional<Row> rowOptional = connection.select("points")
            .from(table)
            .where()
            .isEqual("nickname", user1.getNickname())
            .obtainOne();

    assertTrue(rowOptional.isPresent());
    assertEquals(300, rowOptional.get().get("points"));
  }

  @Test
  public void test4_Security() {
    // SQL Injection check
    Optional<Row> rowOptional = connection.select()
            .from(table)
            .where()
            .isEqual("nickname", "asdfmnaskfopdmko' or '1' = '1").obtainOne();

    assertFalse(rowOptional.isPresent());

    // Check for table
    test2_Select();
  }

  @Test
  public void test5_Delete() {
    QueryResult result = connection.delete()
            .from(table)
            .where()
            .isEqual("nickname", "User1").execute();

    assertNull(result.getRejectMessage());
  }

  @Test
  public void test6_Pool() {
    SQLConnectionPool.Options options = new SQLConnectionPool.Options();
    options.setMaxConnections(10);
    options.setBorrowObjectTimeout(5000L);
    options.setBlockWhenExhausted(false);
    SQLConnectionPool pool = new SQLConnectionPool(builder, options);
    try (SQLDatabaseConnection connection = pool.getResource()) {
      System.out.println("Got connection from pool");
      assertTrue(connection.save(table, user1).execute().isSuccessful());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    assertEquals(1, pool.size());
    try (SQLDatabaseConnection connection = pool.getResource()) {
      connection.exec(() -> "xcbnxcvkjonmcvxikjno");
    } catch (SQLException e) {
      e.printStackTrace();
    }
    assertEquals(1, pool.errorCount());
    assertEquals(0, pool.size());
    try {
      pool.getResource().close();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    assertEquals(1, pool.size());
    pool.close();
    assertEquals(0, pool.size());
  }

  @Test
  public void test6_Transactions() {
    TransactionFlow.Result result1 = connection.beginTransaction()
            .flow()
            .step(connection.save(table, user1))
            .step(connection.select()
                    .from(table)
                    .where().isEqual("nickname", user1.getNickname()))
            .create()
            .execute();
    assertTrue(result1.isSuccessful());
    assertEquals(2, result1.size());
    assertTrue(result1.get(1) instanceof QueryRowsResult);
    assertEquals(1, ((QueryRowsResult<?>) result1.get(1)).size());
  }

  @Test
  public void test7_RawNode() {
    String raw = "SELECT * FROM " + table +  " WHERE nickname = ?";
    QueryNode<?> query = QueryNode.fromRawQuery(raw, "User1");
    Pair<String, Object[]> preparedQuery = query.toPreparedQuery();
    assertEquals(raw, preparedQuery.getFirst());
    assertArrayEquals(new Object[]{"User1"}, preparedQuery.getSecond());
  }

  @Test
  public void test8_RawQuery() {
    Query query = QueryNode.fromRawQuery("SELECT * FROM " + table + " WHERE nickname = ?", "User1");
      try {
          ResultSet result = connection.queryRaw(query);
          assertNotNull(result);
          assertTrue(result.next());
            assertEquals("User1", result.getString("nickname"));
      } catch (SQLException e) {
          fail(e);
      }
  }

  @Test
  public void test9_MultiplePk() {
    assertTrue(connection.buildEntitySchema("multiple_pk", MultiplePKModel.class));
  }

  @AfterAll
  public void close() {
    System.out.println("Closing connection...");
    connection.disconnect();
    System.out.println("Connection closed");
  }

  @AllArgsConstructor
  private static class User {
    @PrimaryKey
    @NullableField(nullable = false)
    private final String nickname;
    @NullableField(nullable = false)
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

  @AllArgsConstructor
  private static class MultiplePKModel {
    @PrimaryKey
    private final String nickname;
    @PrimaryKey
    private final String email;
    private final int points;
  }

}
