package me.zort.sqllib.internal.impl;

import me.zort.sqllib.api.options.NamingStrategy;

public class DefaultNamingStrategy implements NamingStrategy {

    @Override
    public String convert(String str) {
        if(str.isEmpty()) return "";
        str = str.toLowerCase();
        String[] words = str.split(" ");
        if(words.length > 1) {
            for(int i = 1; i < words.length; i++) {
                String w = words[i];
                if(w.length() > 1) {
                    w = w.substring(0, 1).toUpperCase() + w.substring(1);
                } else w = w.toUpperCase();
                words[i] = w;
            }
        }
        return String.join("", words);
    }

}
