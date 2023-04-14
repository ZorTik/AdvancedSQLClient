package me.zort.sqllib.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ExceptionsUtility {

    public static void runCatching(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
