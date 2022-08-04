package me.zort.sqllib.internal.query;

import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;

public enum QueryPriority {

    GENERAL(0),
    CONDITION(1),
    OTHER(2);

    @Getter
    private final int prior;

    QueryPriority(int prior) {
        this.prior = prior;
    }

    public boolean isAncestor() {
        return prior == Arrays.stream(QueryPriority.values())
                .min(Comparator.comparingInt(QueryPriority::getPrior))
                .orElse(QueryPriority.GENERAL)
                .getPrior();
    }

}
