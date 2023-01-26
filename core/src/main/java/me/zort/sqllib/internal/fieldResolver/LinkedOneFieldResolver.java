package me.zort.sqllib.internal.fieldResolver;

import me.zort.sqllib.ObjectMapper;
import me.zort.sqllib.SQLDatabaseConnectionImpl;
import me.zort.sqllib.api.data.Row;
import me.zort.sqllib.api.provider.Select;
import me.zort.sqllib.internal.annotation.LinkedOne;
import me.zort.sqllib.internal.annotation.PrimaryKey;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

/**
 * Functionality class for {@link LinkedOne} annotation.
 * @see LinkedOne
 * @author ZorTik
 */
public class LinkedOneFieldResolver implements ObjectMapper.FieldValueResolver {

    @Override
    public Object obtainValue(SQLDatabaseConnectionImpl connection,
                              AnnotatedElement element,
                              Row row,
                              String fieldName,
                              String convertedName,
                              Type type) {
        if(!element.isAnnotationPresent(LinkedOne.class)) {
            // This makes mapping function hop to the next resolver.
            return null;
        }
        Class<?> targetClass;
        if(element instanceof Field) {
            targetClass = ((Field) element).getType();
        } else {
            targetClass = ((Parameter) element).getType();
        }
        Field targetIdField = null;
        for(Field field : targetClass.getDeclaredFields()) {
            if(field.isAnnotationPresent(PrimaryKey.class)) {
                targetIdField = field;
                break;
            }
        }
        if(targetIdField == null) {
            // Target type has no primary key annotated!
            connection.debug(String.format("No primary key field set for target in @LinkedOne field %s.", fieldName));
            return null;
        }

        LinkedOne linkedOne = element.getAnnotation(LinkedOne.class);
        Object idObject = row.get(fieldName);
        if(idObject == null && (idObject = row.get(convertedName)) == null) {
            // No local column found with that name.
            connection.debug(String.format("No local column found for @LinkedOne field %s.", fieldName));
            return null;
        }
        return connection.query(Select.of("*")
                                .from(linkedOne.targetTable())
                                .where().isEqual(
                                        connection.getOptions().getNamingStrategy().fieldNameToColumn(targetIdField.getName()),
                                        idObject),
                        targetClass)
                .stream()
                .findFirst().orElse(null);
    }

}
