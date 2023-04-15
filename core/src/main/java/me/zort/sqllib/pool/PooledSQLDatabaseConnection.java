package me.zort.sqllib.pool;

import lombok.Getter;
import me.zort.sqllib.SQLDatabaseConnection;
import me.zort.sqllib.internal.factory.SQLConnectionFactory;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public abstract class PooledSQLDatabaseConnection extends SQLDatabaseConnection implements Closeable {

    private SQLConnectionPool assignedPool = null;
    @Getter(onMethod_ = {@Nullable})
    private long lastUsed = System.currentTimeMillis();

    public PooledSQLDatabaseConnection(SQLConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    protected void setAssignedPool(SQLConnectionPool pool) {
        assignedPool = pool;
    }

    protected void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

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
