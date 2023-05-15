package me.zort.sqllib.mapping;

import me.zort.sqllib.api.mapping.StatementMappingResultAdapter;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

public class DefaultResultAdapter implements StatementMappingResultAdapter {
    @Override
    public Object adaptResult(Method method, QueryResult result) {
        Class<?> returnType = method.getReturnType();
        if(returnType.equals(QueryResult.class)) {
            return result;
        } else if (isVoid(returnType) || !(result instanceof QueryRowsResult)) {
            return null;
        }
        QueryRowsResult<?> rows = (QueryRowsResult<?>) result;
        if (List.class.isAssignableFrom(returnType)) return rows;
        Object obj = rows.isEmpty() ? null : rows.get(0);
        if (Optional.class.isAssignableFrom(returnType)) return Optional.ofNullable(obj);
        return obj;
    }

    @Override
    public Class<?> retrieveResultType(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(Optional.class) || returnType.equals(List.class)) {
            return getGenericArgument(returnType, method.getGenericReturnType(), true);
        } else if (isVoid(returnType)) {
            return null;
        } else {
            return returnType;
        }
    }

    // List<T>
    private static Class<?> getGenericArgument(Class<?> clazz, Type genericType, boolean throwNotFound) {
        Type[] typeParameters = ((ParameterizedType) genericType).getActualTypeArguments();
        if (typeParameters.length < 1) {
            if (throwNotFound) {
                throw new IllegalArgumentException("The given class does not have a generic argument");
            } else {
                return clazz;
            } // For simplicity, return itself to be mapped.
        }

        try {
            return Class.forName(typeParameters[0].getTypeName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return clazz;
        }
    }

    private static boolean isVoid(Class<?> type) {
        return type.equals(void.class) || type.equals(Void.class);
    }
}
