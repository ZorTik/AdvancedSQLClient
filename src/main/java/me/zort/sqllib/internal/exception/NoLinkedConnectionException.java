package me.zort.sqllib.internal.exception;

import lombok.Getter;
import me.zort.sqllib.api.Query;

public class NoLinkedConnectionException extends RuntimeException {

    @Getter
    private final Query location;

    public NoLinkedConnectionException(Query location) {
        this.location = location;
    }

}
