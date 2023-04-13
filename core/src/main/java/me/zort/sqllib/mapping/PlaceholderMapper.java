package me.zort.sqllib.mapping;

import lombok.RequiredArgsConstructor;
import me.zort.sqllib.mapping.annotation.Placeholder;
import me.zort.sqllib.util.ParameterPair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class PlaceholderMapper {

    private final ParameterPair[] parameters;
    private final Pattern pattern = Pattern.compile("\\{[a-zA-Z0-9]+}");

    public String assignValues(String input) {
        Matcher matcher = pattern.matcher(input);
        while(matcher.find()) {
            String placeholder = matcher.group();
            String placeholderName = placeholder.substring(1, placeholder.length() - 1);
            for (ParameterPair pair : parameters) {
                if (!pair.getParameter().isAnnotationPresent(Placeholder.class))
                    continue;

                if (placeholderName.equals(pair.getParameter().getAnnotation(Placeholder.class).value())) {
                    input = input.replace(placeholder, String.valueOf(pair.getValue()));
                }
            }
        }

        return input;
    }

}
