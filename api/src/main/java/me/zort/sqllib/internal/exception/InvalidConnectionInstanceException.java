package me.zort.sqllib.internal.exception;

import lombok.Getter;
import me.zort.sqllib.api.SQLConnection;

public class InvalidConnectionInstanceException extends RuntimeException {

    @Getter
    private final SQLConnection invalid;

    public InvalidConnectionInstanceException(SQLConnection invalid) {
        super(String.format("Invalid connection instance %s!", invalid.getClass().getName()));
        this.invalid = invalid;
    }
}
