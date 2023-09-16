package me.zort.sqllib.internal.query;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level query node that is used to build a query.
 *
 * @author ZorTik
 */
public abstract class AncestorQueryNode extends QueryNode<QueryNode<?>> {
  public AncestorQueryNode() {
    this(new ArrayList<>());
  }
  public AncestorQueryNode(List<QueryNode<?>> initial) {
    super(null, initial, QueryPriority.GENERAL);
  }
}
