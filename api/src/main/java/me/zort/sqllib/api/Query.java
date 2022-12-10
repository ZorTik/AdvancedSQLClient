package me.zort.sqllib.api;

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
    default Query getAncestor() {
        return this;
    }

}
