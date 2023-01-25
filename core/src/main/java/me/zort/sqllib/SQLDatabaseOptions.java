package me.zort.sqllib;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import me.zort.sqllib.api.options.NamingStrategy;
import me.zort.sqllib.internal.Defaults;
import me.zort.sqllib.internal.impl.DefaultNamingStrategy;

@AllArgsConstructor
@Data
public class SQLDatabaseOptions {

    private boolean autoReconnect;
    private boolean debug;
    private boolean logSqlErrors;
    private NamingStrategy namingStrategy;
    private Gson gson;

}
