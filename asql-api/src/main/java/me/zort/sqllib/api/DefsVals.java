package me.zort.sqllib.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicReference;

@AllArgsConstructor
@Getter
public class DefsVals {
    private final String[] defs;
    private final AtomicReference<Object>[] vals;
}
