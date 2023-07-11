package me.zort.sqllib;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.zort.sqllib.api.ISQLDatabaseOptions;
import me.zort.sqllib.api.options.NamingStrategy;
import me.zort.sqllib.internal.Defaults;
import me.zort.sqllib.naming.SymbolSeparatedNamingStrategy;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
public final class SQLDatabaseOptions implements ISQLDatabaseOptions {

  private boolean autoReconnect = true;
  private boolean debug = false;
  private boolean logSqlErrors = true;
  private transient NamingStrategy namingStrategy = SQLDatabaseConnectionImpl.DEFAULT_NAMING_STRATEGY;
  private transient Gson gson = Defaults.DEFAULT_GSON;

  /**
   * Loads options from a connection.
   *
   * @param connection The connection to load options from.
   */
  @SuppressWarnings("unused")
  public void load(final @NotNull SQLDatabaseConnectionImpl connection) {
    ISQLDatabaseOptions options = connection.getOptions();
    this.autoReconnect = options.isAutoReconnect();
    this.debug = options.isDebug();
    this.logSqlErrors = options.isLogSqlErrors();
    this.namingStrategy = options.getNamingStrategy();
    this.gson = options.getGson();
  }

}
