package org.dc.javatools.util.validation.asserts;

import org.dc.javatools.exception.InvalidFileExtensionException;

public abstract class FileAssert extends Assert {

    private FileAssert() {
        super();
    }

    static void isValidFileExtension(boolean expression) throws InvalidFileExtensionException {
        final String message = "檔案副檔名不合規";
        try {
            isValidFileExtension(expression, message);
            log.info("檔案副檔名合規");
        } catch (InvalidFileExtensionException ex) {
            log.warn(message);
            throw ex;
        }
    }

    private static void isValidFileExtension(boolean expression, String message) throws InvalidFileExtensionException {
        isTrue(expression, InvalidFileExtensionException.class, message);
    }
}
