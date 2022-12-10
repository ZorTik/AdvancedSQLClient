package me.zort.sqllib.util;

public final class Encoding {

    /**
     * Handles string to be able to push into database.
     *
     * @param s String to be encoded.
     * @return Encoded string.
     */
    public static String handleTo(String s) {
        return s.replaceAll("'", "''");
    }

}
