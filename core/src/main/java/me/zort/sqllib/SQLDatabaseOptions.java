package me.zort.sqllib;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.zort.sqllib.api.options.NamingStrategy;
import me.zort.sqllib.internal.Defaults;
import me.zort.sqllib.internal.impl.DefaultNamingStrategy;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
public final class SQLDatabaseOptions {

    private boolean autoReconnect = true;
    private boolean debug = false;
    private boolean logSqlErrors = true;
    private transient NamingStrategy namingStrategy = new DefaultNamingStrategy();
    private transient Gson gson = Defaults.DEFAULT_GSON;

    /**
     * Loads options from a connection.
     *
     * @param connection The connection to load options from.
     */
    public void load(final @NotNull SQLDatabaseConnectionImpl connection) {
        SQLDatabaseOptions options = connection.getOptions();
        this.autoReconnect = options.autoReconnect;
        this.debug = options.debug;
        this.logSqlErrors = options.logSqlErrors;
        this.namingStrategy = options.namingStrategy;
        this.gson = options.gson;
    }

}
