package me.zort.sqllib.mapping.annotation;

import me.zort.sqllib.mapping.PlaceholderMapper;
import me.zort.sqllib.mapping.QueryAnnotation;
import me.zort.sqllib.mapping.exception.SQLMappingException;
import me.zort.sqllib.util.ParameterPair;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Table {
  String value();

  class Util {
    @Nullable
    public static String getFromContext(AnnotatedElement element, @Nullable ParameterPair[] parameters) {
      PlaceholderMapper mapper = new PlaceholderMapper(parameters != null ? parameters : new ParameterPair[0]);
      if (element.isAnnotationPresent(Table.class)) {
        return mapper.assignValues(element.getAnnotation(Table.class).value());
      } else if (!(element instanceof Method)) {
        throw new SQLMappingException("Element " + element.toString() + " is not suitable for @Table check!", null, null);
      }
      Method method = (Method) element;
      if (method.getDeclaringClass().isAnnotationPresent(Table.class)) {
        return mapper.assignValues(method.getDeclaringClass().getAnnotation(Table.class).value());
      } else {
        throw new SQLMappingException("Method " + method.getName() + " in class " + method.getDeclaringClass().getSimpleName() + " requires @Table annotation", method, null);
      }
    }
  }
}
