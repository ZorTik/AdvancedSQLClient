package me.zort.sqllib;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.zort.sqllib.api.options.NamingStrategy;
import me.zort.sqllib.internal.Defaults;
import me.zort.sqllib.internal.impl.DefaultNamingStrategy;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SQLDatabaseOptions {

    private boolean autoReconnect = true;
    private boolean debug = false;
    private boolean logSqlErrors = true;
    private transient NamingStrategy namingStrategy = new DefaultNamingStrategy();
    private transient Gson gson = Defaults.DEFAULT_GSON;

}
