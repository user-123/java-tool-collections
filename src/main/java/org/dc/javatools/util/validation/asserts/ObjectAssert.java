package org.dc.javatools.util.validation.asserts;

import java.util.Objects;

public abstract class ObjectAssert extends Assert {

    private ObjectAssert() {
        super();
    }

    public static <T extends Throwable> void isNotNull(Object object, String message) throws T {
        isNotNull(object != null, NullPointerException.class, message);
    }

    public static <T extends Throwable> void isNotNull(Object object, Class<T> exceptionClass, String message) throws T {
        isTrue(object != null, exceptionClass, message);
    }

    public static <T extends Throwable> void isNotEquals(Object object1, Object object2, Class<T> exceptionClass, String message) throws T {
        isTrue(!Objects.equals(object1, object2), exceptionClass, message);
    }

    public static <T extends Throwable> void isDeepEquals(Object object1, Object object2, Class<T> exceptionClass, String message) throws T {
        isTrue(Objects.deepEquals(object1, object2), exceptionClass, message);
    }
}
