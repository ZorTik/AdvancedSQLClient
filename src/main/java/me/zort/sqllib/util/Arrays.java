package me.zort.sqllib.util;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Array;

@UtilityClass
public final class Arrays {

    public static <T> T[] add(T[] array, T element) {
        T[] newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length + 1);
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = element;
        return newArray;
    }

}
