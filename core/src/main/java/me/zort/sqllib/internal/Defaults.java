package me.zort.sqllib.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class Defaults {

    public static final Gson DEFAULT_GSON = new GsonBuilder()
            .enableComplexMapKeySerialization()
            .create();

}
