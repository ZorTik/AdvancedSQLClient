package me.zort.sqllib.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.zort.sqllib.api.options.NamingStrategy;
import me.zort.sqllib.naming.SnakeCaseNamingStrategy;

public final class Defaults {

  public static final String DEFAULT_DRIVER = "com.mysql.jdbc.Driver";
  public static final boolean DEFAULT_AUTO_RECONNECT = true;
  public static final boolean DEFAULT_DEBUG = false;
  public static final boolean DEFAULT_LOG_SQL_ERRORS = true;
  public static final NamingStrategy DEFAULT_NAMING_STRATEGY = new SnakeCaseNamingStrategy('_');

  public static final Gson DEFAULT_GSON = new GsonBuilder()
          .enableComplexMapKeySerialization()
          .create();

}
