package me.zort.sqllib.internal.query.part;

import me.zort.sqllib.internal.query.QueryDetails;
import me.zort.sqllib.internal.query.QueryNode;
import me.zort.sqllib.internal.query.ResultSetAware;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public class OffsetStatement<P extends QueryNode<?>> extends QueryNode<P> implements ResultSetAware {

  private final int offset;

  public OffsetStatement(@Nullable P parent, List<QueryNode<?>> initial, int offset) {
    super(parent, initial, Integer.MAX_VALUE);
    this.offset = offset;
  }

  @Override
  public QueryDetails buildQueryDetails() {
    return new QueryDetails(" OFFSET " + Math.max(offset, 0), new HashMap<>());
  }

  @SuppressWarnings("unchecked")
  @Override
  public OffsetStatement<P> then(String part) {
    return (OffsetStatement<P>) super.then(part);
  }
}
