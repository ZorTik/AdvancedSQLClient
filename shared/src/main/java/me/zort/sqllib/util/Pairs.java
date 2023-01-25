package me.zort.sqllib.util;

import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

@NoArgsConstructor
public class Pairs<F, S> extends ArrayList<Pair<F, S>> {

    public Pairs(@NotNull Collection<? extends Pair<F, S>> c) {
        super(c);
    }

    public Pairs<F, S> and(F first, S second) {
        this.add(new Pair<>(first, second));
        return this;
    }

}
