package me.zort.sqllib.transaction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.zort.sqllib.api.data.QueryResult;
import me.zort.sqllib.api.data.QueryRowsResult;

public final class FlowResult extends QueryRowsResult<QueryResult> {
  @Setter(AccessLevel.PROTECTED)
  @Getter
  private int brokenIndex = -1;

  public FlowResult(final boolean successful) {
    super(successful);
  }

  public FlowResult(final boolean successful, String rejectMessage) {
    super(successful, rejectMessage);
  }

}
