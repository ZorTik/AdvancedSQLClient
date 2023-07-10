package me.zort.sqllib.api;

import java.sql.SQLException;

/**
 * This class represents a query.
 *
 * @author ZorTik
 */
public interface Query {

  String buildQuery();

  /**
   * Returns the highest parent of this query
   * tree.
   *
   * @return The parent.
   */
  default Query getAncestor() {
    return this;
  }

  default boolean isAncestor() {
    return getAncestor() == this;
  }

  default void errorSignal(SQLException e) {
  }

}
