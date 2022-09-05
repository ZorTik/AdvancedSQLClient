package me.zort.sqllib.api;

import me.zort.sqllib.internal.query.QueryPart;

/**
 * This class represents a query.
 * @author ZorTik
 */
public interface Query {

    String buildQuery();

    /**
     * Returns the highest parent of this query
     * tree.
     * @return The parent.
     */
    QueryPart<?> getAncestor();

}
