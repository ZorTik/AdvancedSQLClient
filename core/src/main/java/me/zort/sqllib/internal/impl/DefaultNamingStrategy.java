package me.zort.sqllib.internal.impl;

import me.zort.sqllib.api.options.NamingStrategy;

public class DefaultNamingStrategy implements NamingStrategy {

    @Override
    public String fieldNameToColumn(String str) {
        if(str.isEmpty()) return "";

        char[] chars = str.toCharArray();
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for(char c : chars) {
            if(Character.isUpperCase(c)
                    && index > 0
                    && !Character.isUpperCase(chars[index - 1])
                    && (index == chars.length - 1 || !Character.isUpperCase(chars[index + 1]))
            ) {
                sb.append('_');
            }

            sb.append(Character.toLowerCase(c));
            index++;
        }
        return sb.toString();
    }

}
