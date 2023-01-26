package me.zort.sqllib.util;

public final class Util {

    public static String buildQuoted(Object obj) {
        obj = obj instanceof String
                //? String.format("'%s'", obj) // No longer needed, because I use prepared statements.
                ? String.format("%s", obj)
                : String.valueOf(obj);
        return (String) obj;
    }

    public static int count(String str, String substr) {
        String copy = str;
        int count = 0;

        while (copy.contains(substr)) {
            copy = copy.replaceFirst(substr, "");
            count++;
        }

        return count;
    }

}
