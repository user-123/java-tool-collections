package org.dc.javatools.exception;

public class InvalidFileExtensionException extends FileCheckException {

    public InvalidFileExtensionException() {
        this("不合規的副檔名或檔案類型");
    }

    public InvalidFileExtensionException(String message) {
        super(message);
    }

}
