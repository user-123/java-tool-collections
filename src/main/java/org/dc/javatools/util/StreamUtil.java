package org.dc.javatools.util;

import java.util.function.BinaryOperator;

public class StreamUtil {

    private StreamUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> BinaryOperator<T> replacing() {
        return (existing, replacement) -> replacement;
    }

    public static <T> BinaryOperator<T> keeping() {
        return (existing, replacement) -> existing;
    }

    public static <T> BinaryOperator<T> throwing() {
        return (existing, replacement) -> {
            //ObjectAssert.isNotEquals(existing, replacement, IllegalStateException.class, "Duplicate key: %s".formatted(existing));
            throw new IllegalStateException("existing value: [%s], new value: [%s]".formatted(existing, replacement));
        };
    }

}
