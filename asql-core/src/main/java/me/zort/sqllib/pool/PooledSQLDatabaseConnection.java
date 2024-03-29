package me.zort.sqllib.pool;

import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public abstract class PooledSQLDatabaseConnection extends SQLDatabaseConnection implements Closeable {

  @Getter(onMethod_ = {@Nullable})
  private SQLConnectionPool assignedPool = null;
  @Getter(onMethod_ = {@Nullable})
  private long lastUsed = System.currentTimeMillis();

  public PooledSQLDatabaseConnection(final @NotNull SQLConnectionFactory connectionFactory) {
    super(connectionFactory);
  }

  protected void setAssignedPool(SQLConnectionPool pool) {
    assignedPool = pool;
  }

  protected void setLastUsed(long lastUsed) {
    this.lastUsed = lastUsed;
  }

  public boolean isPoolAssigned() {
    return assignedPool != null;
  }

  /**
   * The close() method in this class checks if there is any assigned
   * pool (most commonly when this instance is part of a pool) and optionally
   * releases this object to be used again. Closes otherwise.
   */
  @Override
  public void close() {
    lastUsed = System.currentTimeMillis();
    if (assignedPool != null) {
      assignedPool.releaseObject(this);
    } else {
      super.close();
    }
  }
}
