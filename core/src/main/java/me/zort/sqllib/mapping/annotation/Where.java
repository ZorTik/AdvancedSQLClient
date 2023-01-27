package me.zort.sqllib.mapping.annotation;

import me.zort.sqllib.internal.query.Conditional;
import me.zort.sqllib.internal.query.part.WhereStatement;
import me.zort.sqllib.mapping.PlaceholderMapper;
import org.intellij.lang.annotations.Pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Where {

    Condition[] value(); // And

    @interface Condition {

        String column();
        String value();
        Type type() default Type.EQUALS;

        enum Type {
            EQUALS, BT, LT
        }
    }

    class Builder {
        public static WhereStatement<?> build(Conditional<?> parent, Where annotation, PlaceholderMapper mapper) {
            WhereStatement<?> where = parent.where();
            for (Condition condition : annotation.value()) {
                String value = mapper.assignValues(condition.value());

                switch (condition.type()) {
                    case EQUALS:
                        where.isEqual(condition.column(), value);
                        break;
                    case BT:
                    case LT:
                        try {
                            int number = Integer.parseInt(value);
                            if (condition.type().equals(Condition.Type.BT)) {
                                where.bt(condition.column(), number);
                            } else {
                                where.lt(condition.column(), number);
                            }
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Value of current @Where.Condition must be a number! (" + value + ")");
                        }
                        break;
                }
            }
            return where;
        }
    }
}
