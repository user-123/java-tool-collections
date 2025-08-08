package org.dc.javatools.exception;

public class FileCheckException extends ArgumentCheckException {

    public FileCheckException() {
        this("檔案檢查不通過");
    }

    public FileCheckException(String message) {
        super(message);
    }

}
