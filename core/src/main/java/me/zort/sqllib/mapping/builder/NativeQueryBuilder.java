package me.zort.sqllib.mapping.builder;

import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.internal.query.QueryDetails;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.internal.query.QueryPriority;
import me.zort.sqllib.mapping.PlaceholderMapper;
import me.zort.sqllib.mapping.QueryAnnotation;
import me.zort.sqllib.mapping.annotation.Exec;
import me.zort.sqllib.mapping.annotation.Query;
import me.zort.sqllib.util.ParameterPair;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NativeQueryBuilder implements QueryAnnotation.QueryBuilder<Annotation> {
    @Override
    public QueryNode<?> build(SQLConnection connection, Annotation queryAnnotation, Method method, ParameterPair[] parameters) {
        PlaceholderMapper mapper = new PlaceholderMapper(parameters);
        return new QueryNode<QueryNode<?>>(null, new ArrayList<>(), QueryPriority.GENERAL) {
            private int currPhIndex = 0;
            @Override
            public QueryDetails buildQueryDetails() {
                String annotValue;
                String[] annotParams;

                if (queryAnnotation instanceof Query) {
                    annotValue = ((Query) queryAnnotation).value();
                    annotParams = ((Query) queryAnnotation).params();
                } else if(queryAnnotation instanceof Exec) {
                    annotValue = ((Exec) queryAnnotation).value();
                    annotParams = ((Exec) queryAnnotation).params();
                } else {
                    throw new IllegalArgumentException(String.format("NativeQueryBuilder does not support %s annotation!", queryAnnotation.getClass().getName()));
                }

                annotValue = mapper.assignValues(annotValue);

                Map<String, Object> params = new HashMap<>();
                for (String param : annotParams) {
                    param = mapper.assignValues(param);
                    int index = annotValue.indexOf("?");
                    if (index == -1)
                        break;

                    String placeholder = nextPlaceholder();
                    annotValue = annotValue.replaceFirst("\\?", "<" + placeholder + ">");
                    params.put(placeholder, param);
                }
                return new QueryDetails(annotValue, params);
            }

            private String nextPlaceholder() {
                return "nq_" + currPhIndex++;
            }
        };
    }

}
