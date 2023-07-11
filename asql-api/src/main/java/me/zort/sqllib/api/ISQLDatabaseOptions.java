package me.zort.sqllib.api;

import com.google.gson.Gson;
import me.zort.sqllib.api.options.NamingStrategy;

public interface ISQLDatabaseOptions {

  void setAutoReconnect(boolean autoReconnect);

  void setDebug(boolean debug);

  void setLogSqlErrors(boolean logSqlErrors);

  void setNamingStrategy(NamingStrategy namingStrategy);

  void setGson(Gson gson);

  boolean isAutoReconnect();

  boolean isDebug();

  boolean isLogSqlErrors();

  NamingStrategy getNamingStrategy();

  Gson getGson();

}
