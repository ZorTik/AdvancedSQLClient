package me.zort.sqllib;

import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.provider.Select;

public class Example {

    public void example() {
        SQLDatabaseConnectionImpl connection = SQLConnectionBuilder.of("", "", "")
                .withDriver("com.mysql.jdbc.Driver")
                .withParam("useSSL", "false")
                .build();
        if(connection.connect()) {
            QueryRowsResult<Row> result = connection.select("nickname, coins")
                    .from("players")
                    .where().isEqual("nickname", "ZorTik")
                    .obtainAll();
            if(!result.isSuccessful()) {
                // No rows found
            }
            QueryRowsResult<Player> players = connection.query(Select.of().from("players"), Player.class);
        }
    }

    public static class Player {
        private String nickname;
        private int coins;
    }

}
