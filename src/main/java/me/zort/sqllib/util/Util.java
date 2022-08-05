package me.zort.sqllib.util;

public final class Util {

    public static String buildQuoted(Object obj) {
        obj = obj instanceof String
                ? String.format("'%s'", obj)
                : String.valueOf(obj);
        return (String) obj;
    }

}
