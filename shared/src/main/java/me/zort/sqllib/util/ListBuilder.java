package me.zort.sqllib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ListBuilder {

    public static <T> List<T> arrayListOf(T... objects) {
        return arrayListOf(ArrayList::new, objects);
    }

    public static <T> List<T> arrayListOf(ListFactory<T> factory, T... objects) {
        List<T> list = factory.create();
        list.addAll(Arrays.asList(objects));
        return list;
    }

    public interface ListFactory<T> {

        List<T> create();

    }

}
