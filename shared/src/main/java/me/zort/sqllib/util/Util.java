package me.zort.sqllib.util;

public final class Util {

    public static String buildQuoted(Object obj) {
        obj = obj instanceof String
                ? String.format("'%s'", obj)
                : String.valueOf(obj);
        return (String) obj;
    }

    public static int count(String str, String substr) {
        String copy = str;
        int count = 0;

        while (copy.contains(substr)) {
            copy = copy.replaceFirst(str, substr);
            count++;
        }

        return count;
    }

}
