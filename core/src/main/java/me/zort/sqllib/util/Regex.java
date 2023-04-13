package me.zort.sqllib.util;

import java.util.regex.PatternSyntaxException;

public class Regex {

    private Regex() {
    }

    public static String skipRegexCharacters(String input) {
        String s = input;
        StringBuilder stringBuilder = new StringBuilder(s);
        try {
            s.replaceAll(s, s);
            return input;
        } catch(PatternSyntaxException e) {
            stringBuilder.insert(e.getIndex() + 1, "\\");
            return stringBuilder.toString();
        }
    }

}
