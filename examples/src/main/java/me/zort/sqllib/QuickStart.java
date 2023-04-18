package me.zort.sqllib;

import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.internal.annotation.PrimaryKey;
import me.zort.sqllib.mapping.annotation.*;

import java.util.List;
import java.util.Optional;

public class QuickStart {

    private SQLDatabaseConnection connection;

    public void quickStart() {

        String address = "localhost";
        int port = 3306;
        String database = "database";
        String username = "username";
        String password = "password";

        connection = new SQLConnectionBuilder(address, port, database, username, password).build();

        if (!connection.connect()) {
            System.out.println("Failed to connect to the database!");
            System.exit(1);
        }

        QueryResult result = connection.exec(() ->
                "CREATE TABLE IF NOT EXISTS users(" +
                "firstname VARCHAR(32) PRIMARY KEY NOT NULL," +
                "lastname VARCHAR(32) NOT NULL);");

        if (!result.isSuccessful()) {
            System.out.println("Failed to create the table!");
            System.out.println(result.getRejectMessage());
            System.exit(1);
        }

        QueryResult result2 = connection.insert()
                .into("users", "firstname", "lastname")
                .values("John", "Doe")
                .execute();

        Optional<Row> result3 = connection.select()
                .from("users")
                .where().isEqual("firstname", "John")
                .obtainOne();

        if (!result3.isPresent()) {
            System.out.println("Where did John go?");
            System.exit(1);
        }

        connection.select();
        connection.update();
        connection.delete();
        connection.upsert();

        connection.disconnect();
    }

    static class User {
        @PrimaryKey
        private String firstname;
        private String lastname;

        public User(String firstname, String lastname) {
            this.firstname = firstname;
            this.lastname = lastname;
        }
    }

    public void saveUser() {
        User user = new User("John", "Doe");
        QueryResult result = connection.save("users", user);
    }

    public void loadUser() {
        User user = connection.select()
                .from("users")
                .where().isEqual("firstname", "John")
                .obtainOne(User.class)
                .orElse(null);
    }


    @Table("users")
    interface UsersGate {

        @Save
        QueryResult saveUser(User user);

        @Select
        @Where(@Where.Condition(column = "firstname", value = "{First Name}"))
        Optional<User> getUser(@Placeholder("First Name") String firstName);

        @Select
        @Limit(10)
        List<User> selectFirstTen();

        @Delete
        void deleteAll();
    }

    public void gateExample() {
        UsersGate gate = connection.createGate(UsersGate.class);

        QueryResult result = gate.saveUser(new User("John", "Doe"));

        User user = gate.getUser("John").orElse(null);
    }

}
