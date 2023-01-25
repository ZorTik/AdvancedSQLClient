package me.zort.sqllib.internal.query;

import me.zort.sqllib.internal.exception.IllegalStatementOperationException;
import me.zort.sqllib.internal.query.part.WhereStatement;

import java.util.ArrayList;

public interface Conditional<P extends QueryNode<?> & Conditional<P>> {

    default WhereStatement<P> where() {
        return where(QueryPriority.CONDITION.getPrior());
    }

    default WhereStatement<P> where(int priority) {
        if(!(this instanceof QueryNode)) {
            throw new IllegalStatementOperationException("This instance is not query part!");
        }
        WhereStatement<P> stmt = new WhereStatement<>((P) this, new ArrayList<>(), priority);
        ((QueryNode<?>) this).then(stmt);
        return stmt;
    }

}
