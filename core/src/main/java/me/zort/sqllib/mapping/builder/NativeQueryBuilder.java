package me.zort.sqllib.mapping.builder;

import lombok.Getter;
import me.zort.sqllib.api.Executive;
import me.zort.sqllib.api.SQLConnection;
import me.zort.sqllib.internal.query.QueryDetails;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.internal.query.QueryPriority;
import me.zort.sqllib.internal.query.ResultSetAware;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class NativeQueryBuilder implements QueryAnnotation.QueryBuilder<Annotation> {
    @Override
    public QueryNode<?> build(QueryAnnotation.DefaultMappingDetails details, Annotation queryAnnotation, Method method, ParameterPair[] parameters) {
        SQLConnection connection = details.getConnection();
        PlaceholderMapper mapper = new PlaceholderMapper(parameters);
        AtomicInteger currPhIndex = new AtomicInteger(0);
        Supplier<String> nextPlaceholder = () -> "nq_" + currPhIndex.getAndIncrement();
        return buildNodeImpl(queryAnnotation, connection, () -> {
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

                String placeholder = nextPlaceholder.get();
                annotValue = annotValue.replaceFirst("\\?", "<" + placeholder + ">");
                params.put(placeholder, param);
            }
            return new QueryDetails(annotValue, params);
        });
    }

    private static QueryNode<?> buildNodeImpl(Annotation queryAnnotation, SQLConnection connection, Supplier<QueryDetails> detailsSupplier) {
        if (queryAnnotation instanceof Exec) return new NodeExecutiveImpl(connection) {
            @Override
            public QueryDetails buildQueryDetails() {return detailsSupplier.get();}
        };
        else if (queryAnnotation instanceof Query) return new NodeExecutiveImplRS(connection) {
            @Override
            public QueryDetails buildQueryDetails() {return detailsSupplier.get();}
        };
        throw new IllegalArgumentException();
    }

    private static abstract class NodeExecutiveImpl extends QueryNode<QueryNode<?>> implements Executive {
        @Getter
        private final SQLConnection connection;

        public NodeExecutiveImpl(SQLConnection connection) {
            super(null, new ArrayList<>(), QueryPriority.GENERAL);
            this.connection = connection;
        }
    }

    private static abstract class NodeExecutiveImplRS extends NodeExecutiveImpl implements ResultSetAware {
        public NodeExecutiveImplRS(SQLConnection connection) {
            super(connection);
        }
    }

}
