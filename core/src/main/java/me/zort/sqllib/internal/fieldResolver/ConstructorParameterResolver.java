package me.zort.sqllib.internal.fieldResolver;

import me.zort.sqllib.SQLDatabaseConnectionImpl;
import me.zort.sqllib.api.ObjectMapper;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.util.Validator;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApiStatus.AvailableSince("0.5.1")
public class ConstructorParameterResolver implements ObjectMapper.FieldValueResolver {

    private static final Pattern argumentPattern = Pattern.compile("(arg)(\\d+)");

    @Override
    public Object obtainValue(SQLConnection connection,
                              AnnotatedElement element,
                              Row row,
                              String fieldName,
                              String convertedName,
                              Type type) {
        if (!(element instanceof Parameter) || !(((Parameter) element).getDeclaringExecutable() instanceof Constructor))
            return null;

        Parameter p = (Parameter) element;
        Matcher matcher = argumentPattern.matcher(p.getName());

        if (matcher.matches()) {
            try {
                Field[] fields = ((Constructor) p.getDeclaringExecutable()).getDeclaringClass().getDeclaredFields();
                int index = Integer.parseInt(matcher.group(2));
                if (index >= fields.length)
                    return null;

                int i = -1;
                for (Field field : fields) {
                    if (Validator.validateAssignableField(field))
                        i++;

                    if (i == index) {
                        fieldName = field.getName();
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        return row.get(fieldName);
    }
}
