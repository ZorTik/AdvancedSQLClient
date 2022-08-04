package me.zort.sqllib;

import me.zort.sqllib.api.data.QueryRowsResult;
import me.zort.sqllib.api.data.Row;

public class Test {

    public void test() {
        SQLDatabaseConnectionImpl connection = SQLConnectionBuilder.of("", "", "")
                .withDriver("com.mysql.jdbc.Driver")
                .withParam("useSSL", "false")
                .build();
        if(!connection.connect()) {

        }
        QueryRowsResult<Row> result = connection.select("nickname, coins")
                .from("player")
                .where().isEqual("nickname", "ZorTik")
                .obtainAll();
        boolean successful = result.isSuccessful();

    }

}
