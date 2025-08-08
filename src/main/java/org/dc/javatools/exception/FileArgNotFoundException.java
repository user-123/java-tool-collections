package org.dc.javatools.exception;

public class FileArgNotFoundException extends ArgumentNotFoundException {

    public FileArgNotFoundException() {
        this("file參數缺失或不匹配");
    }

    public FileArgNotFoundException(String message) {
        super(message);
    }

}
