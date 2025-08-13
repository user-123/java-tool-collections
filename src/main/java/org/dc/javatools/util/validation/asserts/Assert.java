package org.dc.javatools.util.validation.asserts;

import java.lang.reflect.InvocationTargetException;

public abstract class Assert {
    protected static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Assert.class);

    protected Assert() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void isTrue(boolean expression, Class<T> exceptionClass, String message) throws T {
        if (expression) {
            return;
        }
        T exceptionInstance;
        try {
            exceptionInstance = message == null ? exceptionClass.getConstructor().newInstance() : exceptionClass.getConstructor(String.class).newInstance(message);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
            throw new RuntimeException("Failed to create exception instance", ex);  //TODO 再調整
        }
        log.trace("§§§§§ 中斷流程，斷言拋出 §§§§§：", exceptionInstance);
        throw (T) exceptionInstance.fillInStackTrace();
    }

    public static <T extends Throwable> void isTrue(boolean expression, Class<T> exceptionClass) throws T {
        isTrue(expression, exceptionClass, null);
    }
}
