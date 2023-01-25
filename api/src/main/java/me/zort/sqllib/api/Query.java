package me.zort.sqllib.api;

/**
 * This class represents a query.
 * @author ZorTik
 */
public interface Query {

    String buildQuery(); // TODO: 25.01.2023 Support for prepared statements.

    /**
     * Returns the highest parent of this query
     * tree.
     * @return The parent.
     */
    default Query getAncestor() {
        return this;
    }

    default boolean isAncestor() {
        return getAncestor() == this;
    }

}
