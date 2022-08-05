package me.zort.sqllib.api.provider;

import me.zort.sqllib.internal.query.DeleteQuery;

public final class Delete {

    public static DeleteQuery of() {
        return new DeleteQuery(null);
    }

    public static DeleteQuery of(String table) {
        return new DeleteQuery(null, table);
    }

}
