package me.zort.sqllib;

import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.provider.Select;
import me.zort.sqllib.internal.annotation.JsonField;

import java.util.List;

public class Example {

    public void example() {
        SQLDatabaseConnection connection = SQLConnectionBuilder.of(
                "localhost",
                        3306,
                        "database",
                        "user",
                        "password")
                .withDriver("com.mysql.jdbc.Driver")
                .withParam("useSSL", "false")
                .build();
        if(connection.connect()) {
            QueryRowsResult<Row> result = connection.select("nickname, coins")
                    .from("players")
                    .where().isEqual("nickname", "ZorTik")
                    .or().in("nickname", "Player1", "Player2", "Player3")
                    .obtainAll();
            if(!result.isSuccessful()) {
                // No rows found
            }

            QueryRowsResult<Player> players = connection.query(Select.of().from("players"), Player.class);
            // Library can parse from JSON!

            connection.update("players")
                    .set("nickname", "ZorTicek")
                    .where().isEqual("nickname", "ZorTik")
                    .execute();
            connection.upsert()
                    .into("players", "nickname", "coins")
                    .values("ZorTik", "0")
                    .onDuplicateKey("coins", "500")
                    .execute();
        }
    }

    public static class Player {
        private String nickname;
        private int coins;

        @JsonField // Library can use Gson to convert the highest fields from columns
        private Inventory inventory;
    }

    public static class Inventory {
        private List<Item> items;
    }

    public static class Item {
        private String name;
        private String type;
    }

}
