package me.zort.sqllib.transaction.step;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.api.Query;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.transaction.FlowStep;

@RequiredArgsConstructor
public class FlowStepImpl implements FlowStep {
    private final Query query;
    @Getter
    private final boolean optional;
    @Getter
    private QueryResult result = null;

    @Override
    public Status execute(SQLDatabaseConnection connection) {
        if (!(query instanceof QueryNode)) throw new IllegalStateException("FlowStepImpl accepts only QueryNode!");

        QueryNode<?> node = (QueryNode<?>) query;
        node = node.getAncestor();
        QueryResult localResult = node.generatesResultSet()
                ? connection.query(node)
                : connection.exec(node);

        if (localResult.isSuccessful()) result = localResult;
        return localResult.isSuccessful() ? Status.SUCCESS : (optional ? Status.CONTINUE : Status.BREAK);
    }
}
