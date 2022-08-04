package me.zort.sqllib;

import com.google.gson.Gson;
import lombok.Data;
import me.zort.sqllib.api.options.NamingStrategy;
import me.zort.sqllib.internal.Defaults;
import me.zort.sqllib.internal.impl.DefaultNamingStrategy;

@Data
public class SQLDatabaseOptions {

    private boolean autoReconnect;
    private boolean debug;
    private NamingStrategy namingStrategy;
    private Gson gson;

    public SQLDatabaseOptions() {
        this.autoReconnect = true;
        this.debug = false;
        this.namingStrategy = new DefaultNamingStrategy();
        this.gson = Defaults.DEFAULT_GSON;
    }

}
